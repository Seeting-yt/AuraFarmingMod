package fr.miguel.gambling_events.bonus;

import fr.miguel.gambling_events.GamblingEvent;
import fr.miguel.player.CustomPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class MultiplyEvent extends GamblingEvent {

    public MultiplyEvent() {
        super(TYPE.BONUS);
    }

    @Override
    protected void eventAction(@NotNull CustomPlayer customPlayer, double auraBet) {
        double ratio = (auraBet / customPlayer.getPlayerAura());

        Component message = Component.translatable("gambling.event.multiply.message",1 + ratio);

        if (ratio <= 0.9) {
            customPlayer.multiplyPlayerAura(1 + ratio);
            customPlayer.getAssociatedPlayer().sendSystemMessage(message);
        } else {
            customPlayer.multiplyPlayerAura(2.167);
            customPlayer.getAssociatedPlayer()
                    .sendSystemMessage(Component
                            .translatable("gambling.event.multiply.message", 2.167));
        }
    }
}
