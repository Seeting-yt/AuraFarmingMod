package fr.miguel.gambling_events;

import com.google.common.collect.ImmutableList;
import fr.miguel.api.events.random.RandomAuraGambleEvent;
import fr.miguel.api.events.random.RandomEventCallBack;
import fr.miguel.gambling_events.bonus.MultiplyEvent;
import fr.miguel.gambling_events.malus.SubtractEvent;
import fr.miguel.player.CustomPlayer;
import fr.miguel.player.PlayerPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public final class GamblingEvents {

    private static final ArrayList<GamblingEvent> EVENTS =
            new ArrayList<>();
    private static final HashMap<UUID, GamblingEvent> REGISTERED_PICKED_EVENTS = new HashMap<>();

    static {
        registerEvent(new MultiplyEvent());
        registerEvent(new SubtractEvent());
    }

    public static void addPickedEvent(Player player, GamblingEvent event) {
        REGISTERED_PICKED_EVENTS.put(player.getUUID(), event);
    }

    public static <T extends GamblingEvent> void registerEvent(T event) {
        EVENTS.add(event);
    }

    public static ImmutableList<GamblingEvent> getEvents() {
        return ImmutableList.copyOf(EVENTS);
    }

    @Nullable
    public static GamblingEvent getPickedEvent(Player player) {
        return REGISTERED_PICKED_EVENTS.get(player.getUUID());
    }

    public static GamblingEvent getPickedEventByIndex(int index) {
        return EVENTS.get(index);
    }

    public static void activatePickedEventAfterGambling(PlayerPayloads.GamblingEvent gamblingEvent, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.server().getPlayerList().getPlayer(gamblingEvent.playerID());

            if (player == null) return;

            GamblingEvent event = getPickedEvent(player);

            if (event == null) return;
            RandomAuraGambleEvent mainEvent = new RandomAuraGambleEvent(player, context.server());
            RandomAuraGambleEvent.AfterGambling afterGambling = new RandomAuraGambleEvent.AfterGambling(player, context.server(), gamblingEvent.auraBet());

            RandomEventCallBack.EVENT.invoker()
                            .interact(mainEvent);
            RandomEventCallBack.AfterGambling.EVENT
                            .invoker().interact(afterGambling);

            if (mainEvent.isCanceled() || afterGambling.isCanceled())
                return;

            event.action(CustomPlayer.getCustomPlayerOrNew(player), gamblingEvent.auraBet());
        });
    }
}
