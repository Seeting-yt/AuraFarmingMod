package fr.miguel.api.events.random;

import fr.miguel.api.events.aura.AuraChangedCallBack;
import fr.miguel.api.events.cancelable.CancelableEventCallBack;
import net.fabricmc.fabric.api.event.Event;

public interface RandomEventCallBack extends CancelableEventCallBack<RandomAuraGambleEvent> {

    Event<RandomEventCallBack> EVENT = CancelableEventCallBack.create(RandomEventCallBack.class,
            (baseCallBack) -> baseCallBack::interact);

    static interface BeforeScreen extends RandomEventCallBack {
        Event<BeforeScreen> EVENT = CancelableEventCallBack.create(BeforeScreen.class,
                (baseCallBack) -> baseCallBack::interact);
    }

    static interface AfterGambling extends RandomEventCallBack {
        Event<AfterGambling> EVENT = CancelableEventCallBack.create(AfterGambling.class,
                (baseCallBack) -> baseCallBack::interact);
    }
}
