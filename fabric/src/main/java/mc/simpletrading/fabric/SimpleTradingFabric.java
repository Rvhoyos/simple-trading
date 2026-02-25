package mc.simpletrading.fabric;

import net.fabricmc.api.ModInitializer;

import mc.simpletrading.SimpleTradingMod;
import mc.simpletrading.commands.TradeCommand;
import mc.simpletrading.economy.TradeManager;

/**
 * Fabric mod initialiser for Simple Trading.
 *
 * <p>
 * Registers the following with Fabric API events:
 * </p>
 * <ul>
 * <li><b>Commands</b> — {@link TradeCommand} via the
 * {@code CommandRegistrationCallback}.</li>
 * <li><b>Player disconnect</b> — cleans up active trades and pending
 * requests via {@link TradeManager#handlePlayerLogout}.</li>
 * </ul>
 *
 * @see SimpleTradingMod#init()
 */
public final class SimpleTradingFabric implements ModInitializer {

    /**
     * Called by Fabric Loader during server startup.
     * Runs common init and hooks into Fabric-specific events.
     */
    @Override
    public void onInitialize() {
        SimpleTradingMod.init();

        // Register /trade commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) -> {
                    TradeCommand.register(dispatcher);
                });

        // Cancel trades on player disconnect
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT
                .register((handler, server) -> {
                    TradeManager.getInstance().handlePlayerLogout(handler.getPlayer());
                });
    }
}
