package fr.miguel.events;

import fr.miguel.AuraFarm;
import fr.miguel.api.AuraAPI;
import fr.miguel.api.events.aura.AuraChangedEvent;
import fr.miguel.api.events.random.RandomAuraGambleEvent;
import fr.miguel.network.LeftClickPayload;
import fr.miguel.player.CustomPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * All player related registered events
 */
public final class PlayerEvents {

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

    public static void onAuraChanged(AuraChangedEvent event) {
        if (!event.isCanceled())
            event.getCustomPlayer().getAssociatedPlayer().experienceLevel = (int) event.getNewAmount();

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



    public static boolean onPlayerMine(Level level, Player player,
                                    BlockPos blockPos, BlockState blockState,
                                    @Nullable BlockEntity blockEntity) {

        if (level.isClientSide()) return true;

        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) return true;

        ItemStack itemStack = serverPlayer.getMainHandItem();

        if (itemStack.is(Items.WOODEN_PICKAXE) && blockState.is(Blocks.OBSIDIAN))
            onPlayerMineObsidian(serverLevel, itemStack, serverPlayer);

        if (AuraAPI.isItemOfAll(itemStack, List.of(ItemTags.PICKAXES))) {
            int newAuraItem = AuraAPI.auraBonusForItemTier(itemStack);

            int auraItem = AuraAPI.getAuraDurability(itemStack);

            int maxAuraItem = AuraAPI.getMaxAuraDurability(itemStack);


            boolean bonus = AuraAPI.isItemOfAll(itemStack, List.of(ItemTags.COAL_ORES,
                    ItemTags.COPPER_ORES, ItemTags.DIAMOND_ORES, ItemTags.EMERALD_ORES,
                    ItemTags.GOLD_ORES, ItemTags.IRON_ORES, ItemTags.LAPIS_ORES,
                    ItemTags.REDSTONE_ORES));

            if (bonus) {
                if (auraItem + newAuraItem >= maxAuraItem) {
                    AuraAPI.setAuraDurability(itemStack, maxAuraItem);
                    CustomPlayer.getCustomPlayerOrNew(player).addPlayerAura((auraItem + newAuraItem) - maxAuraItem);
                } else {
                    AuraAPI.setAuraDurability(itemStack, auraItem + newAuraItem);
                }
            } else {
                if (auraItem <= newAuraItem) {
                    AuraAPI.setAuraDurability(itemStack, 0);
                    CustomPlayer.getCustomPlayerOrNew(player).removePlayerAura(newAuraItem - auraItem);
                } else {
                    AuraAPI.setAuraDurability(itemStack, auraItem - newAuraItem);
                }
            }
        }

        return true;
    }

    public static void onPlayerMineObsidian(ServerLevel level, ItemStack itemStack,
                                            ServerPlayer serverPlayer) {

        var enchantRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        ItemEnchantments enchantments = itemStack.getEnchantments();

        List<ResourceKey<Enchantment>> criteriaList = List.of(Enchantments.FORTUNE,
                Enchantments.EFFICIENCY, Enchantments.MENDING, Enchantments.UNBREAKING);

        for (var criteria : criteriaList) {
            var holder = enchantRegistry.getOrThrow(criteria);

            int enchantmentLevel = enchantments.getLevel(holder);

            int maxLevel = holder.value().getMaxLevel();

            if (enchantmentLevel != maxLevel) {
                return;
            }
        }

        ServerAdvancementManager advancementManager = level.getServer().getAdvancements();
        AdvancementHolder advancement = advancementManager.get(AuraFarm.id("mine_obsidian"));

        if (advancement != null) {
            AdvancementProgress progress = serverPlayer.getAdvancements()
                    .getOrStartProgress(advancement);

            for (String criteria : progress.getRemainingCriteria())
                serverPlayer.getAdvancements().award(advancement, criteria);

        }
    }


    public static void onRandomGambling(RandomAuraGambleEvent event) {}

    public static void onRandomGamblingBefore(RandomAuraGambleEvent event) {}

    public static void onRandomGamblingAfter(RandomAuraGambleEvent event) {}


}
