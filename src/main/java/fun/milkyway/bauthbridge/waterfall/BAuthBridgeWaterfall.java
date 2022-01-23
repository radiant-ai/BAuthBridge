package fun.milkyway.bauthbridge.waterfall;

import fun.milkyway.bauthbridge.common.utils.Utils;
import fun.milkyway.bauthbridge.waterfall.listeners.AuthorizationListener;
import fun.milkyway.bauthbridge.waterfall.listeners.SecurityListener;
import fun.milkyway.bauthbridge.waterfall.managers.BridgedPlayerManager;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;

public class BAuthBridgeWaterfall extends Plugin {
    private BridgedPlayerManager bridgedPlayerManager;
    private Configuration configuration;
    @Override
    public void onEnable()
    {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists())
            dataFolder.mkdir();
        InputStream inputStream = getResourceAsStream("config.yml");
        File configFile = new File(dataFolder, "config.yml");
        try {
            if (!configFile.exists()) {
                OutputStream outputStream = new FileOutputStream(configFile);
                inputStream.transferTo(outputStream);
                outputStream.close();
                inputStream.close();
            }
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException exception) {
            Utils.exceptionWarningIntoLogger(getLogger(), exception);
        }
        bridgedPlayerManager = new BridgedPlayerManager();
        getProxy().getPluginManager().registerListener(this, new AuthorizationListener(this, bridgedPlayerManager));
        getProxy().getPluginManager().registerListener(this, new SecurityListener(this, bridgedPlayerManager));
    }

    @Override
    public void onDisable()
    {
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
