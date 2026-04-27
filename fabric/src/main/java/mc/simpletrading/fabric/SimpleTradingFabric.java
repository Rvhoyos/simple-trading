package mc.simpletrading.fabric;

import net.fabricmc.api.ModInitializer;

import mc.simpletrading.SimpleTradingMod;
import mc.simpletrading.commands.TradeCommand;
import mc.simpletrading.economy.TradeManager;

/**
 * Fabric mod initialiser for Simple Trading.
 *
 * Registers commands via {@code CommandRegistrationCallback} and cleans up
 * active trades and pending requests on player disconnect via
 * {@link TradeManager#handlePlayerLogout}.
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
