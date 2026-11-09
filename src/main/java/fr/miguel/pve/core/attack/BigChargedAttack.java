package fr.miguel.pve.core.attack;

import fr.miguel.pve.core.BattleContext;
import fr.miguel.pve.core.BattleEvent;
import fr.miguel.pve.core.BattleMath;
import fr.miguel.pve.core.Combatant;

import java.util.ArrayList;
import java.util.List;

public record BigChargedAttack(String id, String nameKey, double baseDamage,
                               double power, double accuracy)
        implements AttackInterface {

    @Override
    public List<BattleEvent> use(BattleContext ctx, Combatant user, Combatant target) {
        ArrayList<BattleEvent> events = new ArrayList<>();

        double chance = BattleMath.hitChance(user.aura(), target.aura(), accuracy());
        if (ctx.random().nextDouble() >= chance) {
            events.add(new BattleEvent.Miss(target.side()));
            return events;
        }

        events.add(new BattleEvent.Used(user.side(), nameKey()));
        double ratio = user.aura() / target.aura();

        int damage;
        if (ratio < 1) {
            damage = BattleMath.damage(user.aura(), baseDamage(), power(), ctx.random());

        } else {
            damage = (int) ((1 + ratio) *
                    BattleMath.damage(user.aura(), baseDamage(), power(), ctx.random()));
        }
        target.damage(damage);
        events.add(new BattleEvent.Hit(target.side(), damage, target.hp()));

        if (target.isDown()) events.add(new BattleEvent.Down(target.side()));
        return events;
    }
}
