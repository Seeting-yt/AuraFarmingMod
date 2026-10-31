package fr.miguel.network;

import fr.miguel.AuraFarm;
import fr.miguel.battle.BattleManager;
import fr.miguel.menu.AuraBattleMenu;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

public final class BattlePayloads {

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(AuraFarm.id(path));
    }

    public record OpenWaiting(String targetName) implements CustomPacketPayload {
        public static final Type<OpenWaiting> TYPE = payloadType("battle_open_waiting");

        public static final StreamCodec<ByteBuf, OpenWaiting> CODEC =
                ByteBufCodecs.STRING_UTF8.map(OpenWaiting::new, OpenWaiting::targetName);

        @Override public Type<OpenWaiting> type() { return TYPE; }
    }

    public record OpenChallenge(String challengerName) implements CustomPacketPayload {
        public static final Type<OpenChallenge> TYPE = payloadType("battle_open_challenge");

        public static final StreamCodec<ByteBuf, OpenChallenge> CODEC =
                ByteBufCodecs.STRING_UTF8.map(OpenChallenge::new, OpenChallenge::challengerName);

        @Override public Type<OpenChallenge> type() { return TYPE; }
    }

    public record CloseScreen() implements CustomPacketPayload {
        public static final CloseScreen INSTANCE = new CloseScreen();

        public static final Type<CloseScreen> TYPE = payloadType("battle_close_screen");
        public static final StreamCodec<ByteBuf, CloseScreen> CODEC = StreamCodec.unit(INSTANCE);

        @Override public Type<CloseScreen> type() { return TYPE; }
    }

    // ---------------- C2S -----------------

    public record BattleAction(byte action) implements CustomPacketPayload {

        public static final byte ACCEPT = 0;
        public static final byte REFUSE = 1;
        public static final byte CANCEL = 2;

        public static final Type<BattleAction> TYPE = payloadType("battle_action");
        public static final StreamCodec<ByteBuf, BattleAction> CODEC =
                ByteBufCodecs.BYTE.map(BattleAction::new, BattleAction::action);

        @Override public @NonNull Type<BattleAction> type() { return TYPE; }
    }

    public record MenuAction(int action, int value) implements CustomPacketPayload {
        public static final int TOGGLE = 0;    // value = menu slot's index
        public static final int SET_AURA = 1;  // value = aura bet
        public static final int ALL_IN = 2;
        public static final int VALIDATE = 3;
        public static final int EDIT = 4;      // cancel self-validation
        public static final int CANCEL = 5;

        public static final Type<MenuAction> TYPE = payloadType("battle_menu_action");
        public static final StreamCodec<FriendlyByteBuf, MenuAction> CODEC =
                CustomPacketPayload.codec(MenuAction::write, MenuAction::new);

        private MenuAction(FriendlyByteBuf b) { this(b.readVarInt(), b.readVarInt()); }
        private void write(FriendlyByteBuf b) { b.writeVarInt(action); b.writeVarInt(value); }

        @Override public @NonNull Type<MenuAction> type() { return TYPE; }
    }

    // ---------------- S2C -----------------

    public record BattleState(int myAura, int maxAura, boolean myValidated, int myItemCount, long myItemsAura,
                              String oppName, int oppAura, boolean oppValidated, int oppItemCount, long oppItemsAura) implements CustomPacketPayload {

        public static final Type<BattleState> TYPE = payloadType("battle_state");

        public static final StreamCodec<FriendlyByteBuf, BattleState> CODEC =
                CustomPacketPayload.codec(BattleState::write, BattleState::new);

        private BattleState(FriendlyByteBuf b) {
            this(b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readVarLong(),
                    b.readUtf(), b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readVarLong());
        }

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(myAura); b.writeVarInt(maxAura); b.writeBoolean(myValidated);
            b.writeVarInt(myItemCount); b.writeVarLong(myItemsAura);
            b.writeUtf(oppName); b.writeVarInt(oppAura); b.writeBoolean(oppValidated);
            b.writeVarInt(oppItemCount); b.writeVarLong(oppItemsAura);
        }

        @Override public @NonNull Type<BattleState> type() { return TYPE; }
    }

    public static void register() {

        registerToClientAndServer(OpenWaiting.TYPE, OpenWaiting.CODEC);

        registerToClientAndServer(OpenChallenge.TYPE, OpenChallenge.CODEC);

        registerToClientAndServer(CloseScreen.TYPE, CloseScreen.CODEC);

        registerToClientAndServer(BattleAction.TYPE, BattleAction.CODEC);

        registerToClientAndServer(BattleState.TYPE, BattleState.CODEC);

        registerToClientAndServer(MenuAction.TYPE, MenuAction.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(BattleAction.TYPE, ((payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();

            try {
                switch (payload.action()) {
                    case BattleAction.ACCEPT -> BattleManager.accept(server, player);
                    case BattleAction.REFUSE -> BattleManager.refuse(server, player);
                    case BattleAction.CANCEL -> BattleManager.cancel(server, player);

                    default -> {}
                }
            } catch (BattleManager.BattleException e) {
                player.sendSystemMessage(e.text());
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(MenuAction.TYPE, (payload, context) -> {
            if (context.player().containerMenu instanceof AuraBattleMenu menu)
                menu.handleAction(context.player(), payload.action(), payload.value());
        });


    }

    private static <T extends CustomPacketPayload> void
    registerToClientAndServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {

        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
    }
}
