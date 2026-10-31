package fr.miguel.items;

import fr.miguel.AuraFarm;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItems {
    private static final float DICE_COOLDOWN = 24 * 3600*0;

    public static final ResourceKey<Item> DICE_KEY =
            ResourceKey.create(Registries.ITEM, AuraFarm.id("aura_dice"));
    public static final Item DICE_ITEM =
            registerItem("aura_dice",
                    new DiceItem(new Item.Properties()
                            .stacksTo(1)
                            .useCooldown(DICE_COOLDOWN)
                            .setId(DICE_KEY)));

    public static Item registerItem(String name, Item item) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                AuraFarm.id(name),
                item
        );
    }
    public static void registerModItems() {

    }
}
