package fr.miguel.api.events.cancelable;

public interface Cancelable {

    boolean isCanceled();
    void setCanceled(boolean canceled);

}
