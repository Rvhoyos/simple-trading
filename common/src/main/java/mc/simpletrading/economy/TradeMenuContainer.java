package mc.simpletrading.economy;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * A {@link SimpleContainer} subclass that backs the trade GUI chest.
 *
 * Holds a reference to the owning {@link TradeSession} so that any
 * inventory change can notify the session (which resets both players'
 * ready states to prevent last-second scam swaps).
 *
 * The {@link #updatingButtons} flag allows {@link TradeGui} to update
 * the ready-state button items without triggering
 * {@link TradeSession#onContainerChanged()}.
 *
 * @see TradeSession
 * @see TradeGui
 */
public class TradeMenuContainer extends SimpleContainer {

    /** The trade session this container belongs to. */
    private final TradeSession session;

    /**
     * Creates a new trade container.
     *
     * @param size    number of slots (always 54 for a 6-row chest)
     * @param session the owning trade session
     */
    public TradeMenuContainer(int size, TradeSession session) {
        super(size);
        this.session = session;
    }

    /**
     * Returns the trade session associated with this container.
     *
     * @return the owning {@link TradeSession}
     */
    public TradeSession getSession() {
        return session;
    }

    /** When {@code true}, {@link #setChanged()} will not notify the session. */
    private boolean updatingButtons = false;

    /**
     * Sets the button-update guard flag.
     *
     * While {@code true}, calls to {@link #setChanged()} will not
     * propagate to {@link TradeSession#onContainerChanged()}, allowing
     * cosmetic button updates without resetting the ready states.
     *
     * @param updating {@code true} to suppress change notifications
     */
    public void setUpdatingButtons(boolean updating) {
        this.updatingButtons = updating;
    }

    /**
     * Called whenever a slot in this container is modified.
     *
     * Delegates to the session's {@link TradeSession#onContainerChanged()}
     * unless the {@link #updatingButtons} flag is set or no session exists.
     */
    @Override
    public void setChanged() {
        super.setChanged();
        if (session != null && !updatingButtons) {
            session.onContainerChanged();
        }
    }
}
