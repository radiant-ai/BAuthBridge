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
import net.md_5.bungee.event.EventPriority;

public class AuthorizationListener implements Listener {
    private final BAuthBridgeWaterfall plugin;
    private final Configuration configuration;
    private final BridgedPlayerManager bridgedPlayerManager;
    public AuthorizationListener(BAuthBridgeWaterfall plugin, BridgedPlayerManager bridgedPlayerManager) {
        this.plugin = plugin;
        this.bridgedPlayerManager = bridgedPlayerManager;
        this.configuration = plugin.getConfiguration();
    }
    @EventHandler(priority = EventPriority.LOW)
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getSender() instanceof Server && event.getTag().equals("BungeeCord")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            if (!in.readUTF().equals("Forward")) {
                return;
            }
            if (!in.readUTF().equals("BungeeCord")) {
                return;
            }
            if (!in.readUTF().equals(MessageOptions.CHANNELNAME)) {
                return;
            }

            readStreamAndAct(in);
            event.setCancelled(true); //don't let anything else to handle this message
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
        if (!serverName.equals(plugin.getConfiguration().getString("auth_server", "auth"))) {
            bridgedPlayerManager.setPreviousServer(event.getPlayer().getUniqueId(), serverName);
        }
    }
    
    private void readStreamAndAct(ByteArrayDataInput in) {
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
            return;
        }

        //authorize player on LOGIN and REGISTER, but only if not yet authorized
        if ((message.getAction().equals(AuthorizationMessage.Action.LOGIN)
                || message.getAction().equals(AuthorizationMessage.Action.REGISTER))
                && !bridgedPlayerManager.isAuthorized(message.getPlayerUUID())) {
            connectToPreviousServer(bridgedPlayerManager.authorizePlayer(message.getPlayerUUID()));
        }
        //unauthorize player on PREREGISTER, PRELOGIN and LOGOUT
        else {
            bridgedPlayerManager.unauthorizePlayer(message.getPlayerUUID());
        }
    }

    private void connectToPreviousServer(BridgedPlayer bridgedPlayer) {
        if (!bridgedPlayer.isAuthorized()) {
            return;
        }
        ServerInfo fallBackServer = plugin.getProxy()
                .getServerInfo(configuration.getString("fallback_server", "lobby"));
        ProxiedPlayer proxiedPlayer = plugin.getProxy().getPlayer(bridgedPlayer.getUuid());
        if (proxiedPlayer != null && proxiedPlayer.isConnected()) {

            ServerInfo targetServer;
            if (bridgedPlayer.getPreviousServer() != null) {
                //use player's previous server
                targetServer = plugin.getProxy().getServerInfo(bridgedPlayer.getPreviousServer());
            }
            else {
                //if there is none, use the fallback (lobby) server
                targetServer = fallBackServer;
            }

            if (targetServer != null || fallBackServer != null) {
                proxiedPlayer.connect(targetServer, (result, error) -> {
                    if (!result && proxiedPlayer.isConnected()) {

                        //if the fallback server was not found
                        if (fallBackServer == null) {
                            plugin.getLogger().warning("Tried to connect player "+bridgedPlayer.getUuid()+" to fallback server, but it was null!");
                            plugin.getProxy().getPlayer(bridgedPlayer.getUuid())
                                    .disconnect(TextComponent.fromLegacyText(plugin
                                            .getConfiguration().getString("server_unavailable_message", "")));
                            return;
                        }

                        //if it is the same server, add some delay to the retry
                        if (targetServer.getName().equals(fallBackServer.getName())) {
                            plugin.getLogger().warning("Retrying to connect player "+bridgedPlayer.getUuid()+" to the fallback server...");
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                plugin.getLogger().warning("Thread waiting for connection retry for player "+bridgedPlayer.getUuid()+" was interrupted!");
                                Utils.exceptionWarningIntoLogger(plugin.getLogger(), e);
                                return;
                            }
                        }

                        proxiedPlayer.connect(fallBackServer, (result2, error2) -> {
                            if (!result2 && proxiedPlayer.isConnected()) {
                                plugin.getLogger().warning("Tried to connect player "+bridgedPlayer.getUuid()+" to the fallback server, but was not ables to do so!");
                                plugin.getProxy().getPlayer(bridgedPlayer.getUuid())
                                        .disconnect(TextComponent.fromLegacyText(plugin
                                                .getConfiguration().getString("server_unavailable_message", "")));
                            }
                            else if (result2 && proxiedPlayer.isConnected()) {
                                plugin.getProxy().getPlayer(bridgedPlayer.getUuid())
                                        .sendMessage(TextComponent.fromLegacyText(plugin
                                                .getConfiguration().getString("server_previous_not_available", "")));
                                //fix BAuth lagged out title msg
                                plugin.getProxy().getPlayer(bridgedPlayer.getUuid())
                                        .sendTitle(plugin.getProxy().createTitle().reset());
                            }
                        }, false, 2000);
                    }
                    else if (result && proxiedPlayer.isConnected()) {
                        plugin.getProxy().getPlayer(bridgedPlayer.getUuid())
                                .sendMessage(TextComponent.fromLegacyText(plugin
                                        .getConfiguration().getString("server_connect_previous", "")+ChatColor.GRAY+targetServer.getName()));
                        //fix BAuth lagged out title msg
                        plugin.getProxy().getPlayer(bridgedPlayer.getUuid())
                                .sendTitle(plugin.getProxy().createTitle().reset());
                    }
                }, false, 2000);
            }
            else {
                plugin.getLogger().warning("Tried to connect player "+bridgedPlayer.getUuid()+" to any of the servers, but both were null!");
                plugin.getProxy().getPlayer(bridgedPlayer.getUuid())
                        .disconnect(TextComponent.fromLegacyText(plugin
                                .getConfiguration().getString("server_unavailable_message", "")));
            }
        }
    }
}
