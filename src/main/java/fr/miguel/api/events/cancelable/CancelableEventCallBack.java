package fr.miguel.api.events.cancelable;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.function.Function;

public interface CancelableEventCallBack<T extends CancelableEvent> {

    void interact(T eventInstance);

    @SuppressWarnings("unchecked")
    static <E extends CancelableEvent, C extends CancelableEventCallBack<E>> Event<C> create(
            Class<C> callbackClass,
            Function<CancelableEventCallBack<E>, C> interfaceFactory) {

        return EventFactory.createArrayBacked(callbackClass,
                (listeners) -> interfaceFactory.apply((eventInstance) -> {
                    for (C listener : listeners) {
                        listener.interact(eventInstance);
                    }
                }));
    }
}
