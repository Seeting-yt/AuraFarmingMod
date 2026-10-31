package fr.miguel.api.events;

import fr.miguel.player.CustomPlayer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;

public interface AuraChangedCallBack {
    Event<AuraChangedCallBack> EVENT = EventFactory.createArrayBacked(AuraChangedCallBack.class,
            (listeners) -> (player, oldAmount, newAmount) -> {

                for (AuraChangedCallBack listener : listeners) {
                    InteractionResult result = listener.interact(player, oldAmount, newAmount);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }
                return InteractionResult.PASS;
            });

    InteractionResult interact(CustomPlayer player, double oldAmount, double newAmount);
}
