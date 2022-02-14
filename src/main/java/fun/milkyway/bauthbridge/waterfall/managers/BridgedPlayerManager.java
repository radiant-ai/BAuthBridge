package fun.milkyway.bauthbridge.waterfall.managers;

import fun.milkyway.bauthbridge.common.pojo.PersistenceOptions;
import fun.milkyway.bauthbridge.waterfall.BAuthBridgeWaterfall;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BridgedPlayerManager {
    private final BAuthBridgeWaterfall plugin;
    private final PersistenceManager persistenceManager;
    private final Map<UUID, BridgedPlayer> authorizedPlayers;
    public BridgedPlayerManager(BAuthBridgeWaterfall plugin) {
        this.plugin = plugin;
        this.persistenceManager = new PersistenceManager(plugin);
        authorizedPlayers = new ConcurrentHashMap<>();
    }
    public BridgedPlayer authorizePlayer(UUID uuid) {
        plugin.getLogger().info("Authorizing player "+uuid);
        BridgedPlayer bridgedPlayer = authorizedPlayers.get(uuid);
        authorizedPlayers.put(uuid, new BridgedPlayer(uuid, true, bridgedPlayer != null ? bridgedPlayer.getPreviousServer() : null));
        return authorizedPlayers.get(uuid);
    }
    public BridgedPlayer unauthorizePlayer(UUID uuid) {
        plugin.getLogger().info("Unauthorizing player "+uuid);
        BridgedPlayer bridgedPlayer = authorizedPlayers.get(uuid);
        if (bridgedPlayer != null) {
            authorizedPlayers.put(uuid, new BridgedPlayer(uuid, false, bridgedPlayer.getPreviousServer()));
        }
        return authorizedPlayers.get(uuid);
    }
    public boolean isAuthorized(UUID uuid) {
        BridgedPlayer bridgedPlayer = authorizedPlayers.get(uuid);
        if (bridgedPlayer == null) {
            authorizedPlayers.put(uuid, new BridgedPlayer(uuid, false, null));
        }
        return authorizedPlayers.get(uuid).isAuthorized();
    }
    public BridgedPlayer getPlayer(UUID uuid) {
        return authorizedPlayers.get(uuid);
    }
    public BridgedPlayer setPreviousServer(UUID uuid, String previousServer) {
        BridgedPlayer bridgedPlayer = authorizedPlayers.get(uuid);
        if (bridgedPlayer != null) {
            authorizedPlayers.put(uuid, new BridgedPlayer(uuid, bridgedPlayer.isAuthorized, previousServer));
        }
        return authorizedPlayers.get(uuid);
    }
    public String getPreviousServer(UUID uuid) {
        BridgedPlayer bridgedPlayer = authorizedPlayers.get(uuid);
        if (bridgedPlayer != null) {
            return authorizedPlayers.get(uuid).getPreviousServer();
        }
        return null;
    }
    public boolean saveAll(String fileName) {
        PersistenceManager.Persistance persistance = persistenceManager.getPersistence(fileName);
        if (persistance != null) {
            for (Map.Entry<UUID, BridgedPlayer> entry : authorizedPlayers.entrySet()) {
                BridgedPlayer bridgedPlayer = entry.getValue();
                if (bridgedPlayer.getPreviousServer() != null) {
                    persistance.getConfiguration().set(PersistenceOptions.SECTION_NAME+"."+entry.getKey().toString(), bridgedPlayer.getPreviousServer());
                }
            }
            persistenceManager.savePersistence(persistance);
            return true;
        }
        return false;
    }
    public boolean loadAll(String fileName) {
        PersistenceManager.Persistance persistence = persistenceManager.getPersistence(fileName);
        if (persistence != null) {
            for (String key : persistence.getConfiguration().getSection(PersistenceOptions.SECTION_NAME).getKeys()) {
                UUID uuid = UUID.fromString(key);
                authorizedPlayers.put(uuid, new BridgedPlayer(
                        uuid,
                        false,
                        persistence.getConfiguration().getString(PersistenceOptions.SECTION_NAME+"."+uuid))
                );
            }
            return true;
        }
        return false;
    }

    public class BridgedPlayer {
        private final UUID uuid;
        private final boolean isAuthorized;
        private final String previousServer;
        public BridgedPlayer(UUID uuid, boolean isAuthorized, String previousServer) {
            this.uuid = uuid;
            this.isAuthorized = isAuthorized;
            this.previousServer = previousServer;
        }

        private UUID getUuid() {
            return uuid;
        }

        private boolean isAuthorized() {
            return isAuthorized;
        }

        private String getPreviousServer() {
            return previousServer;
        }
    }
}
