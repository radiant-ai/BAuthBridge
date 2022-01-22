package fun.milkyway.bauthbridge.paper.managers;

import fun.milkyway.bauthbridge.common.Message;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;

public class MessageManager {
    public static final String CHANNEL = "BAuthBridgeChannel";
    private final Plugin plugin;
    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }
    public void sendMessage(Message message) {
        plugin.getServer().sendPluginMessage(plugin, CHANNEL, message.asString().getBytes(StandardCharsets.UTF_8));
    }
}
