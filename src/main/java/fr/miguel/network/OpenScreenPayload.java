package fr.miguel.network;

import fr.miguel.AuraFarm;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record OpenScreenPayload() implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, OpenScreenPayload> CODEC =
            StreamCodec.unit(new OpenScreenPayload());
    public static final CustomPacketPayload.Type<OpenScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(AuraFarm.id("open_screen_packet"));

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
