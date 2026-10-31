package fr.miguel.client.gui.battle;

import fr.miguel.network.BattlePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BattleWaitingScreen extends Screen {

    private static final int PADDING = 16;
    private final String targetName;

    public BattleWaitingScreen(String targetName) {
        super(Component.translatable("gui.screens.battle_waiting"));
        this.targetName = targetName;
    }

    @Override
    protected void init() {
        super.init();

        LinearLayout layout = LinearLayout.vertical().spacing(10);
        layout.defaultCellSetting().alignHorizontallyCenter();

        layout.addChild(label(Component.translatable("gui.screens.battle_waiting.title")));
        layout.addChild(label(Component.translatable("gui.screens.battle_waiting.message", targetName)));


        Component cancelText = Component.translatable("gui.screens.battle_waiting.cancel");

        layout.addChild(Button.builder(cancelText, button ->  {
            button.active = false;
            ClientPlayNetworking.send(new BattlePayloads.BattleAction(BattlePayloads.BattleAction.CANCEL));
        }).width(this.font.width(cancelText) + PADDING).build());

        layout.arrangeElements();
        layout.setX(this.width / 2 - layout.getWidth() / 2);
        layout.setY(this.height / 2 - layout.getHeight() / 2);
        layout.visitWidgets(this::addRenderableWidget);
    }

    private StringWidget label(Component text) {
        StringWidget widget = new StringWidget(text, this.font);

        widget.setWidth(this.font.width(text));
        widget.setHeight(this.font.lineHeight + 4);

        return widget;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
