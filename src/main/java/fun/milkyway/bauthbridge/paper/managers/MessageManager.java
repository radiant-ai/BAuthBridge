package fun.milkyway.bauthbridge.paper.managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fun.milkyway.bauthbridge.common.Message;
import fun.milkyway.bauthbridge.common.pojo.MessageOptions;
import fun.milkyway.bauthbridge.common.utils.Utils;
import org.bukkit.plugin.Plugin;

import java.io.*;

public class MessageManager {
    private final Plugin plugin;
    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
    }
    public void sendMessage(Message message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(MessageOptions.CHANNELNAME);

        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);

        try {
            msgout.writeUTF(message.asString());
        } catch (IOException exception){
            Utils.exceptionWarningIntoLogger(plugin.getLogger(), exception);
            return;
        }

        out.writeShort(msgbytes.toByteArray().length);
        out.write(msgbytes.toByteArray());

        plugin.getServer().sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}
