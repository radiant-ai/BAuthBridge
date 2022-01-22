package fun.milkyway.bauthbridge.paper;

import fun.milkyway.bauthbridge.paper.listeners.AuthorizationListener;
import fun.milkyway.bauthbridge.paper.managers.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BAuthBridgePaper extends JavaPlugin {
    private MessageManager messageManager;
    @Override
    public void onEnable() {
        messageManager = new MessageManager(this);
        getServer().getPluginManager().registerEvents(new AuthorizationListener(messageManager), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
