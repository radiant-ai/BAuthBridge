package fun.milkyway.bauthbridge.waterfall.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fun.milkyway.bauthbridge.common.AuthorizationMessage;
import fun.milkyway.bauthbridge.common.pojo.MessageOptions;
import fun.milkyway.bauthbridge.common.utils.Utils;
import fun.milkyway.bauthbridge.waterfall.BAuthBridgeWaterfall;
import fun.milkyway.bauthbridge.waterfall.managers.BridgedPlayerManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class AuthorizationListener implements Listener {
    private final BAuthBridgeWaterfall plugin;
    private final Configuration configuration;
    private final BridgedPlayerManager bridgedPlayerManager;
    private final ServerInfo authServer;
    private final ServerInfo fallbackServer;
    private final Map<UUID, Integer> retryMap;
    private final ScheduledTask scheduledRetryTask;
    public AuthorizationListener(BAuthBridgeWaterfall plugin, BridgedPlayerManager bridgedPlayerManager) {
        this.plugin = plugin;
        this.bridgedPlayerManager = bridgedPlayerManager;
        this.configuration = plugin.getConfiguration();
        fallbackServer = plugin.getProxy().getServerInfo(configuration.getString("fallback_server", "lobby"));
        authServer = plugin.getProxy().getServerInfo(configuration.getString("auth_server", "auth"));
        if (fallbackServer == null) {
            throw new IllegalStateException("Fallback server was not found!");
        }
        if (authServer == null) {
            throw new IllegalStateException("Auth server was not found!");
        }
        retryMap = new ConcurrentHashMap<>();
        scheduledRetryTask = retryConnectScheduler();
    }
    @EventHandler(priority = EventPriority.LOW)
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getSender() instanceof Server && event.getTag().equals("BungeeCord")
            && !event.isCancelled()) {
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
        retryMap.remove(proxiedPlayer.getUniqueId());
        bridgedPlayerManager.unauthorizePlayer(proxiedPlayer.getUniqueId());
    }

    @EventHandler
    public void onServerConnect(ServerConnectedEvent event) {
        retryMap.put(event.getPlayer().getUniqueId(), 0);
        String serverName = event.getServer().getInfo().getName();
        if (!serverName.equals(authServer.getName())) {
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

        plugin.getLogger().info(ChatColor.GRAY+ "Received message " +message.getAction().name() + " message for "+message.getPlayerUUID());

        if ((message.getAction().equals(AuthorizationMessage.Action.LOGIN)
                || message.getAction().equals(AuthorizationMessage.Action.REGISTER))) {
            bridgedPlayerManager.authorizePlayer(message.getPlayerUUID());
        }
        //unauthorize player on PREREGISTER, PRELOGIN and LOGOUT
        else {
            bridgedPlayerManager.unauthorizePlayer(message.getPlayerUUID());
        }
    }

    private CompletableFuture<ServerInfo> connectToServer(@NotNull ServerInfo server, @NotNull ProxiedPlayer proxiedPlayer) {
        CompletableFuture<ServerInfo> connectionResult = new CompletableFuture<>();

        if (!proxiedPlayer.isConnected()) {
            plugin.getLogger().warning("Tried to connect player "+proxiedPlayer.getName()+" with closed connection to the server "+server.getName());
            connectionResult.complete(null);
            return connectionResult;
        }

        ServerConnectRequest request = ServerConnectRequest.builder()
                .connectTimeout(configuration.getInt("retry_frequency", 1000)/4)
                .reason(ServerConnectEvent.Reason.PLUGIN)
                .retry(false)
                .target(server)
                .callback((result, error) -> {
                            if (result.equals(ServerConnectRequest.Result.ALREADY_CONNECTED)) {
                                connectionResult.complete(server);
                            }
                            else if (!result.equals(ServerConnectRequest.Result.SUCCESS) || error != null) {
                                plugin.getLogger().warning("Tried to connect player " + proxiedPlayer.getName() + " to the server " + server.getName() + ", but failed: " + result.name());
                                if (error != null) {
                                    plugin.getLogger().warning("Tried to connect player " + proxiedPlayer.getName() + " to the server " + server.getName() + ", but error occurred: " + error.getMessage());
                                }
                                connectionResult.complete(null);
                            }
                            else {
                                connectionResult.complete(server);
                            }
                })
                .sendFeedback(false)
                .build();

        proxiedPlayer.connect(request);

        return connectionResult;
    }

    private ScheduledTask retryConnectScheduler() {
        return plugin.getProxy().getScheduler().schedule(plugin, () -> {
            //plugin.getLogger().info("Scheduler heartbeat with "+authServer.getPlayers()+" players...");
            for (ProxiedPlayer proxiedPlayer : authServer.getPlayers()) {
                //plugin.getLogger().info("Beat for player "+proxiedPlayer.getName());
                try {
                    if (!bridgedPlayerManager.isAuthorized(proxiedPlayer.getUniqueId())) {
                        plugin.getLogger().info("Skipping connect for "+proxiedPlayer.getName()+" because unauthorized!");
                        continue;
                    }

                    //plugin.getLogger().info("Retry count for "+proxiedPlayer.getName()+" is: "+retryMap.get(proxiedPlayer.getUniqueId()));

                    if (retryMap.get(proxiedPlayer.getUniqueId()) >= configuration.getInt("retry_max", 5)) {
                        proxiedPlayer.disconnect(TextComponent.fromLegacyText(configuration.getString("server_unavailable_message", "")));
                        continue;
                    }

                    ServerInfo previousServer = plugin.getProxy().getServerInfo(bridgedPlayerManager.getPreviousServer(proxiedPlayer.getUniqueId()));
                    CompletableFuture<ServerInfo> connectionResult;

                    if (previousServer == null ||
                            retryMap.get(proxiedPlayer.getUniqueId()) >= configuration.getInt("retry_to_prev_count", 5)) {
                        plugin.getLogger().info("Attempted to connect player "+proxiedPlayer.getName()+" to fallback server");
                        connectionResult = connectToServer(fallbackServer, proxiedPlayer);
                        proxiedPlayer.sendMessage(TextComponent.fromLegacyText(
                                configuration.getString("wait_for_connection_fallback", "")));
                    }
                    else  {
                        plugin.getLogger().info("Attempted to connect player "+proxiedPlayer.getName()+" to previous server");
                        connectionResult = connectToServer(previousServer, proxiedPlayer);
                        proxiedPlayer.sendMessage(TextComponent.fromLegacyText(
                                configuration.getString("wait_for_connection_previous", "")));
                    }

                    connectionResult.thenAcceptAsync(result -> {
                        if (result == null) {
                            retryMap.put(proxiedPlayer.getUniqueId(), retryMap.get(proxiedPlayer.getUniqueId())+1);
                            return;
                        }
                        if (result.getName().equals(fallbackServer.getName())) {
                            proxiedPlayer.sendMessage(TextComponent.fromLegacyText(
                                    configuration.getString("server_connect_fallback", "")));
                        }
                        else {
                            proxiedPlayer.sendMessage(TextComponent.fromLegacyText(
                                    configuration.getString("server_connect_previous", "")+ChatColor.GRAY + result.getName()));
                        }
                        retryMap.put(proxiedPlayer.getUniqueId(), 0);
                    });
                }
                catch (Exception exception) {
                    plugin.getLogger().log(Level.WARNING, "Exception for "+proxiedPlayer.getName()+" "+exception.getMessage(), exception);
                }
            }

        }, 1,
                configuration.getInt("retry_frequency", 1000),
                TimeUnit.MILLISECONDS);
    }
}
