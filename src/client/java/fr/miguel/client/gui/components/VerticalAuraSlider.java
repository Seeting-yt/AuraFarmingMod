package fr.miguel.client.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class VerticalAuraSlider extends AbstractWidget {

    private final IntSupplier current;
    private final IntSupplier max;
    private final IntConsumer onChange;

    private boolean dragging;
    private int local = -1;
    private int lastSent = -1;

    public VerticalAuraSlider(int x, int y, int width, int height,
                              IntSupplier current, IntSupplier max,
                              IntConsumer onChange) {
        super(x, y, width, height, Component.empty());

        this.current = current;
        this.max = max;
        this.onChange = onChange;
    }

    public int shown() {
        return dragging && local >= 0 ? local : current.getAsInt();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF4A4A55);
        g.fill(x, y, x + w, y + h, 0xFF1A1A20);

        int m = max.getAsInt();
        if (m <= 0) return;
        int fillH = (int) Math.round((double) shown() / m * h);
        g.fill(x, y + h - fillH, x + w, y + h, this.active ? 0xFF9B6BFF : 0xFF5A4A80);
        int ky = Math.max(y, Math.min(y + h - 3, y + h - fillH - 1));
        g.fill(x - 3, ky, x + w + 3, ky + 3, 0xFFFFFFFF);   // poignée
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        dragging = true;
        update(event.y());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        update(event.y());
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        dragging = false;
        local = -1;
    }

    private void update(double mouseY) {
        int max = this.max.getAsInt();

        if (max <= 0) return;

        double ratio = Math.max(0, Math.min(1, (getY() + getHeight() - mouseY) / getHeight()));
        int value = (int) Math.round(ratio * max);
        local = value;
        if (value != lastSent) {
            lastSent = value;
            onChange.accept(value);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
