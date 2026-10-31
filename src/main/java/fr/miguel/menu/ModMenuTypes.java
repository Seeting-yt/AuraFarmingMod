package fr.miguel.menu;

import fr.miguel.AuraFarm;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {

    public static final MenuType<AuraBattleMenu> AURA_BATTLE = Registry.register(
            BuiltInRegistries.MENU,
            AuraFarm.id("aura_battle_menu"),
            new MenuType<AuraBattleMenu>(AuraBattleMenu::new, FeatureFlagSet.of())
    );

    public static void init() {}
}
