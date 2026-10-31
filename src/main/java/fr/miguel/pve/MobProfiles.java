package fr.miguel.pve;

import fr.miguel.pve.attack.AttackRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Table des mobs combattables. Les valeurs sont des exemples à équilibrer ; un mob absent n'est pas combattable. */
public final class MobProfiles {

    private static final Map<String, MobProfile> PROFILES = new HashMap<>();

    private MobProfiles() {}

    static {
        String strike = AttackRegistry.STRIKE.id();
        String heavy = AttackRegistry.HEAVY_BLOW.id();
        String jab = AttackRegistry.QUICK_JAB.id();

        PROFILES.put("minecraft:zombie",   new MobProfile(20,  List.of(strike)));
        PROFILES.put("minecraft:skeleton", new MobProfile(30,  List.of(strike, jab)));
        PROFILES.put("minecraft:creeper",  new MobProfile(45,  List.of(heavy)));
        PROFILES.put("minecraft:enderman", new MobProfile(120, List.of(strike, heavy, jab)));
    }

    /** @param entityTypeId ex. "minecraft:zombie" */
    public static Optional<MobProfile> get(String entityTypeId) {
        return Optional.ofNullable(PROFILES.get(entityTypeId));
    }
}
