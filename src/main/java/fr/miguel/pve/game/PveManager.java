package fr.miguel.pve.game;

import fr.miguel.battle.BattleManager;
import fr.miguel.player.CustomPlayer;
import fr.miguel.pve.core.*;
import fr.miguel.pve.core.attack.AttackInterface;
import fr.miguel.pve.core.mob.MobProfile;
import fr.miguel.pve.core.mob.MobProfiles;
import fr.miguel.pve.core.registries.AttackRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.random.RandomGenerator;

public final class PveManager {

    private static final int TURN_TIMEOUT_TICKS = 20 * 30;
    private static final int CLOSE_TIMEOUT_TICKS = 20 * 20;
    private static final double MAX_DISTANCE = 32;
    private static final boolean KILL_ENEMY_ON_WIN = false;

    private static final Map<UUID, PveSession> BY_PLAYER = new HashMap<>();
    private static final Map<UUID, PveSession> BY_ENTITY = new HashMap<>();

    private static MinecraftServer server;

    private static final Logger LOGGER = LoggerFactory.getLogger("PveManager");

    private PveManager() {}

    public static final class PveException extends Exception {
        private final Component text;

        public PveException(String translationKey) {
            super(translationKey);
            this.text = Component.translatable(translationKey);
        }

        public Component text() {
            return this.text;
        }
    }

