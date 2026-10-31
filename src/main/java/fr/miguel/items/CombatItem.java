package fr.miguel.items;

import fr.miguel.pve.game.PveManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class CombatItem extends Item {

    public CombatItem(Properties properties) { super(properties); }

    @Override
    public @NonNull InteractionResult interactLivingEntity(@NonNull ItemStack itemStack, Player player,
                                                           @NonNull LivingEntity target, @NonNull InteractionHand type) {

        return !player.level().isClientSide() && PveManager.launchCombat(player, target) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }
}
