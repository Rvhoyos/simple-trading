package mc.simpletrading.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import mc.simpletrading.economy.TradeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Registers and handles the {@code /trade} command tree.
 *
 * <h3>Sub-commands</h3>
 * <ul>
 * <li>{@code /trade <player>} — sends a trade request to the target
 * player.</li>
 * <li>{@code /trade accept} — accepts an incoming trade request.</li>
 * <li>{@code /trade deny} — denies an incoming trade request.</li>
 * </ul>
 *
 * <p>
 * All sub-commands require the source to be a {@link ServerPlayer}.
 * Trade lifecycle is delegated to {@link TradeManager}.
 * </p>
 */
public class TradeCommand {

    /**
     * Registers the {@code /trade} literal and its sub-commands with the
     * server's command dispatcher.
     *
     * @param dispatcher the Brigadier command dispatcher provided by the mod loader
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trade")
                .then(Commands.literal("accept")
                        .executes(TradeCommand::acceptTrade))
                .then(Commands.literal("deny")
                        .executes(TradeCommand::denyTrade))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TradeCommand::requestTrade)));
    }

    /**
     * Handles {@code /trade <player>} — sends a trade request.
     * Prevents self-trading and delegates to {@link TradeManager#requestTrade}.
     */
    private static int requestTrade(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer requester = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");

            if (requester.equals(target)) {
                requester.sendSystemMessage(Component.literal("\u00a7cYou cannot trade with yourself."));
                return 0;
            }

            TradeManager.getInstance().requestTrade(requester, target);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to initiate trade."));
        }
        return 1;
    }

    /**
     * Handles {@code /trade accept} — accepts a pending trade request.
     * Delegates to {@link TradeManager#acceptTrade}.
     */
    private static int acceptTrade(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            TradeManager.getInstance().acceptTrade(player, player.getServer());
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to accept trade."));
        }
        return 1;
    }

    /**
     * Handles {@code /trade deny} — denies a pending trade request.
     * Delegates to {@link TradeManager#denyTrade}.
     */
    private static int denyTrade(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            TradeManager.getInstance().denyTrade(player, player.getServer());
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to deny trade."));
        }
        return 1;
    }
}
