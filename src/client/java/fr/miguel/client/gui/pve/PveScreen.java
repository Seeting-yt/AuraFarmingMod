package fr.miguel.client.gui.pve;

import fr.miguel.pve.BattleEvent;
import fr.miguel.pve.Side;
import fr.miguel.pve.game.PvePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class PveScreen extends Screen {

    private enum Menu { MAIN, ATTACKS }

    private record Popup(Side side, Component text, int color, int born) {}

    private static final int COL_BORDER = 0xFF000000, COL_PANEL = 0xE02D2D35, COL_TEXT = 0xFFFFFFFF, COL_DIM = 0xFFAAAAAA;
    private static final int BTN_W = 150, BTN_H = 20, GAP = 6, MARGIN = 12;

    private final PvePayloads.Open info;
    private final String playerName;
    private final @Nullable LivingEntity enemy;

    private int playerHp, enemyHp;
    private float playerBar, enemyBar;

    private final Deque<BattleEvent> queue = new ArrayDeque<>();
    private final List<Popup> popups = new ArrayList<>();
    private Component log;
    private int wait;
    private int age;

    private Menu menu = Menu.MAIN;
    private boolean awaiting;
    private int awaitingSince;
    private byte pendingOutcome = PvePayloads.ONGOING;
    private long pendingDelta;
    private boolean finished;
    private boolean closeSent;

    public PveScreen(PvePayloads.Open info) {
        super(Component.translatable("gui.screens.pve"));
        this.info = info;
        Minecraft mc = Minecraft.getInstance();
        this.playerName = mc.player.getName().getString();
        Object e = mc.level == null ? null : mc.level.getEntity(info.enemyEntityId());
        this.enemy = e instanceof LivingEntity le ? le : null;
        this.playerHp = info.playerHp();
        this.enemyHp = info.enemyHp();
        this.playerBar = playerHp;
        this.enemyBar = enemyHp;
        this.log = Component.translatable("pve.screen.start", info.enemyName());
    }

    // ---------- NETWORK ----------

    public void onTurn(PvePayloads.Turn turn) {
        awaiting = false;
        if (turn.outcome() == PvePayloads.CANCELLED) {   // combat annulé côté serveur : on ferme sans rien afficher
            finished = true;
            closeSent = true;
            onClose();
            return;
        }
        queue.addAll(turn.events());
        pendingOutcome = turn.outcome();
        pendingDelta = turn.auraDelta();
        menu = Menu.MAIN;
        rebuildWidgets();
    }

    private void sendAction(byte action, String attackId) {
        ClientPlayNetworking.send(new PvePayloads.Action(action, attackId));
        awaiting = true;
        awaitingSince = age;
        rebuildWidgets();
    }

    // ---------- STATE ----------

    private boolean busy() {
        return awaiting || wait > 0 || !queue.isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        playerBar += (playerHp - playerBar) * 0.25f;
        enemyBar += (enemyHp - enemyBar) * 0.25f;
        popups.removeIf(p -> age - p.born() > 30);

        // pas de réponse du serveur (action refusée) : on rend la main
        if (awaiting && age - awaitingSince > 60) {
            awaiting = false;
            rebuildWidgets();
        }

        boolean wasBusy = wait > 0 || !queue.isEmpty();
        if (wait > 0) wait--;
        else if (!queue.isEmpty()) play(queue.poll());

        if (wasBusy && wait == 0 && queue.isEmpty()) {   // le rejeu vient de se terminer
            if (pendingOutcome != PvePayloads.ONGOING) {
                finished = true;
                log = resultText();
            }
            rebuildWidgets();
        }
    }

    private void play(BattleEvent event) {
        switch (event) {
            case BattleEvent.Used u -> {
                log = Component.translatable("pve.used", nameOf(u.actor()), Component.translatable(u.attackNameKey()));
                swing(u.actor());
                wait = 22;
            }
            case BattleEvent.Miss m -> {
                log = Component.translatable("pve.miss", nameOf(m.target()));
                popups.add(new Popup(m.target(), Component.translatable("pve.screen.miss"), COL_DIM, age));
                wait = 18;
            }
            case BattleEvent.Hit h -> {
                log = Component.translatable("pve.hit", nameOf(h.target()), h.damage(), h.hpAfter(), maxHpOf(h.target()));
                if (h.target() == Side.PLAYER) playerHp = h.hpAfter(); else enemyHp = h.hpAfter();
                popups.add(new Popup(h.target(), Component.literal("-" + h.damage()), 0xFFFF5555, age));
                wait = 30;
            }
            case BattleEvent.Down d -> {
                log = Component.translatable("pve.down", nameOf(d.who()));
                wait = 30;
            }
            case BattleEvent.Fled f -> {
                log = Component.translatable("pve.fled", nameOf(f.who()));
                wait = 30;
            }
        }
    }

    private void swing(Side side) {
        LivingEntity livingEntity = side == Side.PLAYER ? Minecraft.getInstance().player : enemy;
        if (livingEntity != null) livingEntity.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
    }

    private Component resultText() {
        String key = switch (pendingOutcome) {
            case PvePayloads.WON -> "pve.result.won";
            case PvePayloads.LOST -> "pve.result.lost";
            default -> "pve.result.fled";
        };
        return Component.translatable(key, String.format("%+d", pendingDelta));
    }

    private String nameOf(Side side) { return side == Side.PLAYER ? playerName : info.enemyName(); }
    private int maxHpOf(Side side) { return side == Side.PLAYER ? info.playerMaxHp() : info.enemyMaxHp(); }


    // ---------- WIDGETS ---------

    @Override
    protected void init() {
        super.init();
        int x = this.width - MARGIN - BTN_W;
        int bottom = this.height - MARGIN - BTN_H;

        if (finished) {
            addButton(Component.translatable("pve.screen.close"), x, bottom, true, this::onClose);
            return;
        }

        boolean active = !busy();
        if (menu == Menu.MAIN) {
            addButton(Component.translatable("pve.screen.flee"), x, bottom, active,
                    () -> sendAction(PvePayloads.ACT_FLEE, ""));
            addButton(Component.translatable("pve.screen.items"), x, bottom - (BTN_H + GAP), false, () -> { });   // étape 4
            addButton(Component.translatable("pve.screen.attack"), x, bottom - 2 * (BTN_H + GAP), active, () -> {
                menu = Menu.ATTACKS;
                rebuildWidgets();
            });
        } else {
            addButton(Component.translatable("pve.screen.back"), x, bottom, true, () -> {
                menu = Menu.MAIN;
                rebuildWidgets();
            });
            int y = bottom - (BTN_H + GAP);
            for (PvePayloads.AttackInfo a : info.attacks()) {
                addButton(Component.translatable(a.nameKey()).append(" (" + a.hitPercent() + "%)"), x, y, active,
                        () -> sendAction(PvePayloads.ACT_ATTACK, a.id()));
                y -= BTN_H + GAP;
            }
        }
    }

    private void addButton(Component text, int x, int y, boolean active, Runnable action) {
        Button button = Button.builder(text, btn -> action.run()).bounds(x, y, BTN_W, BTN_H).build();
        button.active = active;
        addRenderableWidget(button);
    }


    // ---------- RENDER ----------

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphicsExtractor, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphicsExtractor, mouseX, mouseY, delta);

        int stageBottom = this.height - 100;
        int enemyCx = this.width * 68 / 100, enemyCy = stageBottom * 45 / 100;
        int playerCx = this.width * 26 / 100, playerCy = stageBottom - 55;

        drawEntity(graphicsExtractor, enemy, enemyCx, enemyCy, 55, enemyCx - 120);
        drawEntity(graphicsExtractor, Minecraft.getInstance().player, playerCx, playerCy, 55, playerCx + 120);

        drawHpBox(graphicsExtractor, MARGIN + 4, MARGIN + 4, info.enemyName(), enemyBar, info.enemyMaxHp());
        drawHpBox(graphicsExtractor, this.width - MARGIN - 4 - 170, stageBottom - 40, playerName, playerBar, info.playerMaxHp());

        for (Popup p : popups) {
            int cx = p.side() == Side.PLAYER ? playerCx : enemyCx;
            int cy = (p.side() == Side.PLAYER ? playerCy : enemyCy) - 55 - (age - p.born()) / 2;
            graphicsExtractor.text(this.font, p.text(), cx - this.font.width(p.text()) / 2, cy, p.color());
        }

        // journal
        int logW = this.width - 3 * MARGIN - BTN_W;
        int logY = this.height - MARGIN - 3 * BTN_H - 2 * GAP;
        graphicsExtractor.fill(MARGIN - 1, logY - 1, MARGIN + logW + 1, this.height - MARGIN + 1, COL_BORDER);
        graphicsExtractor.fill(MARGIN, logY, MARGIN + logW, this.height - MARGIN, COL_PANEL);
        graphicsExtractor.text(this.font, log, MARGIN + 8, logY + 8, COL_TEXT);
    }

    private void drawHpBox(GuiGraphicsExtractor graphicsExtractor, int x, int y, String name, float bar, int max) {
        int w = 170;
        graphicsExtractor.fill(x - 1, y - 1, x + w + 1, y + 35, COL_BORDER);
        graphicsExtractor.fill(x, y, x + w, y + 34, COL_PANEL);
        graphicsExtractor.text(this.font, Component.literal(name), x + 6, y + 5, COL_TEXT);

        int bx = x + 6, by = y + 20, bw = w - 12;
        graphicsExtractor.fill(bx - 1, by - 1, bx + bw + 1, by + 9, COL_BORDER);
        graphicsExtractor.fill(bx, by, bx + bw, by + 8, 0xFF3A3A3A);
        float ratio = max <= 0 ? 0 : Math.clamp(bar / max, 0, 1);
        int color = ratio > 0.5f ? 0xFF55FF55 : ratio > 0.2f ? 0xFFFFD34D : 0xFFFF5555;
        graphicsExtractor.fill(bx, by, bx + Math.round(bw * ratio), by + 8, color);

        Component hp = Component.literal(Math.round(bar) + " / " + max);
        graphicsExtractor.text(this.font, hp, x + w - 6 - this.font.width(hp), y + 5, COL_DIM);
    }

    private void drawEntity(GuiGraphicsExtractor graphicsExtractor, @Nullable LivingEntity entity, int cx, int cy, int half, float lookAtX) {
        if (entity == null) return;
        int scale = Math.clamp((int) (2 * half * 0.75f / Math.max(entity.getBbHeight(), 0.6f)), 12, 90);
        InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphicsExtractor, cx - half, cy - half, cx + half, cy + half, scale, 0.0625F, lookAtX, cy, entity);
    }

    // ---------- CLOSING ----------

    @Override public boolean shouldCloseOnEsc() { return finished; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        if (finished && !closeSent) {
            closeSent = true;
            ClientPlayNetworking.send(PvePayloads.Close.INSTANCE);
        }
        super.removed();
    }
}