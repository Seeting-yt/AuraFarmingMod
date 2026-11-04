package fr.miguel.gambling_events;

import fr.miguel.player.CustomPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class GamblingEvent {

    public enum TYPE {
        BONUS("bonus"), MALUS("malus");

        private final String name;

        TYPE(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    @NotNull
    private final TYPE eventType;

    protected GamblingEvent(@NotNull TYPE eventType) {
        this.eventType = Objects.requireNonNull(eventType);
    }

    @NotNull
    public TYPE getEventType() {
        return eventType;
    }


    /**
     * Performs this event action. First it checks the side
     * and if it's server side, call the implemented action
     * @param customPlayer the custom player instance related
     * @param auraBet the aura bet by the player
     */
    public final void action(@NotNull CustomPlayer customPlayer, double auraBet) {
        Player player = customPlayer.getAssociatedPlayer();

        if (player.level().isClientSide()) return;

        player.sendSystemMessage(Component
                .translatable("gambling.event.message", getEventType().getName()));

        eventAction(customPlayer, auraBet);
    }

    /**
     * Your event action. This method is called server side ONLY.
     * This method should never be called outside of {@link GamblingEvent#action(CustomPlayer, double)}
     * @param customPlayer the custom player instance related
     * @param auraBet the aura bet by the player
     */
    protected abstract void eventAction(@NotNull CustomPlayer customPlayer, double auraBet);


}
