package fr.miguel.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import fr.miguel.api.AuraAPI;
import fr.miguel.network.OpenScreenPayload;
import fr.miguel.player.CustomPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class AuraCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("aura")

                        .then(Commands.literal("get")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int aura = (int) CustomPlayer.getCustomPlayerOrNew(player).getPlayerAura();
                                    ctx.getSource().sendSuccess(() -> AuraAPI.auraGetCommandMessage(aura), false);
                                    return 1;
                                }))

                        .then(Commands.literal("set")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                    double value = DoubleArgumentType.getDouble(ctx, "value");
                                                    CustomPlayer.getCustomPlayerOrNew(target).setPlayerAura(value);
                                                    ctx.getSource().sendSuccess(
                                                            () -> AuraAPI.auraAddedCommandMessage(target, value), true);
                                                    return 1;
                                                }))))
        );

        dispatcher.register(
                Commands.literal("gambling")
                        .executes(ctx -> {
                            ServerPlayNetworking.send(ctx.getSource().getPlayerOrException(), new OpenScreenPayload());

                            return 1;
                        })
        );
    }
}
