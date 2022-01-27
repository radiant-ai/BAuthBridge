package fun.milkyway.bauthbridge.waterfall;

import fun.milkyway.bauthbridge.common.pojo.PersistenceOptions;
import fun.milkyway.bauthbridge.common.utils.Utils;
import fun.milkyway.bauthbridge.waterfall.listeners.AuthorizationListener;
import fun.milkyway.bauthbridge.waterfall.listeners.SecurityListener;
import fun.milkyway.bauthbridge.waterfall.managers.BridgedPlayerManager;
import net.md_5.bungee.api.ChatColor;
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
        if (setupConfig()) {
            getLogger().info(ChatColor.DARK_GREEN+"Loaded config successfully!");

            String authServerName = configuration.getString("auth_server", "auth");
            if (getProxy().getServerInfo(authServerName) != null)
                getLogger().info(ChatColor.DARK_GREEN+"Successfully added server "+ChatColor.YELLOW+
                        authServerName+ChatColor.DARK_GREEN+" as the auth server!");

            String fallbackServerName = configuration.getString("fallback_server", "lobby");
            if (getProxy().getServerInfo(fallbackServerName) != null)
                getLogger().info(ChatColor.DARK_GREEN+"Successfully added server "+ChatColor.YELLOW+
                        fallbackServerName+ChatColor.DARK_GREEN+" as the lobby/fallback server!");

            bridgedPlayerManager = new BridgedPlayerManager(this);
            if (bridgedPlayerManager.loadAll(PersistenceOptions.FILE_NAME)) {
                getLogger().info(ChatColor.DARK_GREEN+"Loaded all players' data!");
            }
            else {
                getLogger().info(ChatColor.DARK_RED+"Failed to load players' data!");
            }
            getLogger().info(ChatColor.DARK_GREEN+"Initialized player manager!");

            setupListeners();
        }
        else {
            getLogger().info(ChatColor.DARK_RED+"Failed to load config!");
            getProxy().stop(ChatColor.DARK_RED+"Authorization bridge failed, shutting down!");
        }
    }

    @Override
    public void onDisable()
    {
        if (bridgedPlayerManager.saveAll(PersistenceOptions.FILE_NAME)) {
            getLogger().info(ChatColor.DARK_GREEN+"Saved all players!");
        }
        else {
            getLogger().info(ChatColor.DARK_RED+"Failed to save players!");
        }

        getProxy().getPluginManager().unregisterListeners(this);
        getLogger().info(ChatColor.DARK_GREEN+"Unregistered all listeners!");
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    private boolean setupConfig() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists())
            dataFolder.mkdir();
        InputStream inputStream = getResourceAsStream("config_bungee.yml");
        File configFile = new File(dataFolder, "config.yml");
        try {
            if (!configFile.exists()) {
                configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(inputStream);
                inputStream.close();
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, configFile);
                getLogger().info(ChatColor.GREEN+"Saved default config file config.yml!");
            }
            else {
                configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
                Configuration newConfiguration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(inputStream);
                ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
                boolean copyNew = false;
                for (String key : newConfiguration.getKeys()) {
                    Object obj = configuration.get(key);
                    if (obj == null) {
                        configuration.set(key, newConfiguration.get(key));
                        copyNew = true;
                    }
                }
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, configFile);
                inputStream.close();
                if (copyNew) {
                    getLogger().info(ChatColor.GREEN+"Copied new config values into the config.yml!");
                }
            }
        } catch (IOException exception) {
            Utils.exceptionWarningIntoLogger(getLogger(), exception);
            return false;
        }
        return true;
    }


    private void setupListeners() {
        getProxy().getPluginManager().registerListener(this, new AuthorizationListener(this, bridgedPlayerManager));
        getLogger().info(ChatColor.DARK_GREEN+"Registered authorization listener!");
        getProxy().getPluginManager().registerListener(this, new SecurityListener(this, bridgedPlayerManager));
        getLogger().info(ChatColor.DARK_GREEN+"Registered security listener!");
    }
}
