package fr.miguel.pve;

import fr.miguel.pve.attack.AttackRegistry;
import fr.miguel.pve.game.PveBattle;

import java.util.List;
import java.util.Random;

/**
 * Simulateur d'équilibrage : "java -cp out fr.miguel.pve.BalanceSim [aura du joueur]".
 * Change les constantes de BattleMath, recompile, relance. Le joueur spamme Strike (cas de base, sans objets).
 * Objectif : EV ~ 0 contre les mobs plus faibles ou égaux, et croissante contre les mobs plus forts.
 */
public final class BalanceSim {

    public static void main(String[] args) {
        double playerAura = args.length > 0 ? Double.parseDouble(args[0]) : 100;
        double[] ratios = {0.1, 0.2, 0.5, 1, 1.5, 2, 3, 5};
        Random rnd = new Random(42);
        int runs = 20000;

        System.out.printf("Joueur : %.0f aura (%d PV)%n", playerAura, BattleMath.maxHp(playerAura));
        System.out.printf("%-7s %-8s %-9s %-7s %-10s %-10s %-10s%n",
                "mob/jou", "mob aura", "victoire", "tours", "gain moy.", "perte moy.", "EV/combat");

        for (double ratio : ratios) {
            double mobAura = playerAura * ratio;
            int wins = 0, losses = 0;
            long turns = 0, gain = 0, loss = 0;
            for (int i = 0; i < runs; i++) {
                PveBattle b = new PveBattle(
                        new Combatant(Side.PLAYER, "joueur", playerAura),
                        new Combatant(Side.ENEMY, "mob", mobAura),
                        List.of(AttackRegistry.STRIKE, AttackRegistry.HEAVY_BLOW), new BattleContext(rnd));
                int t = 0;
                while (b.outcome() == PveBattle.Outcome.ONGOING && t < 3000) {
                    b.playerAttack(AttackRegistry.STRIKE);
                    t++;
                }
                turns += t;
                if (b.outcome() == PveBattle.Outcome.WON) { wins++; gain += b.playerAuraDelta(); }
                else { losses++; loss += -b.playerAuraDelta(); }
            }
            System.out.printf("%-7.1f %-8.0f %-8.0f%% %-7.1f %-10.1f %-10.1f %-10.2f%n",
                    ratio, mobAura, 100.0 * wins / runs, (double) turns / runs,
                    wins == 0 ? 0 : (double) gain / wins, losses == 0 ? 0 : (double) loss / losses,
                    ((double) gain - loss) / runs);
        }

        System.out.printf("%nFuite : 150 -> %.0f | 0 -> %.0f | -40 -> %.0f%n",
                BattleMath.fleeAura(150), BattleMath.fleeAura(0), BattleMath.fleeAura(-40));
    }
}
