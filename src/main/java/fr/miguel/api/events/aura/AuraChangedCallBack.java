package fr.miguel.api.events.aura;

import fr.miguel.api.events.cancelable.CancelableEventCallBack;
import fr.miguel.player.CustomPlayer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;

public interface AuraChangedCallBack extends CancelableEventCallBack<AuraChangedEvent> {
    Event<AuraChangedCallBack> EVENT = CancelableEventCallBack
            .create(AuraChangedCallBack.class, (baseCallBack) -> baseCallBack::interact);
}
