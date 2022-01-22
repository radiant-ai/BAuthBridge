package fun.milkyway.bauthbridge.waterfall.listeners;

import fun.milkyway.bauthbridge.common.AuthorizationMessage;
import fun.milkyway.bauthbridge.waterfall.managers.AuthorizedPlayerManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class AuthorizationListener implements Listener {
    private final Plugin plugin;
    private final AuthorizedPlayerManager authorizedPlayerManager;
    public AuthorizationListener(Plugin plugin, AuthorizedPlayerManager authorizedPlayerManager) {
        this.plugin = plugin;
        this.authorizedPlayerManager = authorizedPlayerManager;
    }
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getSender() instanceof Server) {
            String messageString = new String(event.getData(), StandardCharsets.UTF_8);
            AuthorizationMessage message;
            try {
                message = new AuthorizationMessage(messageString);
            }
            catch (IllegalArgumentException exception) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                exception.printStackTrace(pw);
                plugin.getLogger().warning(sw.toString());
                return;
            }
            authorizedPlayerManager.authorizePlayer(message.getPlayerUUID());
        }
    }
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer proxiedPlayer = event.getPlayer();
        authorizedPlayerManager.unauthorizePlayer(proxiedPlayer.getUniqueId());
    }
}