    public static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);

        ServerLifecycleEvents.SERVER_STOPPING.register(s -> abortAll());
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
        ServerTickEvents.END_SERVER_TICK.register(PveManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server1) -> onDisconnect(listener.getPlayer()));

    }

    public static boolean isBusy(UUID player) {
        return BY_PLAYER.containsKey(player);
    }

    public static boolean launchCombat(Player player, Entity entity) {
        if (server == null) return false;

        if (!(player instanceof ServerPlayer serverPlayer) || !(entity instanceof Mob mob)) return false;

        if (!serverPlayer.isAlive() || serverPlayer.isSpectator() || !mob.isAlive()) return false;

        if (isBusy(serverPlayer.getUUID()) || BY_ENTITY.containsKey(mob.getUUID())) return false;

        if (BattleManager.sessionOf(serverPlayer.getUUID()).isPresent()) return false;

        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        MobProfile profile = MobProfiles.get(typeId).orElse(null);

        if (profile == null) return false;

        List<AttackInterface> attacks = profile.attackIds().stream()
                .map(AttackRegistry::get).flatMap(Optional::stream).toList();

        if (attacks.isEmpty()) return false;

        Combatant playerSide = new Combatant(Side.PLAYER, serverPlayer.getName().getString(), CustomPlayer.getCustomPlayerOrNew(player).getPlayerAura());
        Combatant enemySide = new Combatant(Side.ENEMY, mob.getName().getString(), profile.aura());

        PveBattle battle = new PveBattle(playerSide, enemySide, attacks, new BattleContext(RandomGenerator.getDefault()));

        PveSession session = new PveSession(serverPlayer.getUUID(), mob, battle,
                server.getTickCount() + TURN_TIMEOUT_TICKS, serverPlayer.isInvulnerable());

        BY_ENTITY.put(mob.getUUID(), session);
        BY_PLAYER.put(serverPlayer.getUUID(), session);

        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setNoAi(true);
        mob.setPermanentlyInvulnerable(true);
        serverPlayer.setPermanentlyInvulnerable(true);

        List<PvePayloads.AttackInfo> infos = new ArrayList<>();

        for (AttackInterface attack : AttackRegistry.all())
            infos.add(new PvePayloads.AttackInfo(attack.id(), attack.nameKey(),
                    (int) Math.round(100 * BattleMath.hitChance(playerSide.aura(), enemySide.aura(), attack.accuracy()))));

        List<PvePayloads.ItemInfo> items = new ArrayList<>();
        for (CombatItems.Usable usable : CombatItems.usable(serverPlayer))
            items.add(new PvePayloads.ItemInfo(usable.slot(), (int) Math.round(usable.aura()),
                    (int) Math.round(100 * BattleMath.hitChance(playerSide.aura(), enemySide.aura(), usable.action().accuracy()))));

        ServerPlayNetworking.send(serverPlayer, new PvePayloads.Open(mob.getId(), enemySide.name(),
                playerSide.hp(), playerSide.maxHp(), enemySide.hp(), enemySide.maxHp(), infos, items));
        return true;
    }

    public static void useItem(ServerPlayer player, int slot) throws PveException {
        PveSession session = ongoing(player);
        CombatItems.Usable usable = CombatItems.at(player, slot);
        if (usable == null) throw new PveException("pve.error.bad_item");
        afterTurn(session, player, session.battle.playerUseItem(usable.action(), usable.aura()));
    }

    public static void attack(ServerPlayer player, String attackId) throws PveException {
        PveSession session = BY_PLAYER.get(player.getUUID());
        if (session == null) throw new PveException("pve.error.no_combat");
        AttackInterface attack = AttackRegistry.get(attackId).orElseThrow(() -> new PveException("pve.error.unknown_attack"));

        afterTurn(session, player, session.battle.playerAttack(attack));
    }

    public static void flee(ServerPlayer player) throws PveException {
        PveSession session = BY_PLAYER.get(player.getUUID());

        afterTurn(session, player, session.battle.flee());
    }

    public static void onClientClosed(ServerPlayer player) {
        PveSession session = BY_PLAYER.get(player.getUUID());
        if (session != null && session.closing) finish(session, player);
    }

    private static PveSession ongoing(ServerPlayer player) throws PveException {
        PveSession session = BY_PLAYER.get(player.getUUID());
        if (session == null || session.closing) throw new PveException("pve.error.no_combat");

        return session;
    }

    private static void afterTurn(PveSession session, ServerPlayer player, List<BattleEvent> events) {
        PveBattle battle = session.battle;
        long delta = 0;

        if (battle.outcome() == PveBattle.Outcome.ONGOING) {
            session.deadlineTick = now() + TURN_TIMEOUT_TICKS;
        } else {
            delta = applyOutcome(session, player);
            session.closing = true;
            session.deadlineTick = now() + CLOSE_TIMEOUT_TICKS;
        }

        ServerPlayNetworking.send(player, new PvePayloads.Turn(events, battle.player().hp(), battle.enemy().hp(),
                outcomeCode(battle.outcome()), delta));
    }

    private static long applyOutcome(PveSession session, ServerPlayer player) {
        long delta = session.battle.playerAuraDelta();

        CustomPlayer.getCustomPlayerOrNew(player).addPlayerAura(Math.clamp(delta, Integer.MIN_VALUE, Integer.MAX_VALUE));

        String key = switch (session.battle.outcome()) {
            case WON -> "pve.result.won";
            case LOST -> "pve.result.lost";
            case FLED -> "pve.result.fled";
            case ONGOING -> "pve.result.cancelled";
        };

        player.sendSystemMessage(Component.translatable(key, String.format("%+d", delta)));
        return delta;
    }

    private static byte outcomeCode(PveBattle.Outcome outcome) {
        return switch (outcome) {
            case ONGOING -> PvePayloads.ONGOING;
            case WON -> PvePayloads.WON;
            case LOST -> PvePayloads.LOST;
            case FLED -> PvePayloads.FLED;
        };
    }

    // ---------- TICKS / DISCONNECTION ----------

    private static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        for (PveSession session : new ArrayList<>(BY_PLAYER.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            if (player == null) { finish(session, null); continue; }

            if (session.closing) {
                if (now >= session.deadlineTick) finish(session, player);
                continue;
            }

            boolean abnormal = !session.mob.isAlive() || session.mob.isRemoved() || !player.isAlive()
                    || session.mob.level() != player.level() || session.mob.distanceToSqr(player) > MAX_DISTANCE * MAX_DISTANCE;
            if (abnormal) {
                cancel(session, player);
            } else if (now >= session.deadlineTick) {
                player.sendSystemMessage(Component.translatable("pve.timeout"));
                afterTurn(session, player, session.battle.flee());
            }
        }
    }

    private static void cancel(PveSession session, ServerPlayer player) {
        ServerPlayNetworking.send(player, new PvePayloads.Turn(List.of(), session.battle.player().hp(),
                session.battle.enemy().hp(), PvePayloads.CANCELLED, 0));
        player.sendSystemMessage(Component.translatable("pve.result.cancelled"));
        finish(session, player);
    }

    private static void onDisconnect(ServerPlayer player) {
        PveSession session = BY_PLAYER.get(player.getUUID());
        if (session == null) return;
        if (!session.closing) {
            session.battle.flee();
            applyOutcome(session, player);
        }
        finish(session, player);
    }

    private static void abortAll() {
        for (PveSession s : new ArrayList<>(BY_PLAYER.values()))
            finish(s, server == null ? null : server.getPlayerList().getPlayer(s.playerId));
    }

    // ---------- END ----------

    private static void finish(PveSession session, @Nullable ServerPlayer player) {
        BY_PLAYER.remove(session.playerId);
        BY_ENTITY.remove(session.mob.getUUID());

        session.mob.setNoAi(session.mobWasNoAI);
        session.mob.setPermanentlyInvulnerable(session.mobWasInvulnerable);
        if (player != null) player.setPermanentlyInvulnerable(session.playerWasInvulnerable);

        if (session.battle.outcome() == PveBattle.Outcome.WON && KILL_ENEMY_ON_WIN
                && session.mob.isAlive() && session.mob.level() instanceof ServerLevel level)
            session.mob.kill(level);
    }

    private static long now() {
        return server.getTickCount();
    }
}
