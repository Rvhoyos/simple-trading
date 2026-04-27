package mc.simpletrading.economy;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side GUI controller for the trade window.
 *
 * The trade GUI is presented as a standard 6-row chest ({@link ChestMenu}).
 * The centre column (column 4) acts as a divider and contains the
 * ready/not-ready buttons for each player. Columns 0–3 belong to Player A
 * (the trade initiator) and columns 5–8 belong to Player B (the target).
 *
 * Layout (9 x 6 grid):
 *   A A A A | D | B B B B
 *   A A A A | D | B B B B
 *   A A A A |[A]| B B B B   -- Player A's ready button (slot 22)
 *   A A A A |[B]| B B B B   -- Player B's ready button (slot 31)
 *   A A A A | D | B B B B
 *   A A A A | D | B B B B
 *
 * Click interception is handled via
 * {@link mc.simpletrading.mixin.AbstractContainerMenuMixin}, which delegates
 * to {@link #onClick} for any chest backed by a {@link TradeMenuContainer}.
 *
 * @see TradeSession
 * @see TradeMenuContainer
 */
public class TradeGui {

    /** Slot indices for the centre divider column (column 4 of each row). */
    private static final int[] CENTER_COLUMN = { 4, 13, 22, 31, 40, 49 };

    /** Slot index of Player A's ready/not-ready toggle button. */
    private static final int BUTTON_A = 22;

    /** Slot index of Player B's ready/not-ready toggle button. */
    private static final int BUTTON_B = 31;

    /**
     * Opens the trade GUI for both participants of the given session.
     *
     * Populates the shared {@link TradeMenuContainer} with divider panes
     * and initial "Not Ready" buttons, then opens a {@link ChestMenu} for
     * each player with a personalised title.
     *
     * @param session the active trade session to display
     */
    public static void open(TradeSession session) {
        populate(session.getContainer());

        openMenuForPlayer(session.getPlayerA(), session.getContainer(),
                "Trading with " + session.getPlayerB().getName().getString());
        openMenuForPlayer(session.getPlayerB(), session.getContainer(),
                "Trading with " + session.getPlayerA().getName().getString());
    }

    /**
     * Opens a 6-row chest menu backed by the shared trade container for
     * a single player.
     */
    private static void openMenuForPlayer(ServerPlayer player, TradeMenuContainer container, String title) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal(title);
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
                return ChestMenu.sixRows(containerId, inventory, container);
            }
        });
    }

    /**
     * Fills the centre column with black glass-pane dividers and places
     * the initial "Not Ready" buttons for both players.
     */
    private static void populate(TradeMenuContainer container) {
        ItemStack divider = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        divider.setHoverName(Component.empty());

        for (int index : CENTER_COLUMN) {
            container.setItem(index, divider);
        }

        updateButton(container, true, false);
        updateButton(container, false, false);
    }

    /**
     * Updates a player's ready-state button in the trade GUI.
     *
     * Uses a {@link Items#LIME_DYE} for "Ready" and a
     * {@link Items#RED_DYE} for "Not Ready". The container's
     * {@link TradeMenuContainer#setUpdatingButtons} flag is toggled
     * so that the cosmetic change does not trigger
     * {@link TradeSession#onContainerChanged}.
     *
     * @param container the shared trade container
     * @param isPlayerA {@code true} to update Player A's button, {@code false} for
     *                  Player B
     * @param isReady   the new ready state to display
     */
    public static void updateButton(TradeMenuContainer container, boolean isPlayerA, boolean isReady) {
        int slot = isPlayerA ? BUTTON_A : BUTTON_B;
        String name = isPlayerA ? "Player A" : "Player B";

        ItemStack button = new ItemStack(isReady ? Items.LIME_DYE : Items.RED_DYE);
        button.setHoverName(
                Component.literal(isReady ? "\u00a7a" + name + " is Ready" : "\u00a7c" + name + " is Not Ready"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Click to toggle your ready state.").withStyle(ChatFormatting.GRAY));
        CompoundTag display = button.getOrCreateTagElement("display");
        ListTag loreTag = new ListTag();
        for (Component line : lore) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        display.put("Lore", loreTag);

        container.setUpdatingButtons(true);
        container.setItem(slot, button);
        container.setUpdatingButtons(false);
    }

    /**
     * Processes a click event inside the trade GUI.
     *
     * Enforces the following rules:
     * 1. {@link ClickType#PICKUP_ALL} (double-click) is always cancelled to
     *    prevent vanilla from sweeping items across both sides.
     * 2. Clicks on the centre column are consumed; only the owner's
     *    ready button responds to {@link ClickType#PICKUP}.
     * 3. Players may only interact with their own side (A = cols 0–3,
     *    B = cols 5–8).
     * 4. Shift-clicks from the player's own inventory are routed to their
     *    designated side via {@link #handleShiftClick}.
     *
     * @param player    the player who clicked
     * @param container the shared trade container
     * @param slotId    the raw slot index that was clicked
     * @param clickType the type of click performed
     * @return {@code true} if vanilla handling should be cancelled
     */
    public static boolean onClick(ServerPlayer player, TradeMenuContainer container, int slotId, ClickType clickType) {
        TradeSession session = container.getSession();
        if (session == null)
            return false;

        if (clickType == ClickType.PICKUP_ALL) {
            return true;
        }

        boolean isPlayerA = player.equals(session.getPlayerA());

        if (slotId < 0)
            return false;

        if (slotId < container.getContainerSize()) {
            int col = slotId % 9;

            if (col == 4) {
                if ((isPlayerA && slotId == BUTTON_A) || (!isPlayerA && slotId == BUTTON_B)) {
                    if (clickType == ClickType.PICKUP) {
                        session.toggleReady(player);
                    }
                }
                return true;
            }

            if (isPlayerA && col > 4) {
                player.sendSystemMessage(
                        Component.literal("\u00a7cYou cannot interact with the other player's items!"));
                return true;
            }
            if (!isPlayerA && col < 4) {
                player.sendSystemMessage(
                        Component.literal("\u00a7cYou cannot interact with the other player's items!"));
                return true;
            }

            return false;
        }

        if (clickType == ClickType.QUICK_MOVE) {
            handleShiftClick(player, container, slotId, isPlayerA);
            return true;
        }

        return false;
    }

    /**
     * Moves an item from the player's bottom inventory into their designated
     * side of the trade chest via shift-click.
     *
     * First attempts to stack with existing identical items, then fills
     * empty slots. Only the player's own columns are targeted.
     *
     * @param player     the player performing the shift-click
     * @param container  the shared trade container
     * @param playerSlot the slot index in the player's inventory
     * @param isPlayerA  {@code true} if the player is Player A (left side)
     */
    private static void handleShiftClick(ServerPlayer player, TradeMenuContainer container, int playerSlot,
            boolean isPlayerA) {
        ItemStack clickedStack = player.containerMenu.getSlot(playerSlot).getItem();
        if (clickedStack.isEmpty())
            return;

        int startCol = isPlayerA ? 0 : 5;
        int endCol = isPlayerA ? 3 : 8;

        // First pass: try to stack with existing items
        for (int row = 0; row < 6; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int index = row * 9 + col;
                ItemStack existing = container.getItem(index);
                if (!existing.isEmpty() && ItemStack.isSameItemSameTags(clickedStack, existing)) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    if (space > 0) {
                        int amountToMove = Math.min(space, clickedStack.getCount());
                        existing.grow(amountToMove);
                        clickedStack.shrink(amountToMove);
                        container.setChanged();
                        if (clickedStack.isEmpty())
                            return;
                    }
                }
            }
        }

        // Second pass: put in empty slots
        for (int row = 0; row < 6; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int index = row * 9 + col;
                if (container.getItem(index).isEmpty()) {
                    container.setItem(index, clickedStack.copy());
                    clickedStack.setCount(0);
                    container.setChanged();
                    return;
                }
            }
        }
    }
}
