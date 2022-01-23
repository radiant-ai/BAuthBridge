package fun.milkyway.bauthbridge.waterfall.managers;

import java.util.UUID;

public class BridgedPlayer {
    private final UUID uuid;
    private final boolean isAuthorized;
    private final String previousServer;
    public BridgedPlayer(UUID uuid, boolean isAuthorized, String previousServer) {
        this.uuid = uuid;
        this.isAuthorized = isAuthorized;
        this.previousServer = previousServer;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public String getPreviousServer() {
        return previousServer;
    }
}
