package fr.miguel.pve.game;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miguel.pve.attack.AttackInterface;
import fr.miguel.pve.attack.AttackRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * DEBUG ONLY CLASS. SHOULD NEVER BE USED OUTSIDE OF THIS MOD.
 * @deprecated Methods will change soon, so don't use it for now ;-).
 */
@ApiStatus.Internal
@Deprecated()
public final class PveCommand {

    @FunctionalInterface
    private interface Action {
        void run(ServerPlayer player) throws PveManager.PveException, CommandSyntaxException;
    }

    public static void register() {

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
}
