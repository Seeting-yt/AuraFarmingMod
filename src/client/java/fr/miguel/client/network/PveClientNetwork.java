package fr.miguel.client.network;

import fr.miguel.client.gui.pve.PveScreen;
import fr.miguel.pve.game.PvePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class PveClientNetwork {

    public static void register() {
        registerToClientAndServer(PvePayloads.Open.TYPE, PvePayloads.Open.CODEC);
        registerToClientAndServer(PvePayloads.Turn.TYPE, PvePayloads.Turn.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(PvePayloads.Open.TYPE, (payload, context)
                -> context.client().setScreenAndShow(new PveScreen(payload)));

        ClientPlayNetworking.registerGlobalReceiver(PvePayloads.Turn.TYPE, (payload, context) -> {
           if (context.client().gui.screen() instanceof PveScreen screen) screen.onTurn(payload);
        });
    }

    private PveClientNetwork() {}

    private static <T extends CustomPacketPayload> void
    registerToClientAndServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {

        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
    }
}
