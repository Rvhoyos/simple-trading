package mc.simpletrading.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for {@link ChestMenu} that exposes the private
 * {@code container} field.
 *
 * <p>
 * This is used by {@link AbstractContainerMenuMixin} to determine
 * whether a chest GUI is backed by a
 * {@link mc.simpletrading.economy.TradeMenuContainer} (and therefore
 * requires trade-specific click handling).
 * </p>
 *
 * @see AbstractContainerMenuMixin
 */
@Mixin(ChestMenu.class)
public interface ChestMenuMixin {

    /**
     * Returns the underlying {@link Container} instance of the chest menu.
     *
     * @return the container backing this chest menu
     */
    @Accessor("container")
    Container getContainer();
}
