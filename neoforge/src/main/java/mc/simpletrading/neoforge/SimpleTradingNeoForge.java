package mc.simpletrading.neoforge;

import net.neoforged.fml.common.Mod;

import net.neoforged.neoforge.common.NeoForge;
import mc.simpletrading.SimpleTradingMod;

/**
 * NeoForge mod entry point for Simple Trading.
 *
 * <p>
 * Annotated with {@code @Mod} so NeoForge discovers and instantiates it
 * automatically. Runs common init and registers {@link TradingEvents} on
 * the NeoForge event bus.
 * </p>
 *
 * @see SimpleTradingMod#init()
 * @see TradingEvents
 */
@Mod(SimpleTradingMod.MOD_ID)
public final class SimpleTradingNeoForge {

    /**
     * Constructor called by NeoForge during mod loading.
     * Initialises the mod and registers event listeners.
     */
    public SimpleTradingNeoForge() {
        SimpleTradingMod.init();
        NeoForge.EVENT_BUS.register(TradingEvents.class);
    }
}
