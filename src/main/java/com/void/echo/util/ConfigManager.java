package com.void.echo.util;

import com.void.echo.EchoPlugin;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages ECHO configuration
 */
public class ConfigManager {
    
    private final EchoPlugin plugin;
    
    @Getter
    private FileConfiguration config;
    
    public ConfigManager(EchoPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load configuration from disk
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    /**
     * Get a string from config
     */
    public String getString(String path, String def) {
        return config.getString(path, def);
    }
    
    /**
     * Get an integer from config
     */
    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }
    
    /**
     * Get a double from config
     */
    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }
    
    /**
     * Get a boolean from config
     */
    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }
    
    /**
     * Get a long from config
     */
    public long getLong(String path, long def) {
        return config.getLong(path, def);
    }
}
