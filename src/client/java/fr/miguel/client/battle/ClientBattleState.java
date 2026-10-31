package fr.miguel.client.battle;

import fr.miguel.network.BattlePayloads;

public final class ClientBattleState {

    private static volatile BattlePayloads.BattleState current;

    public static BattlePayloads.BattleState get() { return current; }
    public static void set(BattlePayloads.BattleState state) { current = state; }
    public static void clear() { current = null; }

    private ClientBattleState() {}
}
