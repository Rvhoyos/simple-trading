package mc.simpletrading;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Main entry point for the Simple Trading mod.
 *
 * Provides the mod ID constant and a shared logger used across all modules.
 * The {@link #init()} method is called by each platform-specific entry point
 * (Fabric / Forge) during server startup.
 *
 * @see mc.simpletrading.fabric.SimpleTradingFabric
 * @see mc.simpletrading.forge.SimpleTradingForge
 */
public final class SimpleTradingMod {
    /** Identifier used for registration, logging, and resource namespacing. */
    public static final String MOD_ID = "simpletrading";

    /** Shared logger for the entire mod. */
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /**
     * Common initialisation logic invoked by both Fabric and Forge loaders.
     * Currently logs a startup message; extend here for cross-platform setup.
     */
    public static void init() {
        LOGGER.info("Simple Trading initialized");
    }
}
