package fr.miguel.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fr.miguel.api.AuraAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenMixin {

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"
            )
    )
    private void addAuraToEnchantTooltipLines(
            GuiGraphicsExtractor graphics, Font font, List<Component> lines,
            int xo, int yo,
            Operation<Void> original,
            @Local(name = "minLevel") int minLevel) {

        lines.add(AuraAPI.auraCostMessage(minLevel).copy().withStyle(ChatFormatting.GOLD));

        original.call(graphics, font, lines, xo, yo);

    }
}
