package fr.miguel.items;

import fr.miguel.api.tasks.DelayedTasks;
import fr.miguel.component.ModDataComponentTypes;
import fr.miguel.network.OpenScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

public class DiceItem extends Item {

    public DiceItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull InteractionResult use(Level level, Player player, @NonNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {

            MinecraftServer server = serverPlayer.level().getServer();
            ServerPlayNetworking.send(serverPlayer, new OpenScreenPayload());

            player.getInventory().removeItem(itemStack);

            DelayedTasks.schedule(server, 3, () -> itemStack.set(ModDataComponentTypes.AURA_DICE_STATE, 1));

            DelayedTasks.schedule(server, 6, () -> itemStack.set(ModDataComponentTypes.AURA_DICE_STATE, 2));

            DelayedTasks.schedule(server, 10, () -> itemStack.set(ModDataComponentTypes.AURA_DICE_STATE, 3));

            DelayedTasks.schedule(server, 11, () -> itemStack.set(ModDataComponentTypes.AURA_DICE_STATE, 0));
            return InteractionResult.CONSUME;
        }
    return InteractionResult.CONSUME;
    }

    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }
}
