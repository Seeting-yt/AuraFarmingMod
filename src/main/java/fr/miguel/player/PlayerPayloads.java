package fr.miguel.player;

import fr.miguel.AuraFarm;
import fr.miguel.network.OpenScreenPayload;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class PlayerPayloads {

    public record AuraChangedPayload(UUID playerID, double playerAura) implements CustomPacketPayload {

        public static final Type<AuraChangedPayload> TYPE =
                new CustomPacketPayload.Type<>(AuraFarm.id("aura_changed_payload"));

        public static final StreamCodec<ByteBuf, AuraChangedPayload> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, AuraChangedPayload::playerID,
                ByteBufCodecs.DOUBLE, AuraChangedPayload::playerAura,
                AuraChangedPayload::new
        );
        @Override
        public @NonNull Type<AuraChangedPayload> type() {
            return TYPE;
        }
    }

    public record GamblingEvent(UUID playerID, double auraBet) implements CustomPacketPayload {

        public static final Type<GamblingEvent> TYPE =
                new CustomPacketPayload.Type<>(AuraFarm.id("aura_gambling_payload"));

        public static final StreamCodec<ByteBuf, GamblingEvent> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, GamblingEvent::playerID,
                ByteBufCodecs.DOUBLE, GamblingEvent::auraBet,
                GamblingEvent::new
        );
        @Override
        public @NonNull Type<GamblingEvent> type() {
            return TYPE;
        }
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(AuraChangedPayload.TYPE, AuraChangedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AuraChangedPayload.TYPE, AuraChangedPayload.CODEC);


        PayloadTypeRegistry.clientboundPlay().register(GamblingEvent.TYPE, GamblingEvent.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GamblingEvent.TYPE, GamblingEvent.CODEC);

    }
}
