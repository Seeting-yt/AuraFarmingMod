package fr.miguel.pve;

/**
 * Ce que le serveur raconte du tour. Le client rejoue ces événements dans l'ordre :
 * aujourd'hui un swing + un texte, plus tard une animation custom, sans toucher à la logique.
 */
public sealed interface BattleEvent {
    record Used(Side actor, String attackNameKey) implements BattleEvent {}
    record Miss(Side target) implements BattleEvent {}
    record Hit(Side target, int damage, int hpAfter) implements BattleEvent {}
    record Down(Side who) implements BattleEvent {}
    record Fled(Side who) implements BattleEvent {}
}
