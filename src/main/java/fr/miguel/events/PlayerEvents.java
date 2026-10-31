package fr.miguel.events;

import fr.miguel.api.AuraAPI;
import fr.miguel.network.LeftClickPayload;
import fr.miguel.player.CustomPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;


public class PlayerEvents {
    public static void onPlayerJoin(Player player) {
        CustomPlayer customPlayer =
                CustomPlayer.getCustomPlayerOrNew(player);

        customPlayer.setPlayerAura(player.getRandom().nextDouble()*67);


    }
    


    public static boolean onPlayerGetHurt(LivingEntity livingEntity,
                                          DamageSource damageSource,
                                          float amount) {

        if (!(livingEntity instanceof Player player)) {
            return true;
        }

        CustomPlayer customPlayer = CustomPlayer.getCustomPlayerOrNew(player);
        double auraLost = AuraAPI.auraPointCostForDamage(amount);
        customPlayer.removePlayerAura(auraLost);
        AuraAPI.sendAuraPointLostToPlayer(player, auraLost);
        return true;
    }

    public static InteractionResult waterClutchSucessEvent(Player player, Level level, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }

        // 1. Verify the item being used is a water bucket
        if (player.getMainHandItem().is(Items.WATER_BUCKET)) {

            // 2. Ensure they are placing it on top of a solid surface, and are actually falling
            if (blockHitResult.getDirection() == Direction.UP && player.fallDistance > 3.0f) {

                BlockPos targetPos = blockHitResult.getBlockPos();

                // 3. Precise proximity calculation: Player's Y versus the block surface Y
                // Since a block's top surface is at targetPos.getY() + 1.0
                double distanceToGround = player.getY() - (targetPos.getY() + 1.0);

                // A player's reach is ~4.5 blocks, but an MLG typically happens within 0 to 3 blocks of the floor
                if (distanceToGround >= 0.0 && distanceToGround <= 5.0) {

                    // Calculated blocks fallen: includes current momentum
                    double blocksFallen = player.fallDistance;

                    CustomPlayer customPlayer = CustomPlayer.getCustomPlayerOrNew(player);


                    double newAuraPoints = AuraAPI.auraPointGainedOnMLG(blocksFallen);

                    AuraAPI.sendAuraPointGainedToPlayer(player, newAuraPoints);
                    customPlayer.addPlayerAura(newAuraPoints);

                }
            }
        }
        return InteractionResult.PASS;

    }

    public static InteractionResult onAuraChanged(CustomPlayer customPlayer, double oldAmount, double newAmount) {

        customPlayer.getAssociatedPlayer().experienceLevel = (int) newAmount;

        return InteractionResult.PASS;
    }

    public static InteractionResult onPlayerAttack(Player player, Level level, InteractionHand interactionHand,
                                                   Entity entity, @Nullable EntityHitResult entityHitResult) {

        if (level.isClientSide()) return InteractionResult.PASS;

        ItemStack itemStack = player.getMainHandItem();

        if (AuraAPI.isItemOfAll(itemStack, List.of(ItemTags.SWORDS))) {
            int newAuraItem = AuraAPI.auraBonusForItemTier(itemStack);
            int auraItem = AuraAPI.getAuraDurability(itemStack);
            int maxAuraItem = AuraAPI.getMaxAuraDurability(itemStack);

            if (auraItem + newAuraItem >= maxAuraItem) {
                AuraAPI.setAuraDurability(itemStack, maxAuraItem);
                CustomPlayer.getCustomPlayerOrNew(player).addPlayerAura((auraItem + newAuraItem) - maxAuraItem);
            } else {
                AuraAPI.setAuraDurability(itemStack, auraItem + newAuraItem);
            }
        }

        return InteractionResult.PASS;

    }


    public static void onPlayerMissAttack(LeftClickPayload leftClickPayload, ServerPlayNetworking.Context context) {

        Player player = context.player();

        context.server().execute(() -> {
            CustomPlayer customPlayer = CustomPlayer.getCustomPlayerOrNew(player);



            ItemStack itemStack = player.getMainHandItem();

            if (AuraAPI.isItemOfAll(itemStack, List.of(ItemTags.SWORDS))) {
                int newAuraItem = AuraAPI.auraBonusForItemTier(itemStack);

                int auraItem = AuraAPI.getAuraDurability(itemStack);


                if (auraItem <= newAuraItem) {
                    AuraAPI.setAuraDurability(itemStack, 0);
                    customPlayer.removePlayerAura(newAuraItem - auraItem);
                } else {
                    AuraAPI.setAuraDurability(itemStack, auraItem - newAuraItem);
                }
            }
        });
    }
}
