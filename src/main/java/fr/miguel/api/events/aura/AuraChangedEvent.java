package fr.miguel.api.events.aura;

import fr.miguel.api.events.cancelable.CancelableEvent;
import fr.miguel.player.CustomPlayer;

public class AuraChangedEvent extends CancelableEvent {

    private final CustomPlayer customPlayer;
    private final double oldAmount;

    private double newAmount;

    public AuraChangedEvent(CustomPlayer customPlayer, double oldAmount, double newAmount) {
        this.customPlayer = customPlayer;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
    }


    public CustomPlayer getCustomPlayer() {
        return customPlayer;
    }

    public double getOldAmount() {
        return oldAmount;
    }

    public double getNewAmount() {
        return newAmount;
    }

    public void setNewAmount(double newAmount) {
        this.newAmount = newAmount;
    }
}
