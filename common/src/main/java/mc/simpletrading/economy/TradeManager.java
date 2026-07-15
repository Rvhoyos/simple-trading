package mc.simpletrading.economy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton manager responsible for the full trade lifecycle.
 *
 * Maintains two maps: pending requests, keyed by the target player's UUID,
 * storing who requested the trade and when (requests expire after 60
 * seconds); and active sessions, keyed by both participants' UUIDs,
 * pointing to the shared {@link TradeSession}.
 *
 * Typical flow: {@link #requestTrade} creates a pending request and sends
 * clickable ACCEPT / DENY buttons to the target. {@link #acceptTrade}
 * consumes the pending request, creates a {@link TradeSession}, and opens
 * the GUI via {@link TradeGui#open}. Players interact inside the GUI until
 * both click "Ready", then {@link TradeSession#executeTrade} swaps items
 * and calls {@link #removeActiveSession} to clean up.
 *
 * @see TradeSession
 * @see TradeGui
 */
public class TradeManager {
    private static final TradeManager INSTANCE = new TradeManager();

    /** Maps a participant's UUID to their active {@link TradeSession}. */
    private final Map<UUID, TradeSession> activeSessions = new HashMap<>();

    /** Maps the target player's UUID to the incoming trade request. */
    private final Map<UUID, TradeRequest> pendingRequests = new HashMap<>();

    private TradeManager() {
    }

    /**
     * Returns the global singleton instance.
     *
     * @return the trade manager instance
     */
    public static TradeManager getInstance() {
        return INSTANCE;
    }

    /**
     * Sends a trade request from one player to another.
     *
     * If either player is already in a trade, the request is rejected.
     * Otherwise, the target receives clickable ACCEPT / DENY chat buttons.
     *
     * @param requester the player initiating the trade
     * @param target    the player being asked to trade
     */
    public void requestTrade(ServerPlayer requester, ServerPlayer target) {
        if (hasActiveSession(requester) || hasActiveSession(target)) {
            requester.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("\u00a7cOne of the players is already in a trade."));
            return;
        }

        pendingRequests.put(target.getUUID(), new TradeRequest(requester.getUUID(), System.currentTimeMillis()));

        requester.sendSystemMessage(net.minecraft.network.chat.Component
                .literal("\u00a7aTrade request sent to " + target.getName().getString() + "."));

        net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component
                .literal("\u00a7e" + requester.getName().getString() + " \u00a7ahas requested to trade with you. ");

        net.minecraft.network.chat.MutableComponent acceptBtn = net.minecraft.network.chat.Component
                .literal("\u00a7a\u00a7l[ACCEPT]");
        acceptBtn.setStyle(net.minecraft.network.chat.Style.EMPTY
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/trade accept"))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                        net.minecraft.network.chat.Component.literal("\u00a7aClick to accept"))));

        net.minecraft.network.chat.MutableComponent denyBtn = net.minecraft.network.chat.Component
                .literal("\u00a7c\u00a7l[DENY]");
        denyBtn.setStyle(net.minecraft.network.chat.Style.EMPTY
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/trade deny"))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                        net.minecraft.network.chat.Component.literal("\u00a7cClick to deny"))));

        message.append(acceptBtn).append(net.minecraft.network.chat.Component.literal(" ")).append(denyBtn);

        target.sendSystemMessage(message);
    }

    /**
     * Accepts a pending trade request on behalf of the target player.
     *
     * Validates that the request exists, has not expired, and that both
     * players are available. On success, creates a {@link TradeSession} and
     * opens {@link TradeGui} for both participants.
     *
     * @param target the player accepting the trade
     * @param server the Minecraft server instance (used to resolve the requester)
     */
    public void acceptTrade(ServerPlayer target, net.minecraft.server.MinecraftServer server) {
        TradeRequest request = pendingRequests.remove(target.getUUID());
        if (request == null || request.isExpired()) {
            target.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("\u00a7cYou do not have any pending trade requests."));
            return;
        }

        ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId);
        if (requester == null) {
            target.sendSystemMessage(net.minecraft.network.chat.Component
                    .literal("\u00a7cThe player who requested the trade is no longer online."));
            return;
        }

        if (hasActiveSession(requester) || hasActiveSession(target)) {
            target.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("\u00a7cOne of the players is already in a trade."));
            return;
        }

        TradeSession session = new TradeSession(requester, target);
        activeSessions.put(requester.getUUID(), session);
        activeSessions.put(target.getUUID(), session);

        TradeGui.open(session);
    }

    /**
     * Denies a pending trade request on behalf of the target player.
     * Notifies both parties of the denial.
     *
     * @param target the player denying the trade
     * @param server the Minecraft server instance (used to resolve the requester)
     */
    public void denyTrade(ServerPlayer target, net.minecraft.server.MinecraftServer server) {
        TradeRequest request = pendingRequests.remove(target.getUUID());
        if (request != null) {
            target.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u00a7cTrade request denied."));
            ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId);
            if (requester != null) {
                requester.sendSystemMessage(net.minecraft.network.chat.Component
                        .literal("\u00a7c" + target.getName().getString() + " denied your trade request."));
            }
        } else {
            target.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("\u00a7cYou do not have any pending trade requests."));
        }
    }

    /**
     * Checks whether the given player is currently in an active trade.
     *
     * @param player the player to check
     * @return {@code true} if the player has an active session
     */
    public boolean hasActiveSession(ServerPlayer player) {
        return activeSessions.containsKey(player.getUUID());
    }

    /**
     * Returns the active trade session for the given player, or {@code null}.
     *
     * @param player the player to look up
     * @return the active session, or {@code null} if none exists
     */
    public TradeSession getSession(ServerPlayer player) {
        return activeSessions.get(player.getUUID());
    }

    /**
     * Removes both participants' entries from the active-session map.
     * Called by {@link TradeSession} when a trade completes or is cancelled.
     *
     * @param session the session to clean up
     */
    public void removeActiveSession(TradeSession session) {
        activeSessions.remove(session.getPlayerA().getUUID());
        activeSessions.remove(session.getPlayerB().getUUID());
    }

    /**
     * Cleans up all trade state for a player who has disconnected.
     *
     * Cancels any active session (returning items to both players)
     * and removes any pending requests involving this player.
     *
     * @param player the player who logged out
     */
    public void handlePlayerLogout(ServerPlayer player) {
        TradeSession session = getSession(player);
        if (session != null) {
            session.cancelTrade();
        }
        pendingRequests.remove(player.getUUID());
        pendingRequests.values().removeIf(request -> request.requesterId.equals(player.getUUID()));
    }

    /**
     * Internal record of a pending trade request.
     * Expires after 60 seconds to prevent stale requests.
     */
    private static class TradeRequest {
        /** UUID of the player who initiated the request. */
        final UUID requesterId;

        /** Epoch-millisecond timestamp when the request was created. */
        final long timestamp;

        TradeRequest(UUID requesterId, long timestamp) {
            this.requesterId = requesterId;
            this.timestamp = timestamp;
        }

        /**
         * @return {@code true} if more than 60 seconds have elapsed since creation
         */
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000;
        }
    }
}
