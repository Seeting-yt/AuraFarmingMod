package fr.miguel.player;

import fr.miguel.api.events.AuraChangedCallBack;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Range;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class CustomPlayer {

    private final UUID playerUniqueID;
    private final Player player;

    private static final HashMap<UUID, CustomPlayer> customPlayers = new HashMap<>();

    private double playerAura;

    private CustomPlayer(Player player) {
        this.playerUniqueID = Objects.requireNonNull(player.getUUID());
        this.player = player;
        this.playerAura = 10;

        customPlayers.put(this.playerUniqueID, this);
    }

    public void setPlayerAura(double newPlayerAura) {
        onAuraChanged(getPlayerAura(), newPlayerAura);
        this.playerAura = newPlayerAura;
    }

    public void addPlayerAura(double playerAura) {
        double newAmount = this.getPlayerAura() + playerAura;
        onAuraChanged(getPlayerAura(), newAmount);
        this.playerAura = newAmount;

    }

    public void removePlayerAura(double playerAura) {
        double newAmount = this.getPlayerAura() - playerAura;
        onAuraChanged(getPlayerAura(), newAmount);
        this.playerAura = newAmount;
    }

    public double getPlayerAura() {
        return this.playerAura;
    }

    public Player getAssociatedPlayer() {
        return this.player;
    }

    private void onAuraChanged(double oldAmount, double newAmount) {
        AuraChangedCallBack.EVENT.invoker().interact(this, oldAmount, newAmount);
    }

    public void multiplyPlayerAura(@Range(from = 0, to = Long.MAX_VALUE) double multiplier) {
        double newAmount = this.getPlayerAura()*multiplier;
        onAuraChanged(getPlayerAura(), newAmount);
        this.playerAura = newAmount;
    }

    public static CustomPlayer getCustomPlayerOrNew(Player player) {
        return customPlayers.containsKey(player.getUUID()) ? customPlayers.get(player.getUUID()) : new CustomPlayer(player);
    }
}
