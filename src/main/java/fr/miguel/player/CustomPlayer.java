package fr.miguel.player;

import com.mojang.serialization.Codec;
import fr.miguel.AuraFarm;
import fr.miguel.api.events.aura.AuraChangedCallBack;
import fr.miguel.api.events.aura.AuraChangedEvent;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class CustomPlayer {

    private final Player player;

    private static final HashMap<UUID, CustomPlayer> customPlayers = new HashMap<>();

    private double playerAura;

    public static final AttachmentType<Double> AURA_ATTACHMENT =
            AttachmentRegistry.create(
                    AuraFarm.id("player_aura"),
                    builder
                            -> builder.persistent(Codec.DOUBLE).copyOnDeath()
            );

    private CustomPlayer(@NotNull Player player) {

        this.player = Objects.requireNonNull(player);;
        this.playerAura = 0;

        customPlayers.put(player.getUUID(), this);
    }

    /**
     * Client side ONLY!
     * @param newValue the new aura value
     */
    public void setAuraClient(double newValue) {
        this.playerAura = newValue;
    }

    public void setPlayerAura(double newPlayerAura) {
        AuraChangedEvent event = new AuraChangedEvent(this, getPlayerAura(), newPlayerAura);

        AuraChangedCallBack.EVENT.invoker().interact(event);

        if (event.isCanceled())
            return;

        this.playerAura = event.getNewAmount();

        if (this.player instanceof ServerPlayer serverPlayer)
            synchroToClient(serverPlayer);
    }

    public void addPlayerAura(double playerAura) {
        setPlayerAura(this.getPlayerAura() + playerAura);
    }

    public void removePlayerAura(double playerAura) {
        setPlayerAura(this.getPlayerAura() - playerAura);
    }


    public void multiplyPlayerAura(@Range(from = 0, to = Long.MAX_VALUE) double multiplier) {
        setPlayerAura(this.getPlayerAura()*multiplier);
    }

    public double getPlayerAura() {
        return this.playerAura;
    }

    public Player getAssociatedPlayer() {
        return this.player;
    }

    private void synchroToClient(ServerPlayer player) {
        ServerPlayNetworking.send(player, new PlayerPayloads.AuraChangedPayload(player.getUUID(), this.getPlayerAura()));
    }

    private void init() {
        this.getAssociatedPlayer()
                .getAttachedOrSet(AURA_ATTACHMENT, this.getPlayerAura());
    }

    private CustomPlayer save() {
        this.getAssociatedPlayer()
                .setAttached(AURA_ATTACHMENT, this.getPlayerAura());
        return this;
    }

    private void remove() {
        customPlayers.remove(this.getAssociatedPlayer().getUUID());
    }

    @Contract(pure = true, value = "_ -> new")
    public static @NotNull CustomPlayer getCustomPlayerOrNew(@NotNull Player player) {
        return customPlayers.containsKey(player.getUUID()) ? customPlayers.get(player.getUUID()) : new CustomPlayer(player);
    }

    public static void createInstanceOnJoin(ServerPlayer player) {
        CustomPlayer.getCustomPlayerOrNew(player).init();

    }

    public static void saveDataOnLeave(ServerPlayer player) {
        CustomPlayer.getCustomPlayerOrNew(player)
                .save()
                .remove();
    }

    public static void savePlayersData(MinecraftServer server, boolean flush, boolean force) {
        for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers())
            CustomPlayer.getCustomPlayerOrNew(serverPlayer).save();

    }

    public static void changePlayerXpValue(AuraChangedEvent event) {
        if (event.isCanceled())
            return;

        if (!(event.getCustomPlayer().getAssociatedPlayer() instanceof ServerPlayer serverPlayer))
            return;

        serverPlayer.experienceLevel = (int) event.getNewAmount();

        serverPlayer.connection.send(new ClientboundSetExperiencePacket(
                serverPlayer.experienceProgress,
                serverPlayer.totalExperience,
                serverPlayer.experienceLevel
        ));
    }


}
