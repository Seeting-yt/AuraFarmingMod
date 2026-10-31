package fr.miguel.battle;

import fr.miguel.api.AuraAPI;
import fr.miguel.component.ModDataComponentTypes;
import fr.miguel.player.CustomPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class StakeUtil {

    private StakeUtil() {}

    public static boolean isStaked(ItemStack stack, UUID sessionID) {
        return !stack.isEmpty() && sessionID.equals(stack.get(ModDataComponentTypes.STAKE_ID));
    }

    public static List<ItemStack> stakedItems(Player player, UUID sessionID) {
        List<ItemStack> out = new ArrayList<>();

        Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (isStaked(item, sessionID)) out.add(item);
        }

        return out;
    }

    public static int count(Player player, UUID sessionID) {
        return stakedItems(player, sessionID).size();
    }

    public static long itemsAura(Player player, UUID sessionID) {
        long total = 0;
        for (ItemStack stack : stakedItems(player, sessionID)) total += itemAura(stack);

        return total;
    }

    public static void clearMarks(Player player, UUID sessionID) {
        for (ItemStack itemStack : stakedItems(player, sessionID)) itemStack.remove(ModDataComponentTypes.STAKE_ID);
    }

    public static int playerAura(Player player) {
        return (int) CustomPlayer.getCustomPlayerOrNew(player).getPlayerAura();
    }

    public static long itemAura(ItemStack stack) {
        return AuraAPI.getAuraDurability(stack);
    }
}
