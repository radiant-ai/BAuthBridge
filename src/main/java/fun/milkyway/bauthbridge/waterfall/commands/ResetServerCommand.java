package fun.milkyway.bauthbridge.waterfall.commands;

import fun.milkyway.bauthbridge.waterfall.BAuthBridgeWaterfall;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.UUID;

public class ResetServerCommand extends Command {

    private final ServerInfo lobbyServer;

    public ResetServerCommand(String name, ServerInfo serverInfo) {
        super(name);
        lobbyServer = serverInfo;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (sender instanceof ProxiedPlayer player) {
            if (!BAuthBridgeWaterfall.getInstance().getBridgedPlayerManager().isAuthorized(player.getUniqueId())) {
                return;
            }
            if (player.hasPermission("bauthbridge.resetserver")) {
                return;
            }
        }

        if (args.length == 0) {
            return;
        }

        try {
            UUID uuid = UUID.fromString(args[0]);
            BAuthBridgeWaterfall.getInstance().getBridgedPlayerManager().setPreviousServer(uuid, lobbyServer.getName());
            sender.sendMessage(TextComponent.fromLegacyText("§aSuccessfully reset server for player §e" + uuid));
        } catch (IllegalArgumentException ignored) {
        }
    }
}
