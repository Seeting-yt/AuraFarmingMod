package fr.miguel.menu;

import fr.miguel.api.AuraAPI;
import fr.miguel.battle.BattleManager;
import fr.miguel.battle.BattleSession;
import fr.miguel.battle.StakeUtil;
import fr.miguel.component.ModDataComponentTypes;
import fr.miguel.network.BattlePayloads.MenuAction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class AuraBattleMenu extends AbstractContainerMenu {

    public static final int STAKE_SLOTS = 36;
    public static final int STAKE_COLS = 6;
    public static final int INV_START = STAKE_SLOTS;
    public static final int INV_END = INV_START + 36;

    public static final int STAKE_X = 11, STAKE_Y = 23;
    public static final int INV_X = 173, INV_Y = 23;

    private final @Nullable BattleSession session;
    private final @Nullable MinecraftServer server;
    private final Inventory inventory;
    private final Container stakeDisplay = new SimpleContainer(STAKE_SLOTS);
    private final int[] displaySource = new int[STAKE_SLOTS];

    /**
     * Constructor used server side
     */
    public AuraBattleMenu(int containerId, Inventory inventory, @Nullable BattleSession session, @Nullable MinecraftServer server) {
        super(ModMenuTypes.AURA_BATTLE, containerId);

        this.session = session;
        this.server = server;
        this.inventory = inventory;

        Arrays.fill(displaySource, -1);

        for (int i = 0; i < STAKE_SLOTS; i++)
            addSlot(new LockedSlot(stakeDisplay, i,
                    STAKE_X + (i % STAKE_COLS) * 18, STAKE_Y + (i / STAKE_COLS) * 18));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new LockedSlot(inventory, 9 + row * 9 + col, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new LockedSlot(inventory, col, INV_X + col * 18, INV_Y + 58));

        if (session != null) refreshStakeDisplay();
    }

    /**
     * Constructor used client side by the {@link MenuType} registry
     */
    public AuraBattleMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, null);
    }

    private static final class LockedSlot extends Slot {
        LockedSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(@NonNull ItemStack stack) { return false; }
        @Override public boolean mayPickup(@NonNull Player player) { return false; }
    }

    private static int containerIndex(int menuSlot) {
        int i = menuSlot - INV_START;
        return i < 27 ? 9 + i : i - 27;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {

        if (player instanceof ServerPlayer serverPlayer && session != null
                && slotIndex >= 0 && slotIndex < INV_END
                && buttonNum == 0 && containerInput == ContainerInput.THROW)
            handleAction(serverPlayer, MenuAction.TOGGLE, slotIndex);
    }

    // ---------- ACTIONS (SERVER SIDE) ----------

    public void handleAction(ServerPlayer player, int action, int value) {

        if (session == null || server == null || player != inventory.player) return;
        if (session.state() != BattleSession.State.STAKING) return;

        BattleSession.Stake mine = session.stakeOf(player.getUUID());
        boolean editable = !mine.validated;

        switch (action) {
            case MenuAction.TOGGLE -> { if (editable) toggle(player, value); }
            case MenuAction.SET_AURA -> {
                if (!editable) return;
                int v = Math.max(0, Math.min(value, StakeUtil.playerAura(player)));
                if (v != mine.aura) { mine.aura = v; onStakeChanged(player); }
            }
            case MenuAction.ALL_IN -> { if (editable) allIn(player, mine); }
            case MenuAction.VALIDATE -> validate(player, mine);
            case MenuAction.EDIT -> {
                if (mine.validated) { mine.validated = false; BattleManager.broadcastState(server, session); }
            }
            case MenuAction.CANCEL -> {
                try { BattleManager.cancel(server, player); }
                catch (BattleManager.BattleException e) { player.sendSystemMessage(e.text()); }
            }
            default -> { }
        }
    }

    private void toggle(ServerPlayer player, int menuSlot) {
        if (menuSlot < 0 || menuSlot >= INV_END || session == null) return;

        int source = menuSlot < INV_START ? displaySource[menuSlot] : menuSlot;
        if (source < INV_START || source >= INV_END) return;

        ItemStack stack = inventory.getItem(containerIndex(source));

        if (stack.isEmpty()) return;

        if (StakeUtil.isStaked(stack, session.id())) {
            stack.remove(ModDataComponentTypes.STAKE_ID);
        } else if (AuraAPI.hasAura(stack) && stack.get(ModDataComponentTypes.STAKE_ID) == null) {
            stack.set(ModDataComponentTypes.STAKE_ID, session.id());
        } else {
            return;
        }
        onStakeChanged(player);
    }

    private void allIn(ServerPlayer player, BattleSession.Stake mine) {
        if (session == null) return;

        for (int i = INV_START; i < INV_END; i++) {
            ItemStack s = inventory.getItem(containerIndex(i));
            if (!s.isEmpty() && AuraAPI.hasAura(s) && s.get(ModDataComponentTypes.STAKE_ID) == null)
                s.set(ModDataComponentTypes.STAKE_ID, session.id());
        }
        mine.aura = StakeUtil.playerAura(player);
        onStakeChanged(player);
    }

    private void validate(ServerPlayer player, BattleSession.Stake mine) {
        if (mine.validated || session == null) return;
        if (mine.aura <= 0 && StakeUtil.count(player, session.id()) == 0) {
            player.sendSystemMessage(Component.translatable("battle.error.empty_stake"));
            return;
        }
        mine.aura = Math.min(mine.aura, StakeUtil.playerAura(player));
        mine.validated = true;
        BattleManager.broadcastState(server, session);
        if (session.bothValidated()) BattleManager.onBothValidated(server, session);
    }

    private void onStakeChanged(ServerPlayer player) {
        if (session == null || server == null) return;
        refreshStakeDisplay();
        // every modification cancel opponent's validation
        var opponentId = session.opponentOf(player.getUUID());
        BattleSession.Stake opponent = session.stakeOf(opponentId);
        if (opponent.validated) {
            opponent.validated = false;
            ServerPlayer opp = server.getPlayerList().getPlayer(opponentId);
            if (opp != null) opp.sendSystemMessage(Component.translatable("battle.stake_changed", player.getName()));
        }
        BattleManager.broadcastState(server, session);
    }

    private void refreshStakeDisplay() {
        if (session == null) return;

        stakeDisplay.clearContent();
        Arrays.fill(displaySource, -1);
        int n = 0;
        for (int i = INV_START; i < INV_END && n < STAKE_SLOTS; i++) {
            ItemStack s = inventory.getItem(containerIndex(i));
            if (StakeUtil.isStaked(s, session.id())) {
                stakeDisplay.setItem(n, s.copy());
                displaySource[n] = i;
                n++;
            }
        }
    }



    // ---------- VANILLA ----------

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return session == null || session.state() == BattleSession.State.STAKING;
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);

        if (session != null && server != null && player instanceof ServerPlayer serverPlayer
                && session.state() == BattleSession.State.STAKING)
            BattleManager.onMenuClosed(server, serverPlayer);
    }
}
