package fr.miguel.pve.attack;

/** Attaque purement statistique. Pour un effet spécial, écris ta propre classe qui implémente AttackInterface. */
public record BasicAttack(String id, String nameKey, double baseDamage, double power, double accuracy)
        implements AttackInterface {}
