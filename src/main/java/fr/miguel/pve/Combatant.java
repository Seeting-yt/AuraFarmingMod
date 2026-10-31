package fr.miguel.pve;

/** Un participant au combat. L'aura est figée à la création : PV et dégâts ne bougent plus ensuite. */
public final class Combatant {

    private final Side side;
    private final String name;
    private final double aura;
    private final int maxHp;
    private int hp;

    public Combatant(Side side, String name, double aura) {
        this.side = side;
        this.name = name;
        this.aura = aura;
        this.maxHp = BattleMath.maxHp(aura);
        this.hp = maxHp;
    }

    public Side side() { return side; }
    public String name() { return name; }
    public double aura() { return aura; }
    public int maxHp() { return maxHp; }
    public int hp() { return hp; }
    public boolean isDown() { return hp <= 0; }

    public void damage(int amount) {
        hp = Math.max(0, hp - amount);
    }
}
