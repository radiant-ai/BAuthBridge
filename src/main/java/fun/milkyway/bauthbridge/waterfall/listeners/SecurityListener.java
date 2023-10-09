package fun.milkyway.bauthbridge.waterfall.listeners;

import fun.milkyway.bauthbridge.waterfall.BAuthBridgeWaterfall;
import fun.milkyway.bauthbridge.waterfall.managers.BridgedPlayerManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class SecurityListener implements Listener {
    private final BAuthBridgeWaterfall plugin;
    private final BridgedPlayerManager bridgedPlayerManager;
    public SecurityListener(BAuthBridgeWaterfall plugin, BridgedPlayerManager bridgedPlayerManager) {
        this.plugin = plugin;
        this.bridgedPlayerManager = bridgedPlayerManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(ChatEvent event) {
        Connection connection = event.getSender();
        if (connection instanceof ProxiedPlayer proxiedPlayer) {
            if (bridgedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
                return;
            }
            if (isAllowedString(event.getMessage()) &&
                    proxiedPlayer.getServer().getInfo().getName().equals(plugin.getConfiguration().getString("auth_server", "auth"))) {
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabcompletePre(TabCompleteEvent event) {
        if (event.getSender() instanceof ProxiedPlayer proxiedPlayer) {
            if (!bridgedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
                event.getSuggestions().clear();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabcompletePost(TabCompleteResponseEvent event) {
        Connection connection = event.getSender();
        if (connection instanceof ProxiedPlayer proxiedPlayer) {
            if (!bridgedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
                event.getSuggestions().removeIf(suggestion -> !isAllowedCommand(suggestion));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer proxiedPlayer = event.getPlayer();
        String authServerName = plugin.getConfiguration().getString("auth_server", "auth");
        boolean isAuthorized = bridgedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId());
        if (event.getTarget().getName().equals(authServerName)) {
            if (isAuthorized && plugin.getConfiguration().getBoolean("disallow_connect_to_auth", true)) {
                event.setCancelled(true);
                proxiedPlayer.sendMessage(ChatMessageType.CHAT, TextComponent
                        .fromLegacyText(plugin.getConfiguration().getString("auth_server_disallowed_message", "")));
            }
            else {
                return;
            }
        }
        if (!isAuthorized) {
            ServerInfo newTarget = plugin.getProxy().getServerInfo(authServerName);
            if (newTarget == null)
                event.setCancelled(true);
            else
                event.setTarget(newTarget);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerServerDisconnectedEvent(ServerKickEvent event) {
        ProxiedPlayer proxiedPlayer = event.getPlayer();
        String authServerName = plugin.getConfiguration().getString("auth_server", "auth");
        String fallbackServerName = plugin.getConfiguration().getString("fallback_server", "lobby");
        ServerInfo serverInfo = event.getKickedFrom();
        if (!serverInfo.getName().equals(authServerName) &&
                !serverInfo.getName().equals(fallbackServerName)) {
            ServerInfo toConnect;
            if (bridgedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
                toConnect = plugin.getProxy().getServerInfo(fallbackServerName);
            }
            else {
                toConnect = plugin.getProxy().getServerInfo(authServerName);
            }
            if (toConnect != null && !event.getCause().equals(ServerKickEvent.Cause.LOST_CONNECTION)) {
                event.setCancelServer(toConnect);
                BaseComponent[] components = event.getKickReasonComponent();
                ArrayList<BaseComponent> newComponents = new ArrayList<>();
                newComponents.addAll(Arrays.stream(TextComponent.fromLegacyText(plugin.getConfiguration().getString("kick_reason", ""))).toList());
                newComponents.addAll(Arrays.stream(components).toList());
                proxiedPlayer.sendMessage(ChatMessageType.CHAT, newComponents.toArray(components));
                event.setCancelled(true);
            }
        }
    }



    private boolean isAllowedString(String string) {
        return isAllowedCommand(string) || isAllowedMessage(string);
    }

    private boolean isAllowedMessage(String message) {
        return message.matches("^-?\\d+$");
    }

    private boolean isAllowedCommand(String command) {
        if (command.startsWith("/login ")) {
            return true;
        }
        if (command.startsWith("/l ")) {
            return true;
        }
        if (command.startsWith("/reg ")) {
            return true;
        }
        if (command.startsWith("/register ")) {
            return true;
        }
        if (command.startsWith("/menu")) {
            return true;
        }
        return command.startsWith("/bauth-antibotclick-");
    }
}
