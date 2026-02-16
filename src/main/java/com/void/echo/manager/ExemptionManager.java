package com.void.echo.manager;

import com.void.echo.EchoPlugin;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages check exemptions for players
 */
public class ExemptionManager {
    
    private final EchoPlugin plugin;
    
    // Temporary exemptions (session-based)
    private final Map<UUID, Set<String>> temporaryExemptions = new ConcurrentHashMap<>();
    
    // Permanent exemptions (config-based)
    private final Map<UUID, Set<String>> permanentExemptions = new ConcurrentHashMap<>();
    
    public ExemptionManager(EchoPlugin plugin) {
        this.plugin = plugin;
        loadExemptions();
    }
    
    /**
     * Load exemptions from config
     */
    private void loadExemptions() {
        // TODO: Load from config or database
    }
    
    /**
     * Check if player is exempt from a specific check
     */
    public boolean isExempt(Player player, String checkType) {
        UUID uuid = player.getUniqueId();
        
        // Check permission-based exemptions first
        if (player.hasPermission("echo.bypass")) return true;
        if (player.hasPermission("echo.exempt.all")) return true;
        if (player.hasPermission("echo.exempt." + checkType.toLowerCase())) return true;
        
        // Check temporary exemptions
        Set<String> tempExempt = temporaryExemptions.get(uuid);
        if (tempExempt != null && (tempExempt.contains("all") || tempExempt.contains(checkType))) {
            return true;
        }
        
        // Check permanent exemptions
        Set<String> permExempt = permanentExemptions.get(uuid);
        return permExempt != null && (permExempt.contains("all") || permExempt.contains(checkType));
    }
    
    /**
     * Add temporary exemption for a player (until they log out)
     */
    public void addTemporaryExemption(UUID playerId, String checkType) {
        temporaryExemptions.computeIfAbsent(playerId, k -> new HashSet<>()).add(checkType.toLowerCase());
    }
    
    /**
     * Remove temporary exemption
     */
    public void removeTemporaryExemption(UUID playerId, String checkType) {
        Set<String> exemptions = temporaryExemptions.get(playerId);
        if (exemptions != null) {
            exemptions.remove(checkType.toLowerCase());
            if (exemptions.isEmpty()) {
                temporaryExemptions.remove(playerId);
            }
        }
    }
    
    /**
     * Add permanent exemption for a player
     */
    public void addPermanentExemption(UUID playerId, String checkType) {
        permanentExemptions.computeIfAbsent(playerId, k -> new HashSet<>()).add(checkType.toLowerCase());
        saveExemptions();
    }
    
    /**
     * Remove permanent exemption
     */
    public void removePermanentExemption(UUID playerId, String checkType) {
        Set<String> exemptions = permanentExemptions.get(playerId);
        if (exemptions != null) {
            exemptions.remove(checkType.toLowerCase());
            if (exemptions.isEmpty()) {
                permanentExemptions.remove(playerId);
            }
        }
        saveExemptions();
    }
    
    /**
     * Clear all exemptions for a player
     */
    public void clearExemptions(UUID playerId) {
        temporaryExemptions.remove(playerId);
        permanentExemptions.remove(playerId);
        saveExemptions();
    }
    
    /**
     * Get all exemptions for a player
     */
    public Set<String> getExemptions(UUID playerId) {
        Set<String> all = new HashSet<>();
        
        Set<String> temp = temporaryExemptions.get(playerId);
        if (temp != null) all.addAll(temp);
        
        Set<String> perm = permanentExemptions.get(playerId);
        if (perm != null) all.addAll(perm);
        
        return all;
    }
    
    /**
     * Get available check types
     */
    public List<String> getAvailableChecks() {
        return Arrays.asList(
            "all",
            "aimbot",
            "autoclicker",
            "killaura",
            "reach",
            "timer",
            "velocity",
            "scaffold",
            "fly"
        );
    }
    
    /**
     * Clean up exemptions for disconnected players
     */
    public void cleanup(UUID playerId) {
        temporaryExemptions.remove(playerId);
    }
    
    /**
     * Save exemptions to config
     */
    private void saveExemptions() {
        // TODO: Save to config or database
    }
}
