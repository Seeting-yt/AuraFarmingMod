package fr.miguel.mixin;

import fr.miguel.api.AuraAPI;
import fr.miguel.battle.BattleArena;
import fr.miguel.battle.BattleManager;
import fr.miguel.battle.BattleSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class PlayerFightingDropItemMixin {


    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("mixinMiguelAuraFarming");


    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;",
    at = @At("HEAD"), cancellable = true)
    private void onPlayerDropItem(ItemStack itemStack, boolean thrownFromHand, Prediction prediction, CallbackInfoReturnable<ItemEntity> cir) {
        ServerPlayer serverPlayer = (ServerPlayer) (Object) this;

        if (serverPlayer == null || AuraAPI.hasAura(itemStack)) return;

        BattleSession session = BattleManager.getSessions().get(serverPlayer.getUUID());


        if (session == null || session.state() != BattleSession.State.FIGHTING) return;

        BattleArena arena = session.arena();

        if (arena == null || !arena.level().equals(serverPlayer.level())) return;

        serverPlayer.getInventory().placeItemBackInInventory(itemStack, prediction);

        serverPlayer.containerMenu.broadcastChanges();

        cir.setReturnValue(null);
    }
}
