package fr.miguel.client.gui.components;

import fr.miguel.api.AuraAPI;
import fr.miguel.player.CustomPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class AuraSliderComponent extends AbstractSliderButton {

    private int minimumValue = 0;
    private int maximalValue;
    private int actualValue;

    private final CustomPlayer customPlayer;

    private static final int PADDING = 16;

    public AuraSliderComponent(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), 0.1D);

        Player player = Minecraft.getInstance().player;

        if (player == null) {
            customPlayer = null;
            return;
        }

        customPlayer = CustomPlayer.getCustomPlayerOrNew(player);

        this.maximalValue = (int) this.customPlayer.getPlayerAura();

        updateMessage();
    }

    @Override
    protected void updateMessage() {
        applyValue();
        Component text = AuraAPI.Messages.placeHolder("gui.screens.component.aura_slider.update_message",
                "{{value}}",
                this.actualValue);

        Font font = Minecraft.getInstance().font;

        this.setWidth(font.width(text) + PADDING);

        this.setMessage(text);
        //this.setMessage(Component.translatable("gui.screens.component.aura_slider.update_message"));
    }

    @Override
    protected void applyValue() {
        this.actualValue = this.minimumValue + (int) Math.round(this.value * (this.maximalValue - this.minimumValue));
    }

    public void setMinimumValue(int minimumValue) {

        if (minimumValue >= this.maximalValue)
            throw new IllegalArgumentException("can't set a minimal value larger than the maximal value!");

        this.minimumValue = minimumValue;
    }

    public void setMaximalValue(int maximalValue) {
        if (maximalValue <= this.minimumValue)
            throw new IllegalArgumentException("can't set a maximal value smaller than the maximal value!");

        this.maximalValue = maximalValue;
    }

    public int getMinimumValue() {
        return this.minimumValue;
    }

    public int getMaximalValue() {
        return this.maximalValue;
    }

    public int getActualValue() {
        return this.actualValue;
    }
}
