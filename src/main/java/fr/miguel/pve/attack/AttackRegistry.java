package fr.miguel.pve.attack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AttackRegistry {

    private static final Map<String, AttackInterface> ATTACKS = new LinkedHashMap<>();

    private AttackRegistry() {}

    public static final AttackInterface STRIKE =
            register(new BasicAttack("miguelaurafarming:strike", "attack.miguelaurafarming.strike", 5, 1.0, 1.0));
    public static final AttackInterface HEAVY_BLOW =
            register(new BasicAttack("miguelaurafarming:heavy_blow", "attack.miguelaurafarming.heavy_blow", 0, 1.8, 0.7));
    public static final AttackInterface QUICK_JAB =
            register(new BasicAttack("miguelaurafarming:quick_jab", "attack.miguelaurafarming.quick_jab", 3, 0.6, 1.2));

    public static AttackInterface register(AttackInterface attack) {
        if (ATTACKS.putIfAbsent(attack.id(), attack) != null)
            throw new IllegalStateException("Attaque déjà enregistrée : " + attack.id());
        return attack;
    }

    public static Optional<AttackInterface> get(String id) {
        return Optional.ofNullable(ATTACKS.get(id));
    }

    public static Iterable<AttackInterface> all() {
        return ATTACKS.values();
    }
}
