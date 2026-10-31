package fr.miguel.network;

import fr.miguel.AuraFarm;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record LeftClickPayload() implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, LeftClickPayload> CODEC =
            StreamCodec.unit(new LeftClickPayload());
    public static final Type<LeftClickPayload> TYPE =
            new Type<>(AuraFarm.id("left_click_packet"));

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
