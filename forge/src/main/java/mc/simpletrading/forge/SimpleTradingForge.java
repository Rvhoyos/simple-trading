package mc.simpletrading.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;
import mc.simpletrading.SimpleTradingMod;
import mc.simpletrading.commands.TradeCommand;
import mc.simpletrading.economy.TradeManager;

/**
 * Forge mod entry point for Simple Trading.
 *
 * Annotated with {@code @Mod} so Forge discovers and instantiates it
 * automatically. Runs common init and registers event listeners on
 * the Forge event bus for command registration and player disconnect
 * cleanup.
 *
 * @see SimpleTradingMod#init()
 */
@Mod(SimpleTradingMod.MOD_ID)
public final class SimpleTradingForge {

    /**
     * Constructor called by Forge during mod loading.
     * Initialises the mod and registers event listeners.
     */
    public SimpleTradingForge() {
        SimpleTradingMod.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Registers the {@code /trade} command tree with the server dispatcher.
     *
     * @param event the command registration event
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TradeCommand.register(event.getDispatcher());
    }

    /**
     * Cleans up any active trade sessions or pending requests when
     * a player disconnects.
     *
     * @param event the player logout event
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TradeManager.getInstance().handlePlayerLogout(player);
        }
    }
}
