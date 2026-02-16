package com.void.echo.integration;

import com.void.echo.EchoPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * LuckPerms integration for ECHO
 * Provides permission-based exemptions and enhanced permission handling
 */
public class LuckPermsIntegration {
    
    private final EchoPlugin plugin;
    private LuckPerms luckPerms;
    private boolean enabled = false;
    
    public LuckPermsIntegration(EchoPlugin plugin) {
        this.plugin = plugin;
        initialize();
    }
    
    /**
     * Initialize LuckPerms integration
     */
    private void initialize() {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
                luckPerms = LuckPermsProvider.get();
                enabled = true;
                plugin.getLogger().info("✓ LuckPerms integration enabled");
            } else {
                plugin.getLogger().info("✗ LuckPerms not found - using basic permissions");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize LuckPerms integration: " + e.getMessage());
            enabled = false;
        }
    }
    
    /**
     * Check if LuckPerms is available
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get all exemptions for a player
     */
    public Set<String> getExemptions(Player player) {
        Set<String> exemptions = new HashSet<>();
        
        if (!enabled) {
            // Fallback to basic permission checks
            if (player.hasPermission("echo.exempt.all")) {
                exemptions.add("all");
            }
            if (player.hasPermission("echo.exempt.aimbot")) exemptions.add("aimbot");
            if (player.hasPermission("echo.exempt.autoclicker")) exemptions.add("autoclicker");
            if (player.hasPermission("echo.exempt.killaura")) exemptions.add("killaura");
            if (player.hasPermission("echo.exempt.reach")) exemptions.add("reach");
            if (player.hasPermission("echo.exempt.timer")) exemptions.add("timer");
            if (player.hasPermission("echo.exempt.velocity")) exemptions.add("velocity");
            return exemptions;
        }
        
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return exemptions;
        
        // Check for exemption permissions
        user.getNodes().forEach(node -> {
            String permission = node.getKey();
            if (permission.startsWith("echo.exempt.")) {
                String exemption = permission.substring("echo.exempt.".length());
                exemptions.add(exemption);
            }
        });
        
        return exemptions;
    }
    
    /**
     * Check if player is exempt from a specific check
     */
    public boolean isExempt(Player player, String checkType) {
        if (player.hasPermission("echo.bypass") || player.hasPermission("echo.exempt.all")) {
            return true;
        }
        
        return player.hasPermission("echo.exempt." + checkType.toLowerCase());
    }
    
    /**
     * Add exemption for a player
     */
    public boolean addExemption(UUID playerId, String checkType) {
        if (!enabled) return false;
        
        User user = luckPerms.getUserManager().getUser(playerId);
        if (user == null) return false;
        
        Node node = Node.builder("echo.exempt." + checkType.toLowerCase()).build();
        user.data().add(node);
        luckPerms.getUserManager().saveUser(user);
        
        return true;
    }
    
    /**
     * Remove exemption for a player
     */
    public boolean removeExemption(UUID playerId, String checkType) {
        if (!enabled) return false;
        
        User user = luckPerms.getUserManager().getUser(playerId);
        if (user == null) return false;
        
        Node node = Node.builder("echo.exempt." + checkType.toLowerCase()).build();
        user.data().remove(node);
        luckPerms.getUserManager().saveUser(user);
        
        return true;
    }
    
    /**
     * Check if player is in a staff group
     */
    public boolean isStaff(Player player) {
        if (!enabled) {
            return player.hasPermission("echo.admin") || player.hasPermission("echo.staff");
        }
        
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return false;
        
        // Check if in common staff groups
        return user.getInheritedGroups(user.getQueryOptions()).stream()
                .anyMatch(group -> {
                    String name = group.getName().toLowerCase();
                    return name.contains("staff") || name.contains("admin") || 
                           name.contains("mod") || name.contains("helper");
                });
    }
    
    /**
     * Get player's primary group
     */
    public String getPrimaryGroup(Player player) {
        if (!enabled) return "default";
        
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "default";
        
        return user.getPrimaryGroup();
    }
    
    /**
     * Check if player has specific alert permission level
     */
    public boolean hasAlertLevel(Player player, int level) {
        return player.hasPermission("echo.alerts.level." + level) || 
               player.hasPermission("echo.alerts");
    }
    
    /**
     * Get player's maximum alert level
     */
    public int getAlertLevel(Player player) {
        if (player.hasPermission("echo.alerts.level.3")) return 3; // Critical alerts
        if (player.hasPermission("echo.alerts.level.2")) return 2; // Important alerts
        if (player.hasPermission("echo.alerts.level.1")) return 1; // Basic alerts
        return 0; // No alerts
    }
}
