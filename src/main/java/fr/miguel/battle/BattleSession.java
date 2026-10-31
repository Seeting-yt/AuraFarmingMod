package fr.miguel.battle;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BattleSession {

    public enum State { PENDING, STAKING, FIGHTING, RESOLVING, ENDED }

    public static final class Stake {
        public int aura;
        public boolean validated;

        public List<ItemStack> lockedItems = List.of();
    }

    public record Origin(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch,
                         GameType gameType, float health, int food, float saturation) {
        public static Origin capture(ServerPlayer player) {
            return new Origin(player.level().dimension(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(),
                    player.gameMode.getGameModeForPlayer(), player.getHealth(),
                    player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel());
        }
    }

    private final UUID id = UUID.randomUUID();
    private final UUID challenger;
    private final UUID target;
    private final long expiresAtTick;
    private final Map<UUID, Stake> stakes = new HashMap<>();
    private final Map<UUID, Origin> origins = new HashMap<>();
    private State state = State.PENDING;

    private @Nullable BattleArena arena;
    private long fightStartsAtTick;
    private long fightEndsAtTick;
    private boolean started;
    private @Nullable UUID loser;

    public BattleSession(UUID challenger, UUID target, long expiresAtTick) {
        this.challenger = challenger;
        this.target = target;
        this.expiresAtTick = expiresAtTick;
        stakes.put(challenger, new Stake());
        stakes.put(target, new Stake());
    }

    public UUID id() { return id; }
    public UUID challenger() { return challenger; }
    public UUID target() { return target; }
    public long expiresAtTick() { return expiresAtTick; }
    public State state() { return state; }
    public void setState(State state) { this.state = state; }
    public Stake stakeOf(UUID player) { return stakes.get(player); }
    public boolean bothValidated() { return stakes.values().stream().allMatch(s -> s.validated); }

    public UUID opponentOf(UUID player) {
        return player.equals(challenger) ? target : challenger;
    }

    public Origin origin(UUID player) { return origins.get(player); }
    public void setOrigin(UUID player, Origin origin) { origins.put(player, origin); }

    public @Nullable BattleArena arena() { return arena; }
    public void setArena(@Nullable BattleArena arena) { this.arena = arena; }
    public long fightStartsAtTick() { return fightStartsAtTick; }
    public void setFightStartsAtTick(long tick) { this.fightStartsAtTick = tick; }
    public long fightEndsAtTick() { return fightEndsAtTick; }
    public void setFightEndsAtTick(long tick) { this.fightEndsAtTick = tick; }
    public boolean started() { return started; }
    public void setStarted(boolean started) { this.started = started; }
    public @Nullable UUID loser() { return loser; }
    public void setLoser(@Nullable UUID loser) { this.loser = loser; }
}
