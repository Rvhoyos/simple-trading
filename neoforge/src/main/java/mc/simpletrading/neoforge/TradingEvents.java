package mc.simpletrading.neoforge;

import mc.simpletrading.commands.TradeCommand;
import mc.simpletrading.economy.TradeManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * NeoForge event handlers for Simple Trading.
 *
 * <p>
 * All methods are static and annotated with {@code @SubscribeEvent}.
 * The class is registered on the {@code NeoForge.EVENT_BUS} by
 * {@link SimpleTradingNeoForge}.
 * </p>
 *
 * <h3>Events handled</h3>
 * <ul>
 * <li>{@link RegisterCommandsEvent} — registers the {@code /trade}
 * command.</li>
 * <li>{@link PlayerEvent.PlayerLoggedOutEvent} — cleans up trades on
 * disconnect.</li>
 * </ul>
 */
public class TradingEvents {

    /**
     * Registers the {@code /trade} command tree with the server dispatcher.
     *
     * @param event the command registration event
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TradeCommand.register(event.getDispatcher());
    }

    /**
     * Cleans up any active trade sessions or pending requests when
     * a player disconnects.
     *
     * @param event the player logout event
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TradeManager.getInstance().handlePlayerLogout(player);
        }
    }
}
