package fr.miguel.mixin;

import fr.miguel.api.AuraAPI;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static fr.miguel.api.AuraAPI.*;

@Mixin(Item.class)
public class AuraItemBarMixin {

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void getAuraItemBarStep(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int current = getAuraDurability(stack);
        int max = getMaxAuraDurability(stack);

        int pixels = Math.round((float) current*13.0F / (float) Math.max(1, max));
        cir.setReturnValue(Math.clamp(pixels, 0, 13));
    }

    @Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
    private void makeItemBarVisible(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(AuraAPI.getAuraDurability(stack) < AuraAPI.getMaxAuraDurability(stack));
    }
}
