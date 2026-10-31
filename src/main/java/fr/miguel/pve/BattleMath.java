package fr.miguel.pve;

import java.util.random.RandomGenerator;

/** Toutes les formules d'équilibrage au même endroit. Aucune dépendance à Minecraft. */
public final class BattleMath {

    private BattleMath() {}

    // ---------- paramètres d'équilibrage ----------
    /**
     * PV et dégâts ~ aura^SCALE_EXPONENT (1.0 = linéaire). Nb de tours ~ f(aura défenseur) / f(aura attaquant) :
     * un exposant bas aplatit les écarts, donc un joueur bien équipé peut encore battre un mob plus fort.
     */
    public static final double SCALE_EXPONENT = 0.3;
    public static final double HP_PER_SCALE = 56;
    public static final double DAMAGE_PER_SCALE = 14;
    public static final double DAMAGE_VARIANCE = 0.35;     // +-35 %
    /** Pente de la précision selon l'écart d'aura : 0 = toujours 50 %, 0.45 = de 5 % à 95 %. */
    public static final double HIT_SLOPE = 0.2;
    /** Lisse l'écart relatif : évite la division par 0 et adoucit les très petites auras. */
    public static final double SMOOTHING = 10;
    public static final double MIN_HIT = 0.05;
    public static final double MAX_HIT = 0.95;
    /**
     * Aura gagnée par PV de l'ennemi (avant multiplicateur). 1.0 = "PV totaux" littéral : à 56 PV/aura,
     * un joueur de 100 aura battant un mob de 100 aura gagnerait ~5 600 aura. D'où 1/56 par défaut.
     */
    public static final double REWARD_PER_HP = 1.0 / HP_PER_SCALE;
    /**
     * Le multiplicateur vaut 2^(SPREAD * écart relatif). Il doit être grand : la probabilité de victoire s'effondre
     * vite contre un mob plus fort (27 % à x1.5, 13 % à x2, 5 % à x3), le gain doit compenser. Vérifie avec BalanceSim.
     */
    public static final double WIN_SPREAD = 12.0;
    public static final double LOSS_SPREAD = 4.0;
    /** Plafond du multiplicateur de gain : évite les jackpots absurdes contre un mob d'aura démesurée. */
    public static final double MAX_WIN_MULTIPLIER = 64.0;

    // ---------- statistiques ----------

    /** Aura utilisable pour PV et dégâts : jamais < 1, donc jamais de PV <= 0 même avec une aura négative. */
    public static double scale(double aura) {
        return Math.pow(Math.max(aura, 1.0), SCALE_EXPONENT);
    }

    public static int maxHp(double aura) {
        return (int) Math.max(1, Math.round(HP_PER_SCALE * scale(aura)));
    }

    /** Écart relatif dans ]-1 ; 1[ : > 0 quand 'a' a plus d'aura que 'b'. Supporte l'aura négative. */
    public static double relativeGap(double a, double b) {
        return (a - b) / (Math.abs(a) + Math.abs(b) + SMOOTHING);
    }

    // ---------- combat ----------

    /** 50 % à aura égale, 5 % à 95 % aux extrêmes, puis multiplié par la précision de l'attaque. */
    public static double hitChance(double attackerAura, double defenderAura, double accuracy) {
        double p = (0.5 + HIT_SLOPE * relativeGap(attackerAura, defenderAura)) * accuracy;
        return Math.max(MIN_HIT, Math.min(MAX_HIT, p));
    }

    public static int damage(double attackerAura, double baseDamage, double power, RandomGenerator rnd) {
        double raw = baseDamage + DAMAGE_PER_SCALE * power * scale(attackerAura);
        double variance = 1 + DAMAGE_VARIANCE * (rnd.nextDouble() * 2 - 1);
        return (int) Math.max(1, Math.round(raw * variance));
    }

    // ---------- économie ----------

    /** 1 à aura égale, > 1 contre plus fort (de plus en plus), < 1 contre plus faible : décourage le farm de petits mobs. */
    public static double winMultiplier(double playerAura, double enemyAura) {
        return Math.min(MAX_WIN_MULTIPLIER, Math.pow(2, WIN_SPREAD * relativeGap(enemyAura, playerAura)));
    }

    /** Miroir : perdre contre un plus faible coûte plus cher, perdre contre un plus fort coûte moins. */
    public static double lossMultiplier(double playerAura, double enemyAura) {
        return Math.pow(2, LOSS_SPREAD * relativeGap(playerAura, enemyAura));
    }

    public static long winReward(double playerAura, Combatant enemy) {
        return Math.round(enemy.maxHp() * REWARD_PER_HP * winMultiplier(playerAura, enemy.aura()));
    }

    public static long lossPenalty(double playerAura, Combatant enemy) {
        return Math.round(enemy.maxHp() * REWARD_PER_HP * lossMultiplier(playerAura, enemy.aura()));
    }

    /** Aura après une fuite : aura >= 0 -> 0, sinon aura * 2. */
    public static double fleeAura(double aura) {
        return aura >= 0 ? 0 : aura * 2;
    }
}
