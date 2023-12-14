package com.github.smuddgge.leaf.listeners;

import com.github.smuddgge.leaf.FriendManager;
import com.github.smuddgge.leaf.Leaf;
import com.github.smuddgge.leaf.datatype.User;
import com.github.smuddgge.leaf.events.EventManager;
import com.github.smuddgge.leaf.events.EventType;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * Manages proxy events for this plugin.
 */
public class EventListener {

    /**
     * Executed before a player connects to the server.
     * Used to fire custom events.
     *
     * @param event Server connected event.
     */
    public static void onPlayerJoinCustomEvent(ServerConnectedEvent event) {
        // Check if the player is null.
        if (event.getPlayer() == null) return;

        // Get the user.
        User user = new User(event.getPlayer());

        // Get server connecting to.
        RegisteredServer server = event.getServer();

        // Check if the server is null.
        if (server == null) return;

        // Set the users connected server.
        user.setConnectedServer(server);

        // Run the events.
        if (event.getPreviousServer().isEmpty()) {
            EventManager.runEvent(EventType.PLAYER_JOIN, user);
            FriendManager.onProxyJoin(user);
            user.addHistory(server, PlayerHistoryEventType.JOIN);
        } else {
            EventManager.runEvent(EventType.PLAYER_SWITCH, user);
        }
    }

    /**
     * Fired when the player first joins the server.
     *
     * @param event The instance of the event.
     */
    public static void onPlayerFirstJoin(PlayerChooseInitialServerEvent event) {
        // Check if we are connected to the database
        if (Leaf.isDatabaseDisabled()) return;

        // Check if the player is null.
        if (event.getPlayer() == null) return;
        if (event.getInitialServer().isEmpty()) return;

        // Get the user and server.
        User user = new User(event.getPlayer());
        RegisteredServer server = event.getInitialServer().get();

        // Update the player in the database.
        user.updateDatabase();

        // Check if the user is vanished.
        if (user.isVanished()) return;

        // Set connected server.
        user.setConnectedServer(server);
    }

    /**
     * Executed when a player connects to a server.
     *
     * @param event Server connected event.
     */
    public static void onPlayerSwitch(ServerConnectedEvent event) {
        // Check if we are connected to the database
        if (Leaf.isDatabaseDisabled()) return;

        // Check if the player or the server is null.
        if (event.getPlayer() == null) return;
        if (event.getPreviousServer().isEmpty()) return;

        // Get the user.
        User user = new User(event.getPlayer());
        RegisteredServer previousServer = event.getPreviousServer().get();

        // Check if the user is vanished.
        if (user.isVanished()) return;

        // Set connected server.
        user.setConnectedServer(event.getServer());

        // Sort processes.
        FriendManager.onChangeServer(user);
        user.addHistory(previousServer, PlayerHistoryEventType.JOIN);
    }

    /**
     * Executed when a player disconnects from a server.
     *
     * @param event Disconnection event.
     */
    public static void onPlayerLeave(DisconnectEvent event) {
        // Check if we are connected to the database
        if (Leaf.isDatabaseDisabled()) return;

        // Check if the player is null.
        if (event.getPlayer() == null) return;

        // Get the user
        User user = new User(event.getPlayer());

        // Update the player in the database
        user.updateDatabase();

        // Get server connecting to
        RegisteredServer server = user.getConnectedServer();

        // Check if the server is null
        if (server == null) return;

        // Run event.
        EventManager.runEvent(EventType.PLAYER_LEAVE, user);

        // Check if the user is vanished
        if (user.isVanished()) return;

        FriendManager.onProxyLeave(user);

        // Add history
        user.addHistory(server, PlayerHistoryEventType.LEAVE);
    }

    /**
     * Executed on player chat event.
     *
     * @param event The instance of the event.
     */
    public static void onPlayerChat(PlayerChatEvent event) {
        // Get the user
        User user = new User(event.getPlayer());

        // Run event.
        EventManager.runEvent(EventType.PLAYER_CHAT.setMessage(event.getMessage()), user);
    }
}
