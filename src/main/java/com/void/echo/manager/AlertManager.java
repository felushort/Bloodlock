package com.void.echo.manager;

import com.void.echo.EchoPlugin;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages alert subscriptions for staff members
 */
public class AlertManager {
    
    private final EchoPlugin plugin;
    
    // Players who want to receive alerts
    private final Set<UUID> alertSubscribers = ConcurrentHashMap.newKeySet();
    
    // Verbose mode (detailed alerts)
    private final Set<UUID> verboseMode = ConcurrentHashMap.newKeySet();
    
    // Alert level per player (1-3, higher = more detailed)
    private final Map<UUID, Integer> alertLevels = new ConcurrentHashMap<>();
    
    public AlertManager(EchoPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if player should receive alerts
     */
    public boolean shouldReceiveAlerts(Player player) {
        if (!player.hasPermission("echo.alerts")) return false;
        return alertSubscribers.contains(player.getUniqueId());
    }
    
    /**
     * Toggle alerts for a player
     */
    public boolean toggleAlerts(UUID playerId) {
        if (alertSubscribers.contains(playerId)) {
            alertSubscribers.remove(playerId);
            return false;
        } else {
            alertSubscribers.add(playerId);
            return true;
        }
    }
    
    /**
     * Enable alerts for a player
     */
    public void enableAlerts(UUID playerId) {
        alertSubscribers.add(playerId);
    }
    
    /**
     * Disable alerts for a player
     */
    public void disableAlerts(UUID playerId) {
        alertSubscribers.remove(playerId);
    }
    
    /**
     * Check if player has verbose mode enabled
     */
    public boolean isVerbose(UUID playerId) {
        return verboseMode.contains(playerId);
    }
    
    /**
     * Toggle verbose mode for a player
     */
    public boolean toggleVerbose(UUID playerId) {
        if (verboseMode.contains(playerId)) {
            verboseMode.remove(playerId);
            return false;
        } else {
            verboseMode.add(playerId);
            return true;
        }
    }
    
    /**
     * Get alert level for a player (1-3)
     */
    public int getAlertLevel(UUID playerId) {
        return alertLevels.getOrDefault(playerId, 2); // Default: level 2
    }
    
    /**
     * Set alert level for a player
     */
    public void setAlertLevel(UUID playerId, int level) {
        if (level < 1) level = 1;
        if (level > 3) level = 3;
        alertLevels.put(playerId, level);
    }
    
    /**
     * Send alert to all subscribers
     */
    public void sendAlert(String message, int minLevel) {
        for (UUID uuid : alertSubscribers) {
            if (getAlertLevel(uuid) >= minLevel) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }
    }
    
    /**
     * Send verbose message to players with verbose mode
     */
    public void sendVerbose(String message) {
        for (UUID uuid : verboseMode) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage("§7[VERBOSE] §f" + message);
            }
        }
    }
    
    /**
     * Clean up on player disconnect
     */
    public void cleanup(UUID playerId) {
        // Keep subscriptions across sessions
        // Only remove from verbose mode
        verboseMode.remove(playerId);
    }
    
    /**
     * Get all alert subscribers
     */
    public Set<UUID> getSubscribers() {
        return Set.copyOf(alertSubscribers);
    }
}
