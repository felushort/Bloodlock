package com.void.echo.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.void.echo.EchoPlugin;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages player data (buffers and profiles)
 */
public class PlayerDataManager {
    
    private final EchoPlugin plugin;
    
    // Active player buffers (cached, auto-expire)
    @Getter
    private final Cache<UUID, BehavioralBuffer> bufferCache;
    
    // Long-term player profiles (cached)
    @Getter
    private final Cache<UUID, PlayerProfile> profileCache;
    
    public PlayerDataManager(EchoPlugin plugin) {
        this.plugin = plugin;
        
        // Initialize buffer cache (expire after 5 minutes of inactivity)
        this.bufferCache = Caffeine.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
        
        // Initialize profile cache (expire after 1 hour of inactivity)
        this.profileCache = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }
    
    /**
     * Get or create behavioral buffer for player
     */
    public BehavioralBuffer getBuffer(UUID playerId) {
        return bufferCache.get(playerId, id -> new BehavioralBuffer(id));
    }
    
    /**
     * Get or create profile for player
     */
    public PlayerProfile getProfile(Player player) {
        return getProfile(player.getUniqueId(), player.getName());
    }
    
    /**
     * Get or create profile for player by UUID
     */
    public PlayerProfile getProfile(UUID playerId, String playerName) {
        return profileCache.get(playerId, id -> {
            // Try to load from storage
            PlayerProfile profile = loadProfile(id);
            if (profile == null) {
                // Create new profile
                profile = new PlayerProfile(id, playerName);
                plugin.getLogger().info("Created new profile for " + playerName);
            } else {
                plugin.getLogger().info("Loaded existing profile for " + playerName);
            }
            return profile;
        });
    }
    
    /**
     * Remove player data (on logout or timeout)
     */
    public void removePlayer(UUID playerId) {
        // Save profile before removing
        PlayerProfile profile = profileCache.getIfPresent(playerId);
        if (profile != null) {
            saveProfile(profile);
        }
        
        // Clear buffer
        bufferCache.invalidate(playerId);
    }
    
    /**
     * Save all active profiles
     */
    public void saveAll() {
        plugin.getLogger().info("Saving all player profiles...");
        
        int count = 0;
        for (PlayerProfile profile : profileCache.asMap().values()) {
            saveProfile(profile);
            count++;
        }
        
        plugin.getLogger().info("Saved " + count + " player profiles.");
    }
    
    /**
     * Load profile from storage
     * TODO: Implement persistent storage (database/file)
     */
    private PlayerProfile loadProfile(UUID playerId) {
        // For now, return null (no persistent storage yet)
        // In production, this would load from database or file
        return null;
    }
    
    /**
     * Save profile to storage
     * TODO: Implement persistent storage (database/file)
     */
    private void saveProfile(PlayerProfile profile) {
        // For now, just log
        // In production, this would save to database or file
        plugin.getLogger().fine("Saving profile for " + profile.getPlayerName() + 
                " (Score: " + String.format("%.1f", profile.getCurrentIntegrityScore()) + ")");
    }
    
    /**
     * Get statistics summary
     */
    public String getStats() {
        return String.format("Active Buffers: %d | Cached Profiles: %d",
                bufferCache.estimatedSize(),
                profileCache.estimatedSize());
    }
}
