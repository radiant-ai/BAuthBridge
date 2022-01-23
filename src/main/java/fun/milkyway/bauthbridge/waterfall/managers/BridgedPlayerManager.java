package fun.milkyway.bauthbridge.waterfall.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BridgedPlayerManager {
    private final Map<UUID, BridgedPlayer> authorizedPlayers;
    public BridgedPlayerManager() {
        authorizedPlayers = new HashMap<>();
    }
    public BridgedPlayer authorizePlayer(UUID uuid) {
        BridgedPlayer bridgedPlayer = authorizedPlayers.get(uuid);
        authorizedPlayers.put(uuid, new BridgedPlayer(uuid, true, bridgedPlayer != null ? bridgedPlayer.getPreviousServer() : null));
        return authorizedPlayers.get(uuid);
    }
    public BridgedPlayer unauthorizePlayer(UUID uuid) {
        BridgedPlayer bridgedPlayer = authorizedPlayers.get(uuid);
        if (bridgedPlayer != null) {
            authorizedPlayers.put(uuid, new BridgedPlayer(uuid, false, bridgedPlayer.getPreviousServer()));
        }
        return authorizedPlayers.get(uuid);
    }
    public boolean isAuthorized(UUID uuid) {
        BridgedPlayer bridgedPlayer = authorizedPlayers.get(uuid);
        return bridgedPlayer != null && authorizedPlayers.get(uuid).isAuthorized();
    }
    public BridgedPlayer getAuthorizedPlayer(UUID uuid) {
        return authorizedPlayers.get(uuid);
    }
    public BridgedPlayer setPreviousServer(UUID uuid, String previousServer) {
        BridgedPlayer bridgedPlayer = authorizedPlayers.get(uuid);
        if (bridgedPlayer != null && bridgedPlayer.isAuthorized()) {
            authorizedPlayers.put(uuid, new BridgedPlayer(uuid, true, previousServer));
        }
        return authorizedPlayers.get(uuid);
    }
}
