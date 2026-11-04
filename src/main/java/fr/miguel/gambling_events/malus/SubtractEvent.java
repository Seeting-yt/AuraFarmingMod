package fr.miguel.gambling_events.malus;

import fr.miguel.api.AuraAPI;
import fr.miguel.gambling_events.GamblingEvent;
import fr.miguel.player.CustomPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class SubtractEvent extends GamblingEvent {

    public SubtractEvent() {
        super(TYPE.MALUS);
    }

    @Override
    protected void eventAction(@NotNull CustomPlayer customPlayer, double auraBet) {
        double ratio = auraBet / customPlayer.getPlayerAura();

        Player player = customPlayer.getAssociatedPlayer();

        if (ratio >= 0.67 && ratio < 0.68) {
            customPlayer.multiplyPlayerAura(0.67);
            player.sendSystemMessage(Component.translatable(
                    "gambling.event.multiply.message", 0.67
            ));
        } else {
            customPlayer.removePlayerAura((auraBet - 1) * 2);

            AuraAPI.sendAuraPointLostToPlayer(player, (auraBet - 1) * 2);
        }
    }
}
