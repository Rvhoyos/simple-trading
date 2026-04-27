package mc.simpletrading.mixin;

import mc.simpletrading.economy.TradeMenuContainer;
import mc.simpletrading.economy.TradeGui;
import mc.simpletrading.economy.TradeSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.Container;

/**
 * Mixin into {@link AbstractContainerMenu} that intercepts slot clicks
 * and container closure for trade GUI enforcement.
 *
 * Click interception: when the open menu is a {@link ChestMenu} backed by a
 * {@link TradeMenuContainer}, all clicks are routed through
 * {@link TradeGui#onClick}. If that method returns {@code true},
 * vanilla click handling is cancelled via {@link CallbackInfo#cancel()}.
 *
 * Container removal: when a trade container is closed (e.g. the player
 * presses ESC), the associated {@link TradeSession#cancelTrade()} is
 * invoked to return all items to their original owners.
 *
 * @see ChestMenuMixin
 * @see TradeGui#onClick
 */
@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {

    /**
     * Intercepts all slot clicks. Delegates trade-related clicks to
     * {@link TradeGui#onClick} and cancels vanilla handling when appropriate.
     */
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void simpletrading$interceptClick(int slotId, int button, ClickType clickType, Player player,
            CallbackInfo ci) {
        if ((Object) this instanceof ChestMenu chestMenu) {
            Container container = ((ChestMenuMixin) chestMenu).getContainer();

            if (container instanceof TradeMenuContainer tradeContainer) {
                if (player instanceof ServerPlayer sp) {
                    boolean shouldCancel = TradeGui.onClick(sp, tradeContainer, slotId, clickType);
                    if (shouldCancel) {
                        ci.cancel();
                    }
                }
            }
        }
    }

    /**
     * Intercepts container removal (GUI close). If the closed container
     * is a trade container, cancels the trade to safely return items.
     */
    @Inject(method = "removed", at = @At("HEAD"))
    private void simpletrading$onRemoved(Player player, CallbackInfo ci) {
        if ((Object) this instanceof ChestMenu chestMenu) {
            Container container = ((ChestMenuMixin) chestMenu).getContainer();
            if (container instanceof TradeMenuContainer tradeContainer) {
                if (player instanceof ServerPlayer sp) {
                    TradeSession session = tradeContainer.getSession();
                    if (session != null) {
                        session.cancelTrade();
                    }
                }
            }
        }
    }
}
