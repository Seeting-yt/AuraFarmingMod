package fr.miguel.client.gui;

import fr.miguel.api.AuraAPI;
import fr.miguel.client.gui.components.AuraSliderComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AuraGamblingScreen extends Screen {

    private Button validateButton;
    private AuraSliderComponent auraSlider;

    private final LinearLayout layout = LinearLayout.vertical().spacing(10);

    private static final int PADDING = 16;

    public AuraGamblingScreen() {
        super(Component.translatable("gui.screens.aura_gambling_dice_screen"));
    }

    @Override
    protected void init() {
        super.init();


        StringWidget title = new StringWidget(Component.
                translatable("gui.screens.aura_gambling_dice_screen.texts.title"), this.font);


        title.setWidth(this.font.width(title.getMessage()) + PADDING);
        title.setHeight(20);
        auraSlider = new AuraSliderComponent(0, 0, 50, 20);

        Component buttonComponent = Component
                .translatable("gui.screens.aura_gambling_dice_screen.button.validate");
        validateButton = Button.builder(buttonComponent, (button) -> {
            if (Minecraft.getInstance().player != null) // log only -> will be removed
                Minecraft.getInstance().player.sendSystemMessage(AuraAPI.auraGetCommandMessage(this.auraSlider.getActualValue()));
            this.onClose();
        }).width(this.font.width(buttonComponent) + PADDING).build();

        addChildrenToLayout(List.of(
                title,
                auraSlider,
                validateButton
        ));

        layout.arrangeElements();
        layout.setX(this.width / 2 - layout.getWidth() / 2);
        layout.setY(this.height / 2 - layout.getHeight() / 2);
        layout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private  <T extends LayoutElement> void addChildrenToLayout(List<T> child) {
        child.forEach(this::addChildToLayout);
    }

    private  <T extends LayoutElement> void addChildToLayout(T child) {
        this.layout.addChild(child);
    }
}
