package fr.miguel.pve.game;

import net.minecraft.world.entity.Mob;

import java.util.UUID;

final class PveSession {

    final UUID playerId;
    final Mob mob;
    final PveBattle battle;
    long deadlineTick;
    boolean closing;

    final boolean mobWasNoAI;
    final boolean mobWasInvulnerable;
    final boolean playerWasInvulnerable;

    PveSession(UUID playerId, Mob mob, PveBattle battle, long deadlineTick, boolean playerWasInvulnerable) {
        this.playerId = playerId;
        this.mob = mob;
        this.battle = battle;
        this.deadlineTick = deadlineTick;
        this.mobWasNoAI = mob.isNoAi();
        this.mobWasInvulnerable = mob.isInvulnerable();
        this.playerWasInvulnerable = playerWasInvulnerable;
    }
}
