package fr.miguel.component;

import fr.miguel.AuraFarm;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;

import java.util.UUID;

public class ModDataComponentTypes {


    public static final DataComponentType<Integer> AURA_ITEM_DURABILITY = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            AuraFarm.id("aura_item_durability"),
            DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT).build()
    );

    public static final DataComponentType<Integer> AURA_DICE_STATE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            AuraFarm.id("aura_dice_state"),
            DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DataComponentType<UUID> STAKE_ID = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            AuraFarm.id("stake_id"),
            DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build()
    );

    public static void register() {

    }
}
