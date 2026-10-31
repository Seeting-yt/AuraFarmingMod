package fr.miguel.api;

import fr.miguel.component.ModDataComponentTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;


public class AuraAPI {

    public static final List<TagKey<Item>> TIERED_ITEMS_TAGS = List.of(ItemTags.WOODEN_TOOL_MATERIALS, ItemTags.IRON_TOOL_MATERIALS,
            ItemTags.COPPER_TOOL_MATERIALS, ItemTags.STONE_TOOL_MATERIALS, ItemTags.GOLD_TOOL_MATERIALS,
            ItemTags.DIAMOND_TOOL_MATERIALS, ItemTags.NETHERITE_TOOL_MATERIALS);

    //TODO CHANGE AURA POINTS CALCULUS THAT ARE JUST ABSURD
    private static final String someStringM = "a0d58aae24d5b32c32d84a07ec23653a47fdf44ef3b96e4af25529dcf684c1d5";
    public static double auraPointCostForEnchant(int enchantingLevel) {
        return (int) (67*Math.log(6.7)*enchantingLevel); // = 127 * level
    }

    public static double auraPointCostForDamage(float damageAmount) {
        return Math.log((Math.abs(Long.parseUnsignedLong(someStringM.substring(0, 16), 16)) * damageAmount) + 1);
    }

    public static double auraPointGainedOnMLG(double fallDistance) {
        return 1 + Math.exp(1.67 * Math.log(fallDistance));
    }

    public static void sendAuraPointLostToPlayer(Player player, double amount) {
        player.sendSystemMessage(Messages.placeHolder("message.player.lost_aura",
                Messages.AURA_POINTS_PLACEHOLDER,
                (int) amount));
    }

    public static void sendAuraPointGainedToPlayer(Player player, double amount) {
        player.sendSystemMessage(Messages.placeHolder("message.player.gained_aura",
                Messages.AURA_POINTS_PLACEHOLDER,
                (int) amount));
    }

    public static Component auraCostMessage(int enchantingLevel) {

        return Messages.placeHolder("tooltip.enchant.aura_cost",
                Messages.AURA_POINTS_PLACEHOLDER, (int) auraPointCostForEnchant(enchantingLevel));
    }

    public static Component auraAddedCommandMessage(Player player, double amount) {
        return Messages.placeHolder("message.player.commands.added_aura",
                List.of(Messages.TARGET_PLACEHOLDER, Messages.AURA_POINTS_PLACEHOLDER),
                List.of(player.getPlainTextName(), amount));
    }

    public static Component auraGetCommandMessage(double amount) {
        return Messages.placeHolder("message.player.commands.get_aura",
                Messages.AURA_POINTS_PLACEHOLDER,
                amount);
    }

    public static boolean hasAura(ItemStack itemStack) {
        return itemStack.has(ModDataComponentTypes.AURA_ITEM_DURABILITY);
    }


    public static int getAuraDurability(ItemStack itemStack) {
        return itemStack.getOrDefault(ModDataComponentTypes.AURA_ITEM_DURABILITY, getMaxAuraDurability(itemStack));
    }


    public static int getMaxAuraDurability(ItemStack itemStack) {

        if (itemStack.isDamageableItem())
            return 67*itemStack.getMaxDamage();
        return 0;
    }


    public static void setAuraDurability(ItemStack itemStack, int value) {
        itemStack.set(ModDataComponentTypes.AURA_ITEM_DURABILITY, value);
    }

    public static int auraBonusForItemTier(TagKey<Item> tag) {
        if (tag.equals(ItemTags.NETHERITE_TOOL_MATERIALS)) {
            return (int) Math.exp(6.7 * Math.log(20));
        } else if (tag.equals(ItemTags.DIAMOND_TOOL_MATERIALS)) {
            return (int) Math.exp(6.7 * Math.log(17));
        } else if (tag.equals(ItemTags.GOLD_TOOL_MATERIALS)) {
            return (int) Math.exp(6.7 * Math.log(8));
        } else if (tag.equals(ItemTags.IRON_TOOL_MATERIALS)) {
            return (int) Math.exp(6.7 * Math.log(10));
        } else if (tag.equals(ItemTags.COPPER_TOOL_MATERIALS)) {
            return (int) Math.exp(6.7 * Math.log(7));
        } else if (tag.equals(ItemTags.STONE_TOOL_MATERIALS)) {
            return (int) Math.exp(6.7 * Math.log(5));
        } else if (tag.equals(ItemTags.WOODEN_TOOL_MATERIALS)) {
            return (int) Math.exp(6.7 * Math.log(2));
        }
        return 0;
    }

    public static int auraBonusForItemTier(ItemStack itemStack) {
        if (itemStack.isDamageableItem()) {
            return (int) Math.log(itemStack.getMaxDamage() * Math.exp(6.7) + 1);
        }
        return 0;
    }

    public static boolean isItemOfAll(ItemStack itemStack, List<TagKey<Item>> tags) {
       for (TagKey<Item> tag : tags) {
            if (itemStack.tags().noneMatch(tag::equals))
                return false;
        }

        return true;
    }

    public static boolean isItemOfOne(ItemStack itemStack, List<TagKey<Item>> tags) {
        return tags.stream().anyMatch(itemStack::is);
    }

    public static boolean isItemTired(ItemStack itemStack) {
        return isItemOfOne(itemStack, TIERED_ITEMS_TAGS);
    }



    public static class Messages {

        public static final String TARGET_PLACEHOLDER = "{{target}}";
        public static final String AURA_POINTS_PLACEHOLDER = "{{aura_points}}";

        public static Component placeHolder(String key, String placeHolder, Object replacement) {
            return placeHolder(key, List.of(placeHolder), List.of(replacement));
        }

        public static Component placeHolder(String key, List<String> placeHolder, List<Object> replacement) {
            return Messages.placeHolder(Component.translatable(key), placeHolder, replacement);
        }

        public static Component placeHolder(Component rawMessage, List<String> placeHolders, List<Object> replacement) {

            if (placeHolders.size() != replacement.size())
                throw new IllegalArgumentException("two arrays of different sizes");
            Component modifiedMessage = rawMessage.copy();
            for (int i = 0; i < placeHolders.size(); i++) {
                modifiedMessage = Component.literal(modifiedMessage
                        .getString().replace(placeHolders.get(i),
                                replacement.get(i).toString()));
            }
            return modifiedMessage;
        }
    }
}
