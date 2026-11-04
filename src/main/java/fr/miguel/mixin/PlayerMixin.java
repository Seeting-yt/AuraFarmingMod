package fr.miguel.mixin;

import fr.miguel.api.events.random.RandomAuraGambleEvent;
import fr.miguel.api.events.random.RandomEventCallBack;
import fr.miguel.gambling_events.GamblingEvent;
import fr.miguel.gambling_events.GamblingEvents;
import fr.miguel.items.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class PlayerMixin {

    @Shadow
    @Final
    private MinecraftServer server;
    @Unique
    private static final int TARGET_AVG_MINUTES = 1;
    @Unique
    private static final int TEST_INTERVAL_SECONDS = 10;

    @Unique
    private static final int TARGET_AVG_SECONDS = TARGET_AVG_MINUTES * 60;
    @Unique
    private static final int TICKS_BETWEEN_TESTS = TEST_INTERVAL_SECONDS * 20;
    @Unique
    private static final int PROBABILITY_CHANCE = TARGET_AVG_SECONDS / TEST_INTERVAL_SECONDS;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        long now = player.tickCount;

        if (now % TICKS_BETWEEN_TESTS != 0) return;

        if (player.getRandom().nextInt(PROBABILITY_CHANCE) == 0) {
            RandomAuraGambleEvent mainEvent = new RandomAuraGambleEvent(player, server);
            RandomAuraGambleEvent.BeforeScreen beforeScreen = new RandomAuraGambleEvent.BeforeScreen(player, server);


            RandomEventCallBack.EVENT.invoker().interact(mainEvent);
            RandomEventCallBack.BeforeScreen.EVENT.invoker()
                    .interact(beforeScreen);

            if (mainEvent.isCanceled() || beforeScreen.isCanceled())
                return;

            int eventsCount = GamblingEvents.getEvents().size();

            int eventPickedIndex = player.getRandom().nextInt(eventsCount);

            GamblingEvent eventPicked = GamblingEvents.getPickedEventByIndex(eventPickedIndex);

            player.getInventory().placeItemBackInInventory(
                    new ItemStack(ModItems.DICE_ITEM), Prediction.PREDICTED);

            GamblingEvents.addPickedEvent(player, eventPicked);
        }
    }

    @Inject(method = "giveExperiencePoints", at = @At("HEAD"), cancellable = true)
    private void onGiveXp(int i, CallbackInfo ci) {
        ci.cancel();
    }
}
