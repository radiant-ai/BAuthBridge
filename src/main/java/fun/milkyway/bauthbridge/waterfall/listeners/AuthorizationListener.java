package fun.milkyway.bauthbridge.waterfall.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fun.milkyway.bauthbridge.common.AuthorizationMessage;
import fun.milkyway.bauthbridge.common.pojo.MessageOptions;
import fun.milkyway.bauthbridge.common.utils.Utils;
import fun.milkyway.bauthbridge.waterfall.BAuthBridgeWaterfall;
import fun.milkyway.bauthbridge.waterfall.managers.BridgedPlayer;
import fun.milkyway.bauthbridge.waterfall.managers.BridgedPlayerManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

public class AuthorizationListener implements Listener {
    private final BAuthBridgeWaterfall plugin;
    private final Configuration configuration;
    private final BridgedPlayerManager bridgedPlayerManager;
    public AuthorizationListener(BAuthBridgeWaterfall plugin, BridgedPlayerManager bridgedPlayerManager) {
        this.plugin = plugin;
        this.bridgedPlayerManager = bridgedPlayerManager;
        this.configuration = plugin.getConfiguration();
    }
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getSender() instanceof Server && event.getTag().equals("BungeeCord")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            if (!in.readUTF().equals("Forward")) {
                return;
            }
            if (!in.readUTF().equals("ALL")) {
                return;
            }
            if (!in.readUTF().equals(MessageOptions.CHANNELNAME)) {
                return;
            }

            BridgedPlayer bridgedPlayer = readStreamAndAuthorize(in);

            if (bridgedPlayer != null) {
                connectToPreviousServer(bridgedPlayer);
            }
        }
    }
    
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer proxiedPlayer = event.getPlayer();
        bridgedPlayerManager.unauthorizePlayer(proxiedPlayer.getUniqueId());
    }

    @EventHandler
    public void onServerConnect(ServerConnectedEvent event) {
        String serverName = event.getServer().getInfo().getName();
        if (!serverName.equals(plugin.getConfiguration().getString("auth_server", "auth")) &&
                !serverName.equals(plugin.getConfiguration().getString("fallback_server", "lobby"))) {
            bridgedPlayerManager.setPreviousServer(event.getPlayer().getUniqueId(), serverName);
        }
    }
    
    private BridgedPlayer readStreamAndAuthorize(ByteArrayDataInput in) {
        short dataLength = in.readShort();
        byte[] dataBytes = new byte[dataLength];
        in.readFully(dataBytes);
        ByteArrayDataInput dataIn = ByteStreams.newDataInput(dataBytes);
        String messageString = dataIn.readUTF();

        AuthorizationMessage message;
        try {
            message = new AuthorizationMessage(messageString);
        }
        catch (IllegalArgumentException exception) {
            Utils.exceptionWarningIntoLogger(plugin.getLogger(), exception);
            return null;
        }
        return bridgedPlayerManager.authorizePlayer(message.getPlayerUUID());
    }

    private void connectToPreviousServer(BridgedPlayer bridgedPlayer) {
        ServerInfo fallBackServer = plugin.getProxy()
                .getServerInfo(configuration.getString("fallback_server", "lobby"));
        ProxiedPlayer proxiedPlayer = plugin.getProxy().getPlayer(bridgedPlayer.getUuid());
        if (proxiedPlayer != null && proxiedPlayer.isConnected()) {
            ServerInfo targetServer;
            if (bridgedPlayer.getPreviousServer() != null) {
                targetServer = plugin.getProxy().getServerInfo(bridgedPlayer.getPreviousServer());
            }
            else {
                targetServer = fallBackServer;
            }
            ServerInfo finalTargetServer = targetServer;
            if (finalTargetServer != null) {
                proxiedPlayer.connect(targetServer, (result, error) -> {
                    if (!result && proxiedPlayer.isConnected()) {
                        if (finalTargetServer != fallBackServer && fallBackServer != null) {
                            proxiedPlayer.connect(finalTargetServer, (result2, error2) -> {
                                if (!result2 && proxiedPlayer.isConnected()) {
                                    plugin.getProxy().getPlayer(bridgedPlayer.getUuid())
                                            .disconnect(TextComponent.fromLegacyText("Сервера недоступны!", ChatColor.RED));
                                }
                            }, false, 2000);
                        }
                        else {
                            plugin.getProxy().getPlayer(bridgedPlayer.getUuid())
                                    .disconnect(TextComponent.fromLegacyText("Сервера недоступны!", ChatColor.RED));
                        }
                    }
                }, false, 2000);
            }
            else {
                plugin.getProxy().getPlayer(bridgedPlayer.getUuid())
                        .disconnect(TextComponent.fromLegacyText("Сервера недоступны!", ChatColor.RED));
            }
        }
    }
}
