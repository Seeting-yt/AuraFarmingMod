package fr.miguel.pve.game;

import fr.miguel.battle.BattleManager;
import fr.miguel.player.CustomPlayer;
import fr.miguel.pve.*;
import fr.miguel.pve.attack.AttackInterface;
import fr.miguel.pve.attack.AttackRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
    private static final double MAX_DISTANCE = 32;
    private static final boolean KILL_ENEMY_ON_WIN = true;

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

        serverPlayer.sendSystemMessage(Component.translatable("pve.start",
                enemySide.name(), (long) enemySide.aura(), enemySide.maxHp(), playerSide.maxHp()));
        serverPlayer.sendSystemMessage(PveText.status(battle));
        return true;
    }

    public static void attack(ServerPlayer player, String attackId) throws PveException {
        PveSession s = BY_PLAYER.get(player.getUUID());
        if (s == null) throw new PveException("pve.error.no_combat");
        AttackInterface attack = AttackRegistry.get(attackId).orElseThrow(() -> new PveException("pve.error.unknown_attack"));
        // TODO : restreindre aux attaques que ce joueur possède

        List<BattleEvent> events = s.battle.playerAttack(attack);
        for (BattleEvent e : events) player.sendSystemMessage(PveText.describe(e, s.battle));
        player.sendSystemMessage(PveText.status(s.battle));

        if (s.battle.outcome() == PveBattle.Outcome.ONGOING) s.deadlineTick = server.getTickCount() + TURN_TIMEOUT_TICKS;
        else end(s, player, true);
    }

    public static void flee(ServerPlayer player) throws PveException {
        PveSession s = BY_PLAYER.get(player.getUUID());
        if (s == null) throw new PveException("pve.error.no_combat");
        for (BattleEvent e : s.battle.flee()) player.sendSystemMessage(PveText.describe(e, s.battle));
        end(s, player, true);
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        for (PveSession session : new ArrayList<>(BY_PLAYER.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            if (player == null) { end(session, null, false); continue; }

            boolean abnormal = !session.mob.isAlive() || session.mob.isRemoved() || !player.isAlive()
                    || session.mob.level() != player.level() || session.mob.distanceToSqr(player) > MAX_DISTANCE * MAX_DISTANCE;
            if (abnormal) {                       // combat annulé sans pénalité
                end(session, player, false);
                player.sendSystemMessage(PveText.result(PveBattle.Outcome.ONGOING, 0));
            } else if (now >= session.deadlineTick) {   // trop long : fuite
                player.sendSystemMessage(Component.translatable("pve.timeout"));
                for (BattleEvent e : session.battle.flee()) player.sendSystemMessage(PveText.describe(e, session.battle));
                end(session, player, true);
            }
        }
    }

    private static void onDisconnect(ServerPlayer player) {
        PveSession session = BY_PLAYER.get(player.getUUID());
        if (session == null) return;
        session.battle.flee();       // déconnexion = fuite, avec sa pénalité (appliquée avant la sauvegarde du joueur)
        end(session, player, true);
    }

    private static void abortAll() {
        for (PveSession s : new ArrayList<>(BY_PLAYER.values()))
            end(s, server == null ? null : server.getPlayerList().getPlayer(s.playerId), false);
    }

    private static void end(PveSession session, @Nullable ServerPlayer player, boolean applyOutcome) {
        BY_PLAYER.remove(session.playerId);
        BY_ENTITY.remove(session.mob.getUUID());

        session.mob.setNoAi(session.mobWasNoAI);
        session.mob.setPermanentlyInvulnerable(session.mobWasInvulnerable);
        if (player != null) player.setPermanentlyInvulnerable(session.playerWasInvulnerable);
        if (player == null || !applyOutcome) return;

        long delta = session.battle.playerAuraDelta();
        CustomPlayer.getCustomPlayerOrNew(player).addPlayerAura(Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, delta)));
        player.sendSystemMessage(PveText.result(session.battle.outcome(), delta));

        if (session.battle.outcome() == PveBattle.Outcome.WON && KILL_ENEMY_ON_WIN && session.mob.level() instanceof ServerLevel level)
            session.mob.kill(level);
    }
}
