package fun.milkyway.bauthbridge.waterfall;

import fun.milkyway.bauthbridge.waterfall.listeners.AuthorizationListener;
import fun.milkyway.bauthbridge.waterfall.listeners.SecurityListener;
import fun.milkyway.bauthbridge.waterfall.managers.AuthorizedPlayerManager;
import net.md_5.bungee.api.plugin.Plugin;

public class BAuthBridgeWaterfall extends Plugin {
    private AuthorizedPlayerManager authorizedPlayerManager;
    @Override
    public void onEnable()
    {
        authorizedPlayerManager = new AuthorizedPlayerManager();
        getProxy().getPluginManager().registerListener(this, new AuthorizationListener(this, authorizedPlayerManager));
        getProxy().getPluginManager().registerListener(this, new SecurityListener(this, authorizedPlayerManager));
    }

    @Override
    public void onDisable()
    {
    }
}
