package fr.miguel.api;

import fr.miguel.api.config.AuraConfig;
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

    public static double auraPointCostForEnchant(int enchantingLevel) {
        return enchantingLevel * AuraConfig.getInstance().enchantAuraMultiplier;
    }

    public static double auraPointCostForDamage(float damageAmount) {
        return damageAmount * AuraConfig.getInstance().damageAuraMultiplier;
    }

    public static double auraPointGainedOnMLG(double fallDistance) {
        return AuraConfig.getInstance().MLGScaleFactor * Math.pow(fallDistance, AuraConfig.getInstance().MLGExponent);
    }

    public static int auraBonusForItemTier(TagKey<Item> tag) {
        if (tag.equals(ItemTags.NETHERITE_TOOL_MATERIALS)) return AuraConfig.getInstance().netheriteBonus;
        if (tag.equals(ItemTags.DIAMOND_TOOL_MATERIALS))   return AuraConfig.getInstance().diamondBonus;
        if (tag.equals(ItemTags.GOLD_TOOL_MATERIALS))      return AuraConfig.getInstance().goldBonus;
        if (tag.equals(ItemTags.IRON_TOOL_MATERIALS))      return AuraConfig.getInstance().ironBonus;
        if (tag.equals(ItemTags.COPPER_TOOL_MATERIALS))    return AuraConfig.getInstance().copperBonus;
        if (tag.equals(ItemTags.STONE_TOOL_MATERIALS))     return AuraConfig.getInstance().stoneBonus;
        if (tag.equals(ItemTags.WOODEN_TOOL_MATERIALS))    return AuraConfig.getInstance().woodenBonus;
        return 0;
    }

    public static int auraBonusForItemTier(ItemStack itemStack) {
        if (itemStack.isDamageableItem()) {
            return (int) (itemStack.getMaxDamage() * 0.05);
        }
        return 0;
    }

    public static boolean hasAura(ItemStack itemStack) {
        return itemStack.has(ModDataComponentTypes.AURA_ITEM_DURABILITY);
    }


    public static int getAuraDurability(ItemStack itemStack) {
        return itemStack.getOrDefault(ModDataComponentTypes.AURA_ITEM_DURABILITY, getMaxAuraDurability(itemStack));
    }


    public static int getMaxAuraDurability(ItemStack itemStack) {

        if (itemStack.isDamageableItem())
            return 2 * itemStack.getMaxDamage();
        return 0;
    }


    public static void setAuraDurability(ItemStack itemStack, int value) {
        itemStack.set(ModDataComponentTypes.AURA_ITEM_DURABILITY, value);
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
