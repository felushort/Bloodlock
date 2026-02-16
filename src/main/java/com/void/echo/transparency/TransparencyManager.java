package com.void.echo.transparency;

import com.void.echo.EchoPlugin;
import com.void.echo.data.PlayerDataManager;
import com.void.echo.data.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Layer 9: Public Transparency Mode
 * 
 * Shows players their own integrity scores.
 * 
 * Benefits:
 * - Clean players feel proud
 * - Suspicious players feel pressure
 * - Creates psychological deterrence
 * - Maintains operational security (doesn't reveal exact detection methods)
 * 
 * Display format: "Integrity Score: 97/100"
 */
public class TransparencyManager {
    
    private final EchoPlugin plugin;
    private final PlayerDataManager dataManager;
    
    // Configuration
    private final boolean enabled;
    private final long updateIntervalSeconds;
    private final boolean useActionBar;
    
    // Update task
    private BukkitTask updateTask;
    
    public TransparencyManager(EchoPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        
        // Load configuration
        this.enabled = plugin.getConfigManager().getBoolean("transparency.show-scores", true);
        this.updateIntervalSeconds = plugin.getConfigManager().getLong("transparency.update-interval", 300);
        this.useActionBar = plugin.getConfigManager().getBoolean("transparency.display.actionbar", true);
    }
    
    /**
     * Start transparency updates
     */
    public void start() {
        if (!enabled) {
            return;
        }
        
        long intervalTicks = updateIntervalSeconds * 20; // Convert to ticks
        
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            updateAllPlayers();
        }, 100L, intervalTicks);
        
        plugin.getLogger().info("Transparency mode started (interval: " + updateIntervalSeconds + "s)");
    }
    
    /**
     * Update all online players' displays
     */
    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("echo.bypass")) continue;
            
            updatePlayerDisplay(player);
        }
    }
    
    /**
     * Update single player's integrity score display
     */
    public void updatePlayerDisplay(Player player) {
        PlayerProfile profile = dataManager.getProfile(player);
        double score = profile.getCurrentIntegrityScore();
        
        String message = formatScoreMessage(score);
        
        if (useActionBar) {
            player.sendActionBar(message);
        }
    }
    
    /**
     * Format score message with color coding
     */
    private String formatScoreMessage(double score) {
        String color;
        String label;
        
        if (score >= 80) {
            color = "§a"; // Green
            label = "Clean";
        } else if (score >= 60) {
            color = "§2"; // Dark Green
            label = "Normal";
        } else if (score >= 40) {
            color = "§e"; // Yellow
            label = "Minor Issues";
        } else if (score >= 20) {
            color = "§6"; // Gold
            label = "Suspicious";
        } else {
            color = "§c"; // Red
            label = "Extremely Suspicious";
        }
        
        return String.format("%s⚡ Integrity: %s%.0f/100 §8[%s%s§8]", 
                color, color, score, color, label);
    }
    
    /**
     * Shutdown transparency manager
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }
}
