package mc.simpletrading.economy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an active trade between two players.
 *
 * Each session owns a {@link TradeMenuContainer} (54-slot chest) and tracks
 * the ready state of both participants. The session enforces two anti-scam
 * rules:
 * - Container change resets — any item added or removed resets both players
 *   to "Not Ready" via {@link #onContainerChanged()}.
 * - Dual confirmation — the trade only executes when both players are ready
 *   simultaneously.
 *
 * Item ownership:
 * Columns 0–3 belong to Player A (the initiator, left side).
 * Columns 5–8 belong to Player B (the target, right side).
 * Column 4 is the centre divider and is managed by {@link TradeGui}.
 *
 * @see TradeGui
 * @see TradeManager
 */
public class TradeSession {

    /** The initiator of the trade (left side, columns 0–3). */
    private final ServerPlayer playerA;

    /** The target of the trade (right side, columns 5–8). */
    private final ServerPlayer playerB;

    /** Whether Player A has toggled their ready button. */
    private boolean playerAReady = false;

    /** Whether Player B has toggled their ready button. */
    private boolean playerBReady = false;

    /** The shared 54-slot container backing the trade GUI. */
    private final TradeMenuContainer container;

    /** Guard flag; set to {@code true} once the trade completes or is cancelled. */
    private boolean completed = false;

    /**
     * Creates a new trade session between two players.
     *
     * @param playerA the trade initiator (left side)
     * @param playerB the trade target (right side)
     */
    public TradeSession(ServerPlayer playerA, ServerPlayer playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.container = new TradeMenuContainer(54, this);
    }

    /**
     * @return the trade initiator (left side)
     */
    public ServerPlayer getPlayerA() {
        return playerA;
    }

    /**
     * @return the trade target (right side)
     */
    public ServerPlayer getPlayerB() {
        return playerB;
    }

    /**
     * @return the shared container backing this trade's GUI
     */
    public TradeMenuContainer getContainer() {
        return container;
    }

    /**
     * Called when the container's contents change (item added/removed).
     *
     * Resets both players to "Not Ready" and updates the GUI buttons
     * to prevent last-second item swaps after one player has confirmed.
     */
    public void onContainerChanged() {
        if (completed)
            return;

        boolean changed = false;
        if (playerAReady) {
            playerAReady = false;
            changed = true;
        }
        if (playerBReady) {
            playerBReady = false;
            changed = true;
        }

        if (changed) {
            updateGuiButtons();
            sendMessage(playerA, "\u00a7eTrade modified. Status reset to \u00a7cNot Ready\u00a7e.");
            sendMessage(playerB, "\u00a7eTrade modified. Status reset to \u00a7cNot Ready\u00a7e.");
        }
    }

    /** @return {@code true} if Player A is currently marked as ready */
    public boolean isPlayerAReady() {
        return playerAReady;
    }

    /** @return {@code true} if Player B is currently marked as ready */
    public boolean isPlayerBReady() {
        return playerBReady;
    }

    /**
     * Toggles the ready state of the given player and checks whether
     * the trade should execute.
     *
     * @param player the player toggling their ready state
     */
    public void toggleReady(ServerPlayer player) {
        if (player.equals(playerA)) {
            playerAReady = !playerAReady;
            TradeGui.updateButton(container, true, playerAReady);
        } else if (player.equals(playerB)) {
            playerBReady = !playerBReady;
            TradeGui.updateButton(container, false, playerBReady);
        }

        checkAndExecuteTrade();
    }

    /** Synchronises both ready-state buttons with the current state. */
    private void updateGuiButtons() {
        TradeGui.updateButton(container, true, playerAReady);
        TradeGui.updateButton(container, false, playerBReady);
    }

    /**
     * Executes the trade if both players are ready and the session is still active.
     */
    private void checkAndExecuteTrade() {
        if (playerAReady && playerBReady && !completed) {
            completed = true;
            executeTrade();
        }
    }

    /**
     * Swaps items between the two players.
     *
     * Items from Player A's side (columns 0–3) are given to Player B
     * and vice versa. Both players hear a level-up sound and the GUI
     * is closed. The session is then removed from {@link TradeManager}.
     */
    private void executeTrade() {
        List<ItemStack> itemsFromA = new ArrayList<>();
        List<ItemStack> itemsFromB = new ArrayList<>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty())
                continue;

            int col = i % 9;
            if (col < 4) {
                itemsFromA.add(stack.copy());
                container.setItem(i, ItemStack.EMPTY);
            } else if (col > 4) {
                itemsFromB.add(stack.copy());
                container.setItem(i, ItemStack.EMPTY);
            }
        }

        for (ItemStack stack : itemsFromB) {
            giveItem(playerA, stack);
        }

        for (ItemStack stack : itemsFromA) {
            giveItem(playerB, stack);
        }

        TradeManager.getInstance().removeActiveSession(this);

        playerA.closeContainer();
        playerB.closeContainer();

        sendMessage(playerA, "\u00a7aTrade completed successfully!");
        sendMessage(playerB, "\u00a7aTrade completed successfully!");

        playerA.level().playSound(null, playerA.getX(), playerA.getY(), playerA.getZ(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.MASTER, 1f, 1f);
        playerB.level().playSound(null, playerB.getX(), playerB.getY(), playerB.getZ(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.MASTER, 1f, 1f);
    }

    /**
     * Cancels the trade, returning all items to their original owners.
     *
     * Called when a player closes the GUI (via
     * {@link mc.simpletrading.mixin.AbstractContainerMenuMixin})
     * or disconnects (via {@link TradeManager#handlePlayerLogout}).
     */
    public void cancelTrade() {
        if (completed)
            return;
        completed = true;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty())
                continue;

            int col = i % 9;
            if (col < 4) {
                giveItem(playerA, stack.copy());
            } else if (col > 4) {
                giveItem(playerB, stack.copy());
            }
            container.setItem(i, ItemStack.EMPTY);
        }

        TradeManager.getInstance().removeActiveSession(this);

        if (playerA.containerMenu != null)
            playerA.closeContainer();
        if (playerB.containerMenu != null)
            playerB.closeContainer();

        sendMessage(playerA, "\u00a7cTrade cancelled.");
        sendMessage(playerB, "\u00a7cTrade cancelled.");
    }

    /**
     * Gives an item to a player, dropping it on the ground if their
     * inventory is full.
     *
     * @param player the recipient
     * @param stack  the item to give
     */
    private void giveItem(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /**
     * Sends a system chat message to a player.
     *
     * @param player  the recipient
     * @param message the raw message string (may contain formatting codes)
     */
    private void sendMessage(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }
}
