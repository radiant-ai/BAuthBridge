package fun.milkyway.bauthbridge.waterfall.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthorizedPlayerManager {
    private final Map<UUID, AuthorizedPlayer> authorizedPlayers;
    public AuthorizedPlayerManager() {
        authorizedPlayers = new HashMap<>();
    }
    public void authorizePlayer(UUID uuid) {
        authorizedPlayers.put(uuid, new AuthorizedPlayer(uuid));
    }
    public void unauthorizePlayer(UUID uuid) {
        authorizedPlayers.remove(uuid);
    }
    public boolean isAuthorized(UUID uuid) {
        return authorizedPlayers.get(uuid) != null;
    }
}
