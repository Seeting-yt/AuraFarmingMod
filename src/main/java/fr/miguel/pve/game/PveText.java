package fr.miguel.pve.game;


import fr.miguel.pve.BattleEvent;
import fr.miguel.pve.Combatant;
import fr.miguel.pve.Side;
import net.minecraft.network.chat.Component;

final class PveText {

    private PveText() {}

    private static Combatant of(Side side, PveBattle battle) {
        return side == Side.PLAYER ? battle.player() : battle.enemy();
    }

    static Component describe(BattleEvent event, PveBattle b) {
        return switch (event) {
            case BattleEvent.Used u -> Component.translatable("pve.used",
                    of(u.actor(), b).name(), Component.translatable(u.attackNameKey()));
            case BattleEvent.Miss m -> Component.translatable("pve.miss", of(m.target(), b).name());
            case BattleEvent.Hit h -> Component.translatable("pve.hit",
                    of(h.target(), b).name(), h.damage(), h.hpAfter(), of(h.target(), b).maxHp());
            case BattleEvent.Down d -> Component.translatable("pve.down", of(d.who(), b).name());
            case BattleEvent.Fled f -> Component.translatable("pve.fled", of(f.who(), b).name());
        };
    }

    static Component status(PveBattle battle) {
        return Component.translatable("pve.status",
                battle.player().hp(), battle.player().maxHp(), battle.enemy().name(), battle.enemy().hp(), battle.enemy().maxHp());
    }

    static Component result(PveBattle.Outcome outcome, long delta) {
        String key = switch (outcome) {
            case WON -> "pve.result.won";
            case LOST -> "pve.result.lost";
            case FLED -> "pve.result.fled";
            case ONGOING -> "pve.result.cancelled";
        };
        return Component.translatable(key, String.format("%+d", delta));
    }
}
