package fr.miguel.client.network;

import fr.miguel.client.gui.pve.PveScreen;
import fr.miguel.pve.game.PvePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PveClientNetwork {

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(PvePayloads.Open.TYPE, PvePayloads.Open.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PvePayloads.Turn.TYPE, PvePayloads.Turn.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(PvePayloads.Open.TYPE, (payload, context)
                -> context.client().setScreenAndShow(new PveScreen(payload)));

        ClientPlayNetworking.registerGlobalReceiver(PvePayloads.Turn.TYPE, (payload, context) -> {
           if (context.client().gui.screen() instanceof PveScreen screen) screen.onTurn(payload);
        });
    }

    private PveClientNetwork() {}
}
