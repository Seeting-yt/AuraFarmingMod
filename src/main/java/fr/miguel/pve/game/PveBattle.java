package fr.miguel.pve.game;

import fr.miguel.pve.*;
import fr.miguel.pve.attack.AttackInterface;

import java.util.ArrayList;
import java.util.List;

/** Un combat joueur contre une entité, au tour par tour. Logique pure : aucune dépendance à Minecraft. */
public final class PveBattle {

    public enum Outcome { ONGOING, WON, LOST, FLED }

    private final Combatant player;
    private final Combatant enemy;
    private final List<AttackInterface> enemyAttacks;
    private final BattleContext ctx;
    private Outcome outcome = Outcome.ONGOING;

    public PveBattle(Combatant player, Combatant enemy, List<AttackInterface> enemyAttacks, BattleContext ctx) {
        if (enemyAttacks.isEmpty()) throw new IllegalArgumentException("L'ennemi n'a aucune attaque");
        this.player = player;
        this.enemy = enemy;
        this.enemyAttacks = List.copyOf(enemyAttacks);
        this.ctx = ctx;
    }

    public Combatant player() { return player; }
    public Combatant enemy() { return enemy; }
    public Outcome outcome() { return outcome; }

    /** Tour complet : attaque du joueur, puis riposte de l'ennemi si il est encore debout. */
    public List<BattleEvent> playerAttack(AttackInterface attack) {
        requireOngoing();
        List<BattleEvent> events = new ArrayList<>(attack.use(ctx, player, enemy));
        if (enemy.isDown()) {
            outcome = Outcome.WON;
            return events;
        }

        AttackInterface reply = enemyAttacks.get(ctx.random().nextInt(enemyAttacks.size()));
        events.addAll(reply.use(ctx, enemy, player));
        if (player.isDown()) outcome = Outcome.LOST;
        return events;
    }

    public List<BattleEvent> flee() {
        requireOngoing();
        outcome = Outcome.FLED;
        return List.of(new BattleEvent.Fled(Side.PLAYER));
    }

    /** Variation d'aura du joueur selon l'issue (0 tant que le combat n'est pas fini). */
    public long playerAuraDelta() {
        double start = player.aura();
        return switch (outcome) {
            case ONGOING -> 0;
            case WON -> BattleMath.winReward(start, enemy);
            case LOST -> -BattleMath.lossPenalty(start, enemy);
            case FLED -> Math.round(BattleMath.fleeAura(start) - start);
        };
    }

    private void requireOngoing() {
        if (outcome != Outcome.ONGOING) throw new IllegalStateException("Combat terminé : " + outcome);
    }
}
