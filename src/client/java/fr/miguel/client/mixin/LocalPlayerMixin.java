package fr.miguel.client.mixin;

import fr.miguel.player.CustomPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onClientPlayerTick(CallbackInfo info) {
        LocalPlayer localPlayer = (LocalPlayer) (Object) this;

        CustomPlayer customPlayer = CustomPlayer.getCustomPlayerOrNew(localPlayer);

        localPlayer.experienceLevel = (int) customPlayer.getPlayerAura();
        localPlayer.experienceProgress = 0.0F;
    }
}
