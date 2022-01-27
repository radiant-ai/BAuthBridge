package fun.milkyway.bauthbridge.waterfall.managers;

import fun.milkyway.bauthbridge.common.utils.Utils;
import fun.milkyway.bauthbridge.waterfall.BAuthBridgeWaterfall;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class PersistenceManager {
    private BAuthBridgeWaterfall plugin;
    public PersistenceManager(BAuthBridgeWaterfall plugin) {
        this.plugin = plugin;
    }
    public Persistance getPersistence(String fileName) {
        try {
            File file = new File(plugin.getDataFolder(), fileName);
            if (file.exists()) {
                Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                return new Persistance(file, configuration);
            }
            else {
                Configuration configuration = new Configuration();
                return new Persistance(file, configuration);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning(ChatColor.RED+"Failed to create configuration: "+fileName);
            plugin.getLogger().warning(ChatColor.RED+"Some data may be lost after proxy restart!");
            Utils.exceptionWarningIntoLogger(plugin.getLogger(), exception);
        }
        return null;
    }
    public void savePersistence(@NotNull Persistance persistance) {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(persistance.getConfiguration(), persistance.getFile());
        } catch (IOException exception) {
            plugin.getLogger().warning(ChatColor.RED+"Failed to save configuration: "+persistance.getFile().getName());
            plugin.getLogger().warning(ChatColor.RED+"Some data may be lost after proxy restart!");
            Utils.exceptionWarningIntoLogger(plugin.getLogger(), exception);
        }
    }

    public class Persistance {
        private final File file;
        private final Configuration configuration;
        public Persistance(File file, Configuration configuration) {
            this.file = file;
            this.configuration = configuration;
        }

        public File getFile() {
            return file;
        }

        public Configuration getConfiguration() {
            return configuration;
        }
    }
}
