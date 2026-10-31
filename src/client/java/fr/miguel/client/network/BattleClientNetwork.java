package fr.miguel.client.network;

import fr.miguel.client.battle.ClientBattleState;
import fr.miguel.client.gui.battle.AuraBattleScreen;
import fr.miguel.client.gui.battle.BattleChallengeScreen;
import fr.miguel.client.gui.battle.BattleWaitingScreen;
import fr.miguel.menu.ModMenuTypes;
import fr.miguel.network.BattlePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public final class BattleClientNetwork {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(BattlePayloads.OpenWaiting.TYPE, ((payload, context) ->
                context.client().setScreenAndShow(new BattleWaitingScreen(payload.targetName()))));

        ClientPlayNetworking.registerGlobalReceiver(BattlePayloads.OpenChallenge.TYPE, ((payload, context) ->
                context.client().setScreenAndShow(new BattleChallengeScreen(payload.challengerName()))));

        ClientPlayNetworking.registerGlobalReceiver(BattlePayloads.CloseScreen.TYPE, (payload, context) -> {
            var screen = context.client().gui.screen();

            if (screen instanceof BattleWaitingScreen || screen instanceof BattleChallengeScreen)
                context.client().gui.setScreen(null);
        });

        ClientPlayNetworking.registerGlobalReceiver(BattlePayloads.BattleState.TYPE,
                (payload, context) -> ClientBattleState.set(payload));

        MenuScreens.register(ModMenuTypes.AURA_BATTLE, AuraBattleScreen::new);
    }
}
