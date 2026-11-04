package fr.miguel.api.events.cancelable;

public class CancelableEvent implements Cancelable {

    private boolean canceled;

    public CancelableEvent() {}

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public boolean isCanceled() {
        return this.canceled;
    }

}
