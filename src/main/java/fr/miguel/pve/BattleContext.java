package fr.miguel.pve;

import java.util.random.RandomGenerator;

/** Ce dont une attaque a besoin pour se résoudre. Le RNG est injecté : combats rejouables dans les tests. */
public record BattleContext(RandomGenerator random) {}
