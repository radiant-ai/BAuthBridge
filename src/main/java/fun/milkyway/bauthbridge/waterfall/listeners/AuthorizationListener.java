package fun.milkyway.bauthbridge.waterfall.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fun.milkyway.bauthbridge.common.AuthorizationMessage;
import fun.milkyway.bauthbridge.common.pojo.MessageOptions;
import fun.milkyway.bauthbridge.common.utils.Utils;
import fun.milkyway.bauthbridge.waterfall.BAuthBridgeWaterfall;
import fun.milkyway.bauthbridge.waterfall.managers.BridgedPlayerManager;
import io.github.waterfallmc.waterfall.event.ProxyExceptionEvent;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AuthorizationListener implements Listener {
    private final BAuthBridgeWaterfall plugin;
    private final Configuration configuration;
    private final BridgedPlayerManager bridgedPlayerManager;
    private final ServerInfo fallbackServer;
    private final Map<UUID, RetryWrapper> retryMap;
    public AuthorizationListener(BAuthBridgeWaterfall plugin, BridgedPlayerManager bridgedPlayerManager) {
        this.plugin = plugin;
        this.bridgedPlayerManager = bridgedPlayerManager;
        this.configuration = plugin.getConfiguration();
        fallbackServer = plugin.getProxy().getServerInfo(configuration.getString("fallback_server", "lobby"));
        if (fallbackServer == null) {
            throw new IllegalStateException("Fallback server was not found!");
        }
        retryMap = new ConcurrentHashMap<>();
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
        RetryWrapper retryWrapper = retryMap.get(proxiedPlayer.getUniqueId());
        if (retryWrapper != null) {
            retryWrapper.scheduledTask.cancel();
        }
        retryMap.remove(proxiedPlayer.getUniqueId());
    }

    @EventHandler
    public void onServerConnect(ServerConnectedEvent event) {
        String serverName = event.getServer().getInfo().getName();
        if (!serverName.equals(configuration.getString("auth_server", "auth"))) {
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
            bridgedPlayerManager.authorizePlayer(message.getPlayerUUID());
            retryConnect(message.getPlayerUUID());
        }
        //unauthorize player on PREREGISTER, PRELOGIN and LOGOUT
        else {
            bridgedPlayerManager.unauthorizePlayer(message.getPlayerUUID());
        }
    }

    private CompletableFuture<ServerInfo> connectToServer(ServerInfo server, ProxiedPlayer proxiedPlayer) {
        CompletableFuture<ServerInfo> connectionResult = new CompletableFuture<>();
        if (server == null) {
            plugin.getLogger().warning("Tried to connect player "+proxiedPlayer.getName()+" to the NULL server!");
            connectionResult.complete(null);
            return connectionResult;
        }

        if (proxiedPlayer == null || !proxiedPlayer.isConnected()) {
            plugin.getLogger().warning("Tried to connect NULL player "+proxiedPlayer.getName()+" to the server "+server.getName());
            connectionResult.complete(null);
            return connectionResult;
        }

        if (!proxiedPlayer.isConnected()) {
            plugin.getLogger().warning("Tried to connect player "+proxiedPlayer.getName()+" with closed connection to the server "+server.getName());
            connectionResult.complete(null);
            return connectionResult;
        }

        proxiedPlayer.connect(server, (result, error) -> {
            if (!result || error != null) {
                plugin.getLogger().warning("Tried to connect player "+proxiedPlayer.getName()+" to the server " + server.getName() + ", but failed "+(error != null ? error.getMessage() : ""));
                connectionResult.complete(null);
            }
            else {
                connectionResult.complete(server);
            }
        }, false, configuration.getInt("retry_frequency", 1000));

        return connectionResult;
    }

    private void retryConnect(UUID uuid) {
        ScheduledTask scheduledTask = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            BridgedPlayerManager.BridgedPlayer bridgedPlayer = bridgedPlayerManager.getPlayer(uuid);
            if (bridgedPlayer != null && bridgedPlayer.isAuthorized()) {
                ServerInfo previousServer = bridgedPlayer.getPreviousServerInfo();
                ProxiedPlayer proxiedPlayer = plugin.getProxy().getPlayer(uuid);
                CompletableFuture<ServerInfo> connectionResult;

                if (fallbackServer != null) {
                    if (previousServer == null ||
                            retryMap.get(uuid).attempts > configuration.getInt("retry_to_prev_count", 3)) {
                        connectionResult = connectToServer(fallbackServer, proxiedPlayer);
                        plugin.getLogger().info("Attempted to connect player "+plugin.getProxy().getPlayer(uuid).getName()+" to fallback server");
                        plugin.getProxy().getPlayer(uuid)
                                .sendMessage(TextComponent.fromLegacyText(
                                        configuration.getString("wait_for_connection_fallback", "")));
                    }
                    else {
                        connectionResult = connectToServer(previousServer, proxiedPlayer);
                        plugin.getLogger().info("Attempted to connect player "+plugin.getProxy().getPlayer(uuid).getName()+" to previous server");
                        plugin.getProxy().getPlayer(uuid)
                                .sendMessage(TextComponent.fromLegacyText(
                                        configuration.getString("wait_for_connection_previous", "")));
                    }

                    connectionResult.thenAccept(result -> {
                        if (result == null) {
                            return;
                        }
                        if (result.getName().equals(configuration.getString("fallback_server", "lobby"))) {
                            plugin.getProxy().getPlayer(uuid)
                                    .sendMessage(TextComponent.fromLegacyText(
                                            configuration.getString("server_connect_fallback", "")));
                        }
                        else {
                            plugin.getProxy().getPlayer(uuid)
                                    .sendMessage(TextComponent.fromLegacyText(
                                            configuration.getString("server_connect_previous", "")+ChatColor.GRAY + result.getName()));
                        }
                        retryMap.get(uuid).scheduledTask.cancel();
                    });
                    retryMap.get(uuid).attempts++;
                }
                else {
                    retryMap.get(uuid).scheduledTask.cancel();
                }

            }
        }, configuration.getInt("retry_frequency", 1000)/10,
                configuration.getInt("retry_frequency", 1000),
                TimeUnit.MILLISECONDS);
        retryMap.put(uuid, new RetryWrapper(scheduledTask));
    }

    private class RetryWrapper {
        public final ScheduledTask scheduledTask;
        public int attempts;
        public RetryWrapper(ScheduledTask scheduledTask) {
            this.attempts = 0;
            this.scheduledTask = scheduledTask;
        }
    }
}
