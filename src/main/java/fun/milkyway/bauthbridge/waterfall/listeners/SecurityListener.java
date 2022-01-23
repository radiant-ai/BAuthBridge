package fun.milkyway.bauthbridge.waterfall.listeners;

import fun.milkyway.bauthbridge.waterfall.BAuthBridgeWaterfall;
import fun.milkyway.bauthbridge.waterfall.managers.BridgedPlayerManager;
import io.github.waterfallmc.waterfall.event.ProxyDefineCommandsEvent;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

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
    public void onPermissionCheck(PermissionCheckEvent event) {
        CommandSender commandSender = event.getSender();
        if (commandSender instanceof ProxiedPlayer proxiedPlayer) {
            if (!bridgedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
                event.setHasPermission(false);
            }
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
        if (event.getTarget().getName().equals(authServerName)) {
            return;
        }
        if (!bridgedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
            ServerInfo newTarget = plugin.getProxy().getServerInfo(authServerName);
            if (newTarget == null)
                event.setCancelled(true);
            else
                event.setTarget(newTarget);
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
        return command.startsWith("/bauth-antibotclick-");
    }
}
