package fr.miguel.client.mixin;

import fr.miguel.network.LeftClickPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class LeftClickMixin {


    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Nullable
    public HitResult hitResult;

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void onLeftClick(CallbackInfoReturnable<Boolean> cir) {
        if (this.player != null && this.hitResult != null) {
            if (this.hitResult.getType() == HitResult.Type.ENTITY)
                return;

            ClientPlayNetworking.send(new LeftClickPayload());
        }
    }
}
