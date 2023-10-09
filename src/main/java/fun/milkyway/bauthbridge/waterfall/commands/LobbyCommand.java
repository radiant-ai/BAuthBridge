package fun.milkyway.bauthbridge.waterfall.commands;

import fun.milkyway.bauthbridge.waterfall.BAuthBridgeWaterfall;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class LobbyCommand extends Command {
    private final ServerInfo lobbyServer;
    public LobbyCommand(String name, ServerInfo serverInfo) {
        super(name);
        lobbyServer = serverInfo;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            return;
        }
        if (!BAuthBridgeWaterfall.getInstance().getBridgedPlayerManager().isAuthorized(player.getUniqueId())) {
            return;
        }
        if (player.hasPermission("bauthbridge.lobby")) {
            return;
        }
        player.connect(lobbyServer);
    }
}
