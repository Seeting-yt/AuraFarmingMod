package fr.miguel.pve.attack;

import fr.miguel.pve.BattleContext;
import fr.miguel.pve.BattleEvent;
import fr.miguel.pve.BattleMath;
import fr.miguel.pve.Combatant;

import java.util.ArrayList;
import java.util.List;

public interface AttackInterface {

    /** Identifiant stable (celui qui voyage sur le réseau), ex. "miguelaurafarming:strike". */
    String id();

    /** Clé de traduction du nom affiché. */
    String nameKey();

    /** Dégâts fixes, indépendants de l'aura. */
    double baseDamage();

    /** Multiplicateur de la part de dégâts proportionnelle à l'aura de l'attaquant. */
    double power();

    /** Multiplicateur de la chance de toucher (1.0 = normale). */
    double accuracy();

    /** Résolution standard : annonce, jet de précision, dégâts. Surcharge-la pour des effets spéciaux. */
    default List<BattleEvent> use(BattleContext ctx, Combatant user, Combatant target) {
        List<BattleEvent> events = new ArrayList<>();
        events.add(new BattleEvent.Used(user.side(), nameKey()));

        double chance = BattleMath.hitChance(user.aura(), target.aura(), accuracy());
        if (ctx.random().nextDouble() >= chance) {
            events.add(new BattleEvent.Miss(target.side()));
            return events;
        }

        int damage = BattleMath.damage(user.aura(), baseDamage(), power(), ctx.random());
        target.damage(damage);
        events.add(new BattleEvent.Hit(target.side(), damage, target.hp()));
        if (target.isDown()) events.add(new BattleEvent.Down(target.side()));
        return events;
    }
}
