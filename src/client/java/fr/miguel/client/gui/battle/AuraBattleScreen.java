package fr.miguel.client.gui.battle;

import fr.miguel.client.battle.ClientBattleState;
import fr.miguel.client.gui.components.VerticalAuraSlider;
import fr.miguel.component.ModDataComponentTypes;
import fr.miguel.menu.AuraBattleMenu;
import fr.miguel.network.BattlePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public class AuraBattleScreen extends AbstractContainerScreen<AuraBattleMenu> {

    // relatifs à (leftPos, topPos)
    private static final int BAR_X = 134, BAR_Y = 23, BAR_W = 22, BAR_H = 108;

    private static final int COL_BORDER = 0xFF000000, COL_PANEL = 0xFF2D2D35, COL_SLOT = 0xFF1A1A20,
            COL_SLOT_EDGE = 0xFF4A4A55, COL_STAKED = 0xFFFFD34D,
            COL_TEXT = 0xFFFFFFFF, COL_DIM = 0xFFAAAAAA;

    private Button allInButton, validateButton;
    private VerticalAuraSlider bar;

    public AuraBattleScreen(AuraBattleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.titleLabelY = -1000;       // on dessine nos propres libellés
        this.inventoryLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();   // calcule leftPos / topPos

        bar = addRenderableWidget(new VerticalAuraSlider(
                this.leftPos + BAR_X, this.topPos + BAR_Y, BAR_W, BAR_H,
                () -> { var st = ClientBattleState.get(); return st == null ? 0 : st.myAura(); },
                () -> { var st = ClientBattleState.get(); return st == null ? 0 : st.maxAura(); },
                value -> send(BattlePayloads.MenuAction.SET_AURA, value)));

        int y = this.topPos + 150;
        int w = 96;
        int x = this.leftPos + (this.imageWidth - (3 * w + 16)) / 2;

        allInButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.screens.aura_battle.all_in"),
                b -> send(BattlePayloads.MenuAction.ALL_IN, 0)).bounds(x, y, w, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.screens.aura_battle.cancel"),
                b -> send(BattlePayloads.MenuAction.CANCEL, 0)).bounds(x + w + 8, y, w, 20).build());

        validateButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.screens.aura_battle.validate"),
                b -> {
                    var st = ClientBattleState.get();
                    boolean validated = st != null && st.myValidated();
                    send(validated ? BattlePayloads.MenuAction.EDIT : BattlePayloads.MenuAction.VALIDATE, 0);
                }).bounds(x + 2 * (w + 8), y, w, 20).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        var st = ClientBattleState.get();
        boolean validated = st != null && st.myValidated();
        validateButton.setMessage(Component.translatable(
                validated ? "gui.screens.aura_battle.edit" : "gui.screens.aura_battle.validate"));
        validateButton.active = st != null && (validated || st.myAura() > 0 || st.myItemCount() > 0);
        allInButton.active = st != null && !validated;
        bar.active = st != null && !validated;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        var st = ClientBattleState.get();

        // panneau
        g.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, COL_BORDER);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COL_PANEL);

        // slots (cadre doré pour les items misés)
        for (Slot slot : this.menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            boolean staked = slot.index >= AuraBattleMenu.INV_START
                    && slot.getItem().get(ModDataComponentTypes.STAKE_ID) != null;
            g.fill(x - 1, y - 1, x + 17, y + 17, staked ? COL_STAKED : COL_SLOT_EDGE);
            g.fill(x, y, x + 16, y + 16, COL_SLOT);
        }

        // libellés
        int bx = leftPos + BAR_X;
        Component title = Component.translatable("gui.screens.aura_battle.versus", st != null ? st.oppName() : "...");
        g.text(this.font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 3, COL_STAKED);
        g.text(this.font, Component.translatable("gui.screens.aura_battle.stake_label"),
                leftPos + AuraBattleMenu.STAKE_X - 1, topPos + 13, COL_TEXT);
        g.text(this.font, Component.translatable("gui.screens.aura_battle.aura_label"), bx, topPos + 13, COL_TEXT);
        g.text(this.font, Component.translatable("gui.screens.aura_battle.inventory_label"),
                leftPos + AuraBattleMenu.INV_X - 1, topPos + 13, COL_TEXT);
        g.text(this.font, Component.translatable("gui.screens.aura_battle.hint"),
                leftPos + AuraBattleMenu.STAKE_X - 1, topPos + 138, COL_DIM);

        if (st != null) {
            Component value = Component.translatable("gui.screens.aura_battle.aura_value", bar.shown(), st.maxAura());
            g.text(this.font, value, bx + BAR_W / 2 - font.width(value) / 2, topPos + BAR_Y + BAR_H + 4, COL_TEXT);

            // récapitulatif des deux mises
            int tx = leftPos + AuraBattleMenu.INV_X - 1;
            Component ready = Component.translatable("gui.screens.aura_battle.ready");
            g.text(this.font, Component.translatable("gui.screens.aura_battle.my_total",
                    st.myItemsAura() + st.myAura()), tx, topPos + 104, st.myValidated() ? COL_STAKED : COL_TEXT);
            g.text(this.font, Component.translatable("gui.screens.aura_battle.breakdown",
                    st.myItemCount(), st.myAura()), tx, topPos + 114, COL_DIM);
            g.text(this.font, Component.translatable("gui.screens.aura_battle.opp_total",
                            st.oppName(), st.oppItemsAura() + st.oppAura()), tx, topPos + 126,
                    st.oppValidated() ? COL_STAKED : COL_TEXT);
            g.text(this.font, st.oppValidated()
                            ? Component.translatable("gui.screens.aura_battle.breakdown_ready",
                            st.oppItemCount(), st.oppAura(), ready)
                            : Component.translatable("gui.screens.aura_battle.breakdown",
                            st.oppItemCount(), st.oppAura()),
                    tx, topPos + 136, COL_DIM);
        }
    }

    @Override
    public void removed() {
        ClientBattleState.clear();
        super.removed();
    }

    private void send(int action, int value) {
        ClientPlayNetworking.send(new BattlePayloads.MenuAction(action, value));
    }
}