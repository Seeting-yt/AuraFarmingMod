package fr.miguel.api.events.random;

import fr.miguel.api.events.aura.AuraChangedCallBack;
import fr.miguel.api.events.cancelable.CancelableEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/***
 * A cancelable event.
 * Listen to this class to get both {@link BeforeScreen} and {@link AfterGambling} events.
 */
public class RandomAuraGambleEvent extends CancelableEvent {

    private final ServerPlayer player;
    private final MinecraftServer server;

    public RandomAuraGambleEvent(ServerPlayer player, MinecraftServer server) {
        super();

        this.player = player;
        this.server = server;
    }


    public ServerPlayer getPlayer() {
        return this.player;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    /**
     * Event called just before the aura gambling screen is open.
     * If this event is canceled, the screen is not opened.
     */
    public static class BeforeScreen extends RandomAuraGambleEvent {

        public BeforeScreen(ServerPlayer player, MinecraftServer server) {
            super(player, server);
        }
    }

    /**
     * Event called when the player validate his bet.
     * If this event is canceled, the screen was already opened but nothing happen.
     */
    public static class AfterGambling extends RandomAuraGambleEvent {

        private double auraGambled;

        public AfterGambling(ServerPlayer player, MinecraftServer server, double auraGambled) {
            super(player, server);

            this.auraGambled = auraGambled;
        }

        public double getAuraGambled() {
            return auraGambled;
        }

        public void setAuraGambled(double auraGambled) {
            this.auraGambled = auraGambled;
        }
    }
}
