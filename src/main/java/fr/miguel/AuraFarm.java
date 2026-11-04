package fr.miguel;

import fr.miguel.api.config.AuraConfig;
import fr.miguel.api.events.aura.AuraChangedCallBack;
import fr.miguel.api.events.random.RandomEventCallBack;
import fr.miguel.api.tasks.DelayedTasks;
import fr.miguel.battle.BattleCommand;
import fr.miguel.battle.BattleManager;
import fr.miguel.commands.AuraCommands;
import fr.miguel.component.ModDataComponentTypes;
import fr.miguel.events.PlayerEvents;
import fr.miguel.gambling_events.GamblingEvents;
import fr.miguel.items.ModItems;
import fr.miguel.menu.ModMenuTypes;
import fr.miguel.network.BattlePayloads;
import fr.miguel.network.LeftClickPayload;
import fr.miguel.network.OpenScreenPayload;
import fr.miguel.player.CustomPlayer;
import fr.miguel.player.PlayerPayloads;
import fr.miguel.pve.game.PveCommand;
import fr.miguel.pve.game.PveManager;
import fr.miguel.pve.game.PvePayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;

public class AuraFarm implements ModInitializer {

    public static final String MOD_ID = "miguelaurafarming";

    @Override
    public void onInitialize() {

        AuraConfig.load();

        DelayedTasks.init();

        registerComponentToBuiltInRegistries();

        registerPayloadTypes();

        registerEvents();

        registerPacketEvent();

        registerBattleRelated();

        registerPVEBattleRelated();
    }

    private void registerPVEBattleRelated() {
        PveManager.registerEvents();
        PvePayloads.register();
        PveCommand.register();
    }

    private void registerBattleRelated() {
        BattleCommand.register();

        BattleManager.registerEvents();

        ModMenuTypes.init();

        BattlePayloads.register();

    }

    private void registerPayloadTypes() {
        PayloadTypeRegistry.clientboundPlay().register(LeftClickPayload.TYPE, LeftClickPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LeftClickPayload.TYPE, LeftClickPayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(OpenScreenPayload.TYPE, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenScreenPayload.TYPE, OpenScreenPayload.CODEC);

        PlayerPayloads.register();
    }

    private void registerComponentToBuiltInRegistries() {
        ModDataComponentTypes.register();

        ModItems.registerModItems();
    }

    private void registerEvents() {
        ServerPlayerEvents.JOIN
                .register(CustomPlayer::createInstanceOnJoin);
        ServerPlayerEvents.LEAVE
                        .register(CustomPlayer::saveDataOnLeave);

        ServerLifecycleEvents.BEFORE_SAVE
                .register(CustomPlayer::savePlayersData);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(PlayerEvents::onPlayerGetHurt);

        UseBlockCallback.EVENT.register(PlayerEvents::waterClutchSucessEvent);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AuraCommands.register(dispatcher));

        AuraChangedCallBack.EVENT.register(PlayerEvents::onAuraChanged);

        AttackEntityCallback.EVENT.register(PlayerEvents::onPlayerAttack);

        RandomEventCallBack.EVENT.register(PlayerEvents::onRandomGambling);

        AuraChangedCallBack.EVENT.register(CustomPlayer::changePlayerXpValue);

        RandomEventCallBack.EVENT.register(eventInstance ->
                System.out.println("MAIN EVENT"));

        RandomEventCallBack.AfterGambling.EVENT.register(eventInstance ->
                System.out.println("AFTER GAMBLING"));

        RandomEventCallBack.BeforeScreen.EVENT.register(eventInstance ->
                System.out.println("BEFORE SCREEN"));
    }

    private void registerPacketEvent() {
        ServerPlayNetworking.registerGlobalReceiver(LeftClickPayload.TYPE, PlayerEvents::onPlayerMissAttack);

        ServerPlayNetworking.registerGlobalReceiver(PlayerPayloads.GamblingEvent.TYPE, GamblingEvents::activatePickedEventAfterGambling);
    }
    public static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath(MOD_ID, id);
    }
}
