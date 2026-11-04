package fr.miguel.client.gui;

import fr.miguel.api.AuraAPI;
import fr.miguel.client.gui.components.AuraSliderComponent;
import fr.miguel.items.ModItems;
import fr.miguel.player.CustomPlayer;
import fr.miguel.player.PlayerPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AuraGamblingScreen extends Screen {

    private static final int PADDING = 16;

    public AuraGamblingScreen() {
        super(Component.translatable("gui.screens.aura_gambling_dice_screen"));
    }

    @Override
    protected void init() {
        super.init();

        LinearLayout layout = LinearLayout.vertical().spacing(10);

        StringWidget title = new StringWidget(Component.
                translatable("gui.screens.aura_gambling_dice_screen.texts.title"), this.font);


        title.setWidth(this.font.width(title.getMessage()) + PADDING);
        title.setHeight(20);
        AuraSliderComponent auraSlider = new AuraSliderComponent(0, 0, 50, 20);

        Component buttonComponent = Component
                .translatable("gui.screens.aura_gambling_dice_screen.button.validate");

        Button validateButton = Button.builder(buttonComponent, (button) -> {
            button.active = false;

            Player player = this.minecraft.player;

            if (player == null) {
                this.onClose();
                return;
            }

            ClientPlayNetworking.send(new PlayerPayloads.GamblingEvent(player.getUUID(), auraSlider.getActualValue()));

            this.onClose();
        }).width(this.font.width(buttonComponent) + PADDING).build();


        layout.addChild(title);
        layout.addChild(auraSlider);
        layout.addChild(validateButton);

        layout.arrangeElements();
        layout.setX(this.width / 2 - layout.getWidth() / 2);
        layout.setY(this.height / 2 - layout.getHeight() / 2);
        layout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
