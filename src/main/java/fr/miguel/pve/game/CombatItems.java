package fr.miguel.pve.game;

import fr.miguel.api.AuraAPI;
import fr.miguel.pve.core.ItemAction;
import fr.miguel.pve.core.registries.ItemActions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CombatItems {

    public record Usable(int slot, ItemStack itemStack, ItemAction action, double aura) {}

    private static final int MAX_LISTED = 6;
    private static final int SLOTS = 36;

    private CombatItems() {}

    private static @Nullable ItemAction actionFor(ItemStack itemStack) {
        if (itemStack.isEmpty() || !AuraAPI.hasAura(itemStack)) return null;

        if (itemStack.is(ItemTags.SWORDS)) return ItemActions.SWORD;
        return null;
    }

    static @Nullable Usable at(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= SLOTS) return null;

        ItemStack stack = player.getInventory().getItem(slot);
        ItemAction action = actionFor(stack);
        return action == null ? null : new Usable(slot, stack, action, AuraAPI.getAuraDurability(stack));
    }

    static List<Usable> usable(ServerPlayer player) {
        List<Usable> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            Usable usable = at(player, i);
            if (usable != null) list.add(usable);
        }

        list.sort(Comparator.comparingDouble(Usable::aura).reversed());
        return list.size() > MAX_LISTED ? new ArrayList<>(list.subList(0, MAX_LISTED)) : list;
    }
}
