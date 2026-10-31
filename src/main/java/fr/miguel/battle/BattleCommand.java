package fr.miguel.battle;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BattleCommand {

    @FunctionalInterface
    private interface Action {
        void run(CommandSourceStack src, ServerPlayer player)
            throws BattleManager.BattleException, CommandSyntaxException;
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("battle")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> handle(ctx, (src, me) -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    BattleManager.challenge(src.getServer(), me, target);
                                    src.sendSuccess(() -> Component.translatable("battle.challenge_sent", target.getName()), false);
                                })))
                        // temporaires : serviront de test avant les écrans (étape 2)
                        .then(Commands.literal("accept")
                                .executes(ctx -> handle(ctx, (src, me) -> BattleManager.accept(src.getServer(), me))))
                        .then(Commands.literal("refuse")
                                .executes(ctx -> handle(ctx, (src, me) -> BattleManager.refuse(src.getServer(), me))))
                        .then(Commands.literal("cancel")
                                .executes(ctx -> handle(ctx, (src, me) -> BattleManager.cancel(src.getServer(), me))))));
    }

    private static int handle(CommandContext<CommandSourceStack> ctx, Action action) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.translatable("battle.error.players_only"));
            return 0;
        }
        try {
            action.run(src, player);
            return 1;
        } catch (BattleManager.BattleException e) {
            src.sendFailure(e.text());
            return 0;
        } catch (CommandSyntaxException e) {
            return 0;
        }
    }

}
