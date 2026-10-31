package fr.miguel.mixin;

import fr.miguel.api.AuraAPI;
import fr.miguel.player.CustomPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public class EnchantmentScreenHandler {

    @Shadow
    @Final
    public int[] costs;

    @Unique
    private final static double factor = 0.1;

    @Inject(method = "clickMenuButton", at = @At(value = "HEAD"), cancellable = true)
    private void onEnchantButtonClick(Player player, int buttonId, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        CustomPlayer customPlayer = CustomPlayer.getCustomPlayerOrNew(player);

        if (buttonId >= 0 && buttonId < 3) {
            int enchantLevel = this.costs[buttonId];

            double auraCost = AuraAPI.auraPointCostForEnchant(enchantLevel);

            double playerAura = customPlayer.getPlayerAura();

            if (playerAura < auraCost) {
                double difference = Math.abs(auraCost - playerAura) + 1;
                double probability = 1.0 - Math.exp(-factor * difference);

                if (player.getRandom().nextDouble() < probability) {

                    double auraLost = AuraAPI.auraPointCostForEnchant(enchantLevel);

                    player.sendSystemMessage(Component.translatable("message.player.aura_missing"));

                    AuraAPI.sendAuraPointLostToPlayer(player, auraLost);

                    customPlayer.removePlayerAura(auraLost);

                    callbackInfoReturnable.setReturnValue(false);
                    callbackInfoReturnable.cancel();
                } else if (difference > 0.95 * auraCost) {
                    customPlayer.addPlayerAura(auraCost);
                    AuraAPI.sendAuraPointGainedToPlayer(player, auraCost);
                }
            }
        }
    }
}
