package fr.miguel.pve.game;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miguel.pve.core.attack.AttackInterface;
import fr.miguel.pve.core.registries.AttackRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * DEBUG ONLY CLASS. SHOULD NEVER BE USED OUTSIDE OF THIS MOD.
 * @deprecated Methods will change soon, so don't use it for now ;-). <br>
 * Use {@link fr.miguel.items.CombatItem#interactLivingEntity(ItemStack, Player, LivingEntity, InteractionHand)} instead
 */
@ApiStatus.Internal
@Deprecated()
public final class PveCommand {

    @FunctionalInterface
    private interface Action {
        void run(ServerPlayer player) throws PveManager.PveException, CommandSyntaxException;
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("pve")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("launch")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            warn(p);
                                            boolean started = PveManager.launchCombat(p, EntityArgument.getEntity(ctx, "target"));
                                            if (!started) ctx.getSource().sendFailure(Component.translatable("pve.error.not_combatable"));
                                            return started ? 1 : 0;
                                        })))
                        .then(Commands.literal("attack")
                                .then(Commands.argument("attack", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider
                                                .suggest(attackIds(), builder))
                                        .executes(ctx -> handle(ctx, p -> {
                                            PveManager.attack(p, StringArgumentType.getString(ctx, "attack"));
                                            warn(p);
                                        }))))
                        .then(Commands.literal("flee")
                                .executes(ctx -> {
                                    warn(ctx.getSource().getPlayerOrException());

                                    return handle(ctx, PveManager::flee);
                                }))));
    }

    private static List<String> attackIds() {
        List<String> ids = new ArrayList<>();
        for (AttackInterface a : AttackRegistry.all()) ids.add(a.id());
        return ids;
    }

    private static int handle(CommandContext<CommandSourceStack> ctx, Action action) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        try {
            action.run(player);
            return 1;
        } catch (PveManager.PveException e) {
            ctx.getSource().sendFailure(e.text());
            return 0;
        }
    }

    private static void warn(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("Theses commands are for DEBUG only. Do not use them unless you know what you're doing!"));
    }
}
