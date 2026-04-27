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

/** Forge entrypoint. Initializes the mod and registers events via Forge event bus. */
@Mod(SimpleTradingMod.MOD_ID)
public final class SimpleTradingForge {
    public SimpleTradingForge() {
        SimpleTradingMod.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TradeCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TradeManager.getInstance().handlePlayerLogout(player);
        }
    }
}
