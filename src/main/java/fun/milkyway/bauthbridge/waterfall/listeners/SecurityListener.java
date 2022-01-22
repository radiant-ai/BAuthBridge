package fun.milkyway.bauthbridge.waterfall.listeners;

import fun.milkyway.bauthbridge.waterfall.managers.AuthorizedPlayerManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.event.TabCompleteResponseEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Iterator;

public class SecurityListener implements Listener {
    private Plugin plugin;
    private AuthorizedPlayerManager authorizedPlayerManager;
    public SecurityListener(Plugin plugin, AuthorizedPlayerManager authorizedPlayerManager) {
        this.plugin = plugin;
        this.authorizedPlayerManager = authorizedPlayerManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(ChatEvent event) {
        Connection connection = event.getSender();
        if (connection instanceof ProxiedPlayer) {
            ProxiedPlayer proxiedPlayer = (ProxiedPlayer) connection;
            if (!authorizedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
                if (!isAllowedString(event.getMessage())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPermissionCheck(PermissionCheckEvent event) {
        CommandSender commandSender = event.getSender();
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer proxiedPlayer = (ProxiedPlayer) commandSender;
            if (!authorizedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
                event.setHasPermission(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabcompletePre(TabCompleteEvent event) {
        if (event.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer proxiedPlayer = (ProxiedPlayer) event.getSender();
            if (!authorizedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
                event.getSuggestions().clear();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabcompletePost(TabCompleteResponseEvent event) {
        Connection connection = event.getSender();
        if (connection instanceof ProxiedPlayer) {
            ProxiedPlayer proxiedPlayer = (ProxiedPlayer) connection;
            if (!authorizedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
                Iterator<String> iterator = event.getSuggestions().iterator();
                while (iterator.hasNext()) {
                    String suggestion = iterator.next();
                    if (!isAllowedCommand(suggestion)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private boolean isAllowedString(String string) {
        return isAllowedCommand(string) || isAllowedMessage(string);
    }

    private boolean isAllowedMessage(String message) {
        if (message.matches("^-?\\d+$")) {
            return true;
        }
        return false;
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
        if (command.startsWith("/bauth-antibotclick-")) {
            return true;
        }
        return false;
    }
}
