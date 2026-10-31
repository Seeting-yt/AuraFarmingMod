package fr.miguel.client.gui.battle;

import fr.miguel.network.BattlePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BattleChallengeScreen extends Screen {

    private static final int BUTTON_WIDTH = 100;
    private final String challengerName;

    public BattleChallengeScreen(String challengerName) {
        super(Component.translatable("gui.screens.battle_challenge"));

        this.challengerName = challengerName;
    }

    @Override
    protected void init() {
        super.init();

        LinearLayout layout = LinearLayout.vertical().spacing(10);
        layout.defaultCellSetting().alignHorizontallyCenter();

        layout.addChild(label(Component.translatable("gui.screens.battle_challenge.title")));
        layout.addChild(label(Component.translatable("gui.screens.battle_challenge.message", challengerName)));
        layout.addChild(label(Component.translatable("gui.screens.battle_challenge.rules")));

        Button accept = Button.builder(Component.translatable("gui.screens.battle_challenge.accept"), (button -> {
            answer(BattlePayloads.BattleAction.ACCEPT);
        })).width(BUTTON_WIDTH).build();

        Button refuse = Button.builder(Component.translatable("gui.screens.battle_challenge.refuse"), (button -> {
            answer(BattlePayloads.BattleAction.REFUSE);
        })).width(BUTTON_WIDTH).build();

        this.answerButtons = new Button[] {accept, refuse};

        LinearLayout buttons = LinearLayout.horizontal().spacing(8);
        buttons.addChild(accept);
        buttons.addChild(refuse);
        layout.addChild(buttons);

        layout.arrangeElements();
        layout.setY(this.height / 2 - layout.getHeight() / 2);
        layout.setX(this.width / 2 - layout.getWidth() / 2);
        layout.visitWidgets(this::addRenderableWidget);
    }
    private Button[] answerButtons = new Button[0];
    private void answer(byte action) {
        for (Button button : answerButtons) button.active = false;
        ClientPlayNetworking.send(new BattlePayloads.BattleAction(action));
    }

    private StringWidget label(Component text) {
        StringWidget widget = new StringWidget(text, this.font);

        widget.setWidth(this.font.width(text));
        widget.setHeight(this.font.lineHeight + 4);

        return widget;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
