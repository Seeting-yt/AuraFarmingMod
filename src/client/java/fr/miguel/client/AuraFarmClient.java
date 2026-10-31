package fr.miguel.client;

import fr.miguel.client.events.ClientHUDEvents;
import fr.miguel.client.gui.AuraGamblingScreen;
import fr.miguel.client.network.BattleClientNetwork;
import fr.miguel.network.LeftClickPayload;
import fr.miguel.network.OpenScreenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class AuraFarmClient implements ClientModInitializer {
    private static final Identifier AURA_BAR_TEXT = Identifier.fromNamespaceAndPath("miguelaurafarming", "aura_bar_text");
    @Override
    public void onInitializeClient() {

        HUDElementRegister();

        BattleClientNetwork.register();

        //registerPayloadTypes();

        registerPayLoadEvents();

    }

    private void registerPayLoadEvents() {
        ClientPlayNetworking.registerGlobalReceiver(OpenScreenPayload.TYPE,
                ((payload, context)
                        -> Minecraft.getInstance().execute(()
                        -> Minecraft.getInstance().setScreenAndShow(new AuraGamblingScreen()))));
    }

    private void registerPayloadTypes() {
        PayloadTypeRegistry.clientboundPlay().register(OpenScreenPayload.TYPE, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenScreenPayload.TYPE, OpenScreenPayload.CODEC);
    }

    private void HUDElementRegister() {
        HudElementRegistry
                .attachElementAfter(VanillaHudElements.HOTBAR, AURA_BAR_TEXT, ClientHUDEvents::createAuraBar);
    }
}
