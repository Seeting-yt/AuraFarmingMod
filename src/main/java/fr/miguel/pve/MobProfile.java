package fr.miguel.pve;

import java.util.List;

/** Aura fixe d'un type de mob et attaques qu'il peut utiliser (ids du AttackRegistry). */
public record MobProfile(double aura, List<String> attackIds) {}
