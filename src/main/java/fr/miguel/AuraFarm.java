package fr.miguel;

import fr.miguel.api.events.AuraChangedCallBack;
import fr.miguel.api.tasks.DelayedTasks;
import fr.miguel.battle.BattleCommand;
import fr.miguel.battle.BattleManager;
import fr.miguel.commands.AuraCommands;
import fr.miguel.component.ModDataComponentTypes;
import fr.miguel.events.PlayerEvents;
import fr.miguel.items.ModItems;
import fr.miguel.menu.ModMenuTypes;
import fr.miguel.network.BattlePayloads;
import fr.miguel.network.LeftClickPayload;
import fr.miguel.network.OpenScreenPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;

public class AuraFarm implements ModInitializer {

    public static final String MOD_ID = "miguelaurafarming";

    @Override
    public void onInitialize() {

        DelayedTasks.init();

        registerComponentToBuiltInRegistries();

        registerPayloadTypes();

        registerEvents();

        registerPacketEvent();

        registerBattleRelated();
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


    }

    private void registerComponentToBuiltInRegistries() {
        ModDataComponentTypes.register();

        ModItems.registerModItems();
    }

    private void registerEvents() {
        ServerPlayerEvents.JOIN
                .register(PlayerEvents::onPlayerJoin);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(PlayerEvents::onPlayerGetHurt);

        UseBlockCallback.EVENT.register(PlayerEvents::waterClutchSucessEvent);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AuraCommands.register(dispatcher));

        AuraChangedCallBack.EVENT.register(PlayerEvents::onAuraChanged);

        AttackEntityCallback.EVENT.register(PlayerEvents::onPlayerAttack);


    }

    private void registerPacketEvent() {
        ServerPlayNetworking.registerGlobalReceiver(LeftClickPayload.TYPE, PlayerEvents::onPlayerMissAttack);


    }
    public static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath(MOD_ID, id);
    }
}
