package fr.miguel.battle;

import com.google.common.collect.ImmutableMap;
import fr.miguel.AuraFarm;
import fr.miguel.component.ModDataComponentTypes;
import fr.miguel.menu.AuraBattleMenu;
import fr.miguel.network.BattlePayloads;
import fr.miguel.player.CustomPlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class BattleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuraFarm.MOD_ID + "/battle");

    private static final int PENDING_TIMEOUT_TICKS = 20 * 30;
    private static final int FIGHT_GRACE_TICKS = 20 * 3;
    private static final int FIGHT_TIME_LIMIT_TICKS = 20 * 60 * 5;

    private static final Map<UUID, BattleSession> SESSIONS = new HashMap<>();

    private static MinecraftServer server;

    private BattleManager() {}

    public static final class BattleException extends Exception {

        private final Component text;

        public BattleException(String translationKey) {
            super(translationKey);
            this.text = Component.translatable(translationKey);
        }

        public Component text() { return text; }
    }

    public static ImmutableMap<UUID, BattleSession> getSessions() {
        return ImmutableMap.copyOf(SESSIONS);
    }

    // ---------- EVENTS ----------

    public static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { server = null; SESSIONS.clear();});
        ServerTickEvents.END_SERVER_TICK.register(BattleManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((listener, s) -> onDisconnect(s, listener.getPlayer()));


        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            BattleSession session = fightSession(entity);

            return session == null || (server != null && server.getTickCount() >= session.fightStartsAtTick() && session.loser() == null);
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            BattleSession session = fightSession(entity);

            if (session == null) return true;

            ServerPlayer player = (ServerPlayer) entity;

            player.setHealth(player.getMaxHealth());

            if (session.loser() == null) session.setLoser(player.getUUID());

            return false;
        });
    }

    private static @Nullable BattleSession fightSession(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return null;

        BattleSession session = SESSIONS.get(player.getUUID());

        return session != null && session.state() == BattleSession.State.FIGHTING ? session : null;
    }

    public static Optional<BattleSession> sessionOf(UUID player) {
        return Optional.ofNullable(SESSIONS.get(player));
    }

    // ---------- CHALLENGE ----------

    public static BattleSession challenge(MinecraftServer server, ServerPlayer challenger, ServerPlayer target)
        throws BattleException {
        if (challenger.getUUID().equals(target.getUUID())) throw new BattleException("battle.error.self");
        if (SESSIONS.containsKey(challenger.getUUID())) throw new BattleException("battle.error.you_busy");
        if (SESSIONS.containsKey(target.getUUID())) throw new BattleException("battle.error.target_busy");

        BattleSession session = new BattleSession(
                challenger.getUUID(), target.getUUID(), server.getTickCount() + PENDING_TIMEOUT_TICKS
        );

        SESSIONS.put(challenger.getUUID(), session);
        SESSIONS.put(target.getUUID(), session);

        tell(server, target.getUUID(), Component.translatable("battle.challenged", challenger.getName()));
        send(server, challenger.getUUID(), new BattlePayloads.OpenWaiting(target.getName().getString()));
        send(server, target.getUUID(), new BattlePayloads.OpenChallenge(challenger.getName().getString()));
        return session;
    }

    public static BattleSession accept(MinecraftServer server, ServerPlayer target) throws BattleException {
        BattleSession session = requireTarget(target, BattleSession.State.PENDING);
        ServerPlayer challenger = server.getPlayerList().getPlayer(session.challenger());
        if (challenger == null) {
            end(server, session);
            throw new BattleException("battle.error.opponent_offline");
        }

        session.setState(BattleSession.State.STAKING);
        tell(server, session.challenger(), Component.translatable("battle.accepted", target.getName()));
        openStakeMenu(server, session, challenger);
        openStakeMenu(server, session, target);
        broadcastState(server, session);
        return session;
    }

    public static void refuse(MinecraftServer server, ServerPlayer target) throws BattleException {
        BattleSession session = requireTarget(target, BattleSession.State.PENDING);

        tell(server, session.challenger(), Component.translatable("battle.refused", target.getName()));

        end(server, session);
    }

    public static void cancel(MinecraftServer server, ServerPlayer player) throws BattleException {
        BattleSession session = SESSIONS.get(player.getUUID());

        if (session == null) throw new BattleException("battle.error.no_session");
        if (session.state() != BattleSession.State.PENDING && session.state() != BattleSession.State.STAKING)
            throw new BattleException("battle.error.wrong_state");

        tell(server, session.opponentOf(player.getUUID()), Component.translatable("battle.cancelled", player.getName()));

        end(server, session);
    }

    public static void onMenuClosed(MinecraftServer server, ServerPlayer player) {
        BattleSession s = SESSIONS.get(player.getUUID());
        if (s == null || s.state() != BattleSession.State.STAKING) return;
        tell(server, s.opponentOf(player.getUUID()), Component.translatable("battle.cancelled", player.getName()));
        end(server, s);
    }

    // ---------- TICK / DISCONNECTION ----------

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();

        for (BattleSession session : new HashSet<>(SESSIONS.values())) {
            switch (session.state()) {
                case PENDING -> {
                    if (now >= session.expiresAtTick()) {
                        tellBoth(server, session, Component.translatable("battle.expired"));
                        end(server, session);
                    }
                }
                case FIGHTING -> {
                    if (!session.started() && now >= session.fightStartsAtTick()) {
                        session.setStarted(true);
                        tellBoth(server, session, Component.translatable("battle.go"));
                    }
                    if (session.loser() != null) finishFight(server, session, session.loser());
                    else if (now >= session.fightEndsAtTick()) finishFight(server, session, null);
                }
                default -> {}
            }
        }
    }

    public static void onDisconnect(MinecraftServer server, ServerPlayer player) {
        BattleSession battleSession = SESSIONS.get(player.getUUID());

        if (battleSession == null) return;

        if (battleSession.state() == BattleSession.State.PENDING || battleSession.state() == BattleSession.State.STAKING) {
            tell(server, battleSession.opponentOf(player.getUUID()), Component.translatable("battle.opponent_left", player.getName()));
            end(server, battleSession);
        } else if (battleSession.state() == BattleSession.State.FIGHTING) {
            tell(server, battleSession.opponentOf(player.getUUID()), Component.translatable("battle.opponent_left", player.getName()));
            finishFight(server, battleSession, player.getUUID());
        }
    }

    public static void clear() { SESSIONS.clear(); }

    // ---------- BETTING ----------

    public static void broadcastState(MinecraftServer server, BattleSession s) {
        sendState(server, s, s.challenger());
        sendState(server, s, s.target());
    }

    private static void sendState(MinecraftServer server, BattleSession s, UUID viewerId) {
        ServerPlayer viewer = server.getPlayerList().getPlayer(viewerId);
        ServerPlayer opp = server.getPlayerList().getPlayer(s.opponentOf(viewerId));
        if (viewer == null || opp == null) return;

        BattleSession.Stake mine = s.stakeOf(viewerId);
        BattleSession.Stake theirs = s.stakeOf(opp.getUUID());
        int max = StakeUtil.playerAura(viewer);
        mine.aura = Math.min(mine.aura, max);

        ServerPlayNetworking.send(viewer, new BattlePayloads.BattleState(
                mine.aura, max, mine.validated,
                StakeUtil.count(viewer, s.id()), StakeUtil.itemsAura(viewer, s.id()),
                opp.getName().getString(), theirs.aura, theirs.validated,
                StakeUtil.count(opp, s.id()), StakeUtil.itemsAura(opp, s.id())));
    }

    public static void onBothValidated(MinecraftServer server, BattleSession session) {
        ServerPlayer playerA = server.getPlayerList().getPlayer(session.challenger());
        ServerPlayer playerB = server.getPlayerList().getPlayer(session.target());
        if (playerA == null || playerB == null) { end(server, session); return; }

        session.stakeOf(playerA.getUUID()).lockedItems = StakeUtil.stakedItems(playerA, session.id()).stream().map(ItemStack::copy).toList();
        session.stakeOf(playerB.getUUID()).lockedItems = StakeUtil.stakedItems(playerB, session.id()).stream().map(ItemStack::copy).toList();

        long totalA = StakeUtil.itemsAura(playerA, session.id()) + session.stakeOf(playerA.getUUID()).aura;
        long totalB = StakeUtil.itemsAura(playerB, session.id()) + session.stakeOf(playerB.getUUID()).aura;
        playerA.sendSystemMessage(Component.translatable("battle.stake_locked", totalA, playerB.getName(), totalB));
        playerB.sendSystemMessage(Component.translatable("battle.stake_locked", totalB, playerA.getName(), totalA));

        startFight(server, session, playerA, playerB);
    }

    private static void openStakeMenu(MinecraftServer server, BattleSession s, ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new AuraBattleMenu(containerId, inventory, s, server),
                Component.translatable("gui.screens.aura_battle.title")));
    }

    // ---------- FIGHTING ----------

    private static void startFight(MinecraftServer server, BattleSession session, ServerPlayer playerA, ServerPlayer playerB) {
        BattleArena arena;

        try {
            arena = BattleArena.create(server);
        } catch (RuntimeException e) {
            LOGGER.error("[Battle} Unable to create arena", e);
            tellBoth(server, session, Component.translatable("battle.error.arena"));
            end(server, session);
            return;
        }

        session.setArena(arena);
        session.setState(BattleSession.State.FIGHTING);
        session.setOrigin(playerA.getUUID(), BattleSession.Origin.capture(playerA));
        session.setOrigin(playerB.getUUID(), BattleSession.Origin.capture(playerB));

        long now = server.getTickCount();
        session.setFightStartsAtTick(now + FIGHT_GRACE_TICKS);
        session.setFightEndsAtTick(now + FIGHT_TIME_LIMIT_TICKS);

        playerA.closeContainer();
        playerB.closeContainer();
        enterArena(playerA, arena.level(), BattleArena.SPAWN_A, BattleArena.YAW_A);
        enterArena(playerB, arena.level(), BattleArena.SPAWN_B, BattleArena.YAW_B);
    }

    private static void enterArena(ServerPlayer player, ServerLevel arena, Vec3 pos, float yaw) {
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.clearFire();
        player.teleportTo(arena, pos.x, pos.y, pos.z, Set.of(), yaw, 0.0F, true);
    }

    private record Loot(int items, int aura) {
        static final Loot NONE = new Loot(0, 0);
    }

    private static void finishFight(MinecraftServer server, BattleSession session, @Nullable UUID loserId) {
        if (session.state() != BattleSession.State.FIGHTING) return;

        session.setState(BattleSession.State.RESOLVING);

        for (UUID id : List.of(session.challenger(), session.target())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);

            if (player == null) continue;

            leaveArena(server, player, session, session.origin(id));
        }

        Loot loot = loserId == null ? Loot.NONE : settle(server, session, session.opponentOf(loserId), loserId);

        for (UUID id : List.of(session.challenger(), session.target())) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) continue;
            if (loserId == null) p.sendSystemMessage(Component.translatable("battle.result.draw"));
            else p.sendSystemMessage(Component.translatable(
                    id.equals(loserId) ? "battle.result.lost" : "battle.result.won", loot.items(), loot.aura()));
        }

        BattleArena arena = session.arena();

        if (arena != null) arena.close();
        end(server, session);
    }

    private static Loot settle(MinecraftServer server, BattleSession session, UUID winnerID, UUID loserID) {
        ServerPlayer winner = server.getPlayerList().getPlayer(winnerID);
        ServerPlayer loser = server.getPlayerList().getPlayer(loserID);

        if (winner == null || loser == null) return Loot.NONE;

        int moved = 0;
        Inventory inventory = loser.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!StakeUtil.isStaked(stack, session.id())) continue;

            inventory.setItem(i, ItemStack.EMPTY);
            stack.remove(ModDataComponentTypes.STAKE_ID);
            winner.getInventory().placeItemBackInInventory(stack, Prediction.PREDICTED);
            moved++;
        }

        CustomPlayer customWinner = CustomPlayer.getCustomPlayerOrNew(winner);
        CustomPlayer customLoser = CustomPlayer.getCustomPlayerOrNew(loser);

        int aura = Math.min(session.stakeOf(loserID).aura, StakeUtil.playerAura(loser));

        if (aura > 0) {
            customLoser.removePlayerAura(aura);
            customWinner.addPlayerAura(aura);
        }

        LOGGER.info("[Battle] {} -< {} : {} / {} items, {} aura",
                loser.getName().getString(), winner.getName().getString(),
                moved, session.stakeOf(loserID).lockedItems.size(), aura);

        return new Loot(moved, aura);
    }

    private static void leaveArena(MinecraftServer server, ServerPlayer player, BattleSession session, BattleSession.Origin origin) {
        ServerLevel back = server.getLevel(origin.dimension());

        if (back == null) back = server.overworld();

        player.setGameMode(origin.gameType());
        player.setHealth(origin.health());
        player.getFoodData().setFoodLevel(origin.food());
        player.getFoodData().setSaturation(origin.saturation());
        player.clearFire();
        player.teleportTo(back, origin.x(), origin.y(), origin.z(), Set.of(), origin.yaw(), origin.pitch(), true);
    }

    // ---------- INTERNAL ----------

    private static void end(MinecraftServer server, BattleSession session) {
        session.setState(BattleSession.State.ENDED);
        SESSIONS.remove(session.challenger());
        SESSIONS.remove(session.target());
        for (UUID id : List.of(session.challenger(), session.target())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            StakeUtil.clearMarks(player, session.id());
            if (player.containerMenu instanceof AuraBattleMenu) player.closeContainer();
            ServerPlayNetworking.send(player, BattlePayloads.CloseScreen.INSTANCE);
        }
    }

    private static BattleSession requireTarget(ServerPlayer player, BattleSession.State expected) throws BattleException {
        BattleSession session = SESSIONS.get(player.getUUID());

        if (session == null) throw new BattleException("battle.error.no_session");
        if (session.state() != expected) throw new BattleException("battle.error.wrong_state");
        if (!session.target().equals(player.getUUID())) throw new BattleException("battle.error.not_target");

        return session;
    }

    private static void tellBoth(MinecraftServer server, BattleSession session, Component message) {
        tell(server, session.challenger(), message);
        tell(server, session.target(), message);
    }

    private static void tell(MinecraftServer server, UUID id, Component message) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);

        if (player != null) player.sendSystemMessage(message);
    }

    private static void send(MinecraftServer server, UUID id, CustomPacketPayload packetPayload) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);

        if (player != null) ServerPlayNetworking.send(player, packetPayload);
    }
}
