package fr.miguel.pve.game;

import fr.miguel.AuraFarm;
import fr.miguel.pve.BattleEvent;
import fr.miguel.pve.Side;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class PvePayloads {

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(AuraFarm.id(path));
    }


    public static final byte ONGOING = 0, WON = 1, LOST = 2, FLED = 3, CANCELLED = 4;

    public static final byte ACT_ATTACK = 0, ACT_FLEE = 1;

    public record AttackInfo(String id, String nameKey, int hitPercent) {}

    public record Open(int enemyEntityId, String enemyName, int playerHp, int playerMaxHp,
                       int enemyHp, int enemyMaxHp, List<AttackInfo> attacks) implements CustomPacketPayload {

        public static final Type<Open> TYPE = payloadType("pve_open");
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = CustomPacketPayload.codec(Open::write, Open::new);

        private Open(FriendlyByteBuf b) {
            this(b.readVarInt(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), readAttacks(b));
        }

        private static List<AttackInfo> readAttacks(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<AttackInfo> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(new AttackInfo(b.readUtf(), b.readUtf(), b.readVarInt()));
            return list;
        }

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(enemyEntityId); b.writeUtf(enemyName);
            b.writeVarInt(playerHp); b.writeVarInt(playerMaxHp);
            b.writeVarInt(enemyHp); b.writeVarInt(enemyMaxHp);
            b.writeVarInt(attacks.size());
            for (AttackInfo a : attacks) { b.writeUtf(a.id()); b.writeUtf(a.nameKey()); b.writeVarInt(a.hitPercent()); }
        }

        @Override public @NonNull Type<Open> type() { return TYPE; }
    }

    // ---------- S2C ----------

    public record Turn(List<BattleEvent> events, int playerHp, int enemyHp, byte outcome, long auraDelta)
            implements CustomPacketPayload {

        public static final Type<Turn> TYPE = payloadType("pve_turn");
        public static final StreamCodec<FriendlyByteBuf, Turn> CODEC = CustomPacketPayload.codec(Turn::write, Turn::new);

        private Turn(FriendlyByteBuf b) {
            this(readEvents(b), b.readVarInt(), b.readVarInt(), b.readByte(), b.readLong());
        }

        private static List<BattleEvent> readEvents(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<BattleEvent> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(readEvent(b));
            return list;
        }

        private void write(FriendlyByteBuf b) {
            b.writeVarInt(events.size());
            for (BattleEvent e : events) writeEvent(b, e);
            b.writeVarInt(playerHp); b.writeVarInt(enemyHp); b.writeByte(outcome); b.writeLong(auraDelta);
        }

        @Override public @NonNull Type<Turn> type() { return TYPE; }
    }

    private static void writeEvent(FriendlyByteBuf b, BattleEvent e) {
        switch (e) {
            case BattleEvent.Used u -> { b.writeByte(0); b.writeByte(u.actor().ordinal()); b.writeUtf(u.attackNameKey()); }
            case BattleEvent.Miss m -> { b.writeByte(1); b.writeByte(m.target().ordinal()); }
            case BattleEvent.Hit h -> { b.writeByte(2); b.writeByte(h.target().ordinal()); b.writeVarInt(h.damage()); b.writeVarInt(h.hpAfter()); }
            case BattleEvent.Down d -> { b.writeByte(3); b.writeByte(d.who().ordinal()); }
            case BattleEvent.Fled f -> { b.writeByte(4); b.writeByte(f.who().ordinal()); }
        }
    }

    private static BattleEvent readEvent(FriendlyByteBuf b) {
        int type = b.readByte();
        Side side = Side.values()[b.readByte()];
        return switch (type) {
            case 0 -> new BattleEvent.Used(side, b.readUtf());
            case 1 -> new BattleEvent.Miss(side);
            case 2 -> new BattleEvent.Hit(side, b.readVarInt(), b.readVarInt());
            case 3 -> new BattleEvent.Down(side);
            case 4 -> new BattleEvent.Fled(side);
            default -> throw new IllegalArgumentException("Unknow event: " + type);
        };
    }

    // ---------- C2S ----------
    public record Action(byte action, String attackId) implements CustomPacketPayload {
        public static final Type<Action> TYPE = payloadType("pve_action");
        public static final StreamCodec<FriendlyByteBuf, Action> CODEC = CustomPacketPayload.codec(Action::write, Action::new);

        private Action(FriendlyByteBuf b) { this(b.readByte(), b.readUtf(128)); }
        private void write(FriendlyByteBuf b) { b.writeByte(action); b.writeUtf(attackId, 128); }

        @Override public Type<Action> type() { return TYPE; }
    }

    public record Close() implements CustomPacketPayload {
        public static final Close INSTANCE = new Close();
        public static final Type<Close> TYPE = payloadType("pve_close");
        public static final StreamCodec<ByteBuf, Close> CODEC = StreamCodec.unit(INSTANCE);

        @Override public Type<Close> type() { return TYPE; }
    }

    // ---------- REGISTRIES ----------

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Open.TYPE, Open.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Turn.TYPE, Turn.CODEC);
        registerToClientAndServer(Action.TYPE, Action.CODEC);
        registerToClientAndServer(Close.TYPE, Close.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (payload, context) -> {

            ServerPlayer player = context.player();

            try {
                switch (payload.action()) {
                    case ACT_ATTACK -> PveManager.attack(player, payload.attackId());
                    case ACT_FLEE -> PveManager.flee(player);
                }
            } catch (PveManager.PveException e) {
                player.sendSystemMessage(e.text());
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(Close.TYPE, (payload, context)
                -> PveManager.onClientClosed(context.player()));
    }

    private static <T extends CustomPacketPayload> void
    registerToClientAndServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {

        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
    }
}
