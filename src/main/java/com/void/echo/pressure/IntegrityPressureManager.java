package com.void.echo.pressure;

import com.void.echo.EchoPlugin;
import com.void.echo.data.PlayerDataManager;
import com.void.echo.data.PlayerProfile;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Layer 5: Silent Integrity Pressure
 * 
 * When integrity scores drop, instead of banning immediately,
 * we apply "pressure" - stricter validation that makes cheats unreliable.
 * 
 * To legit players: Nothing changes
 * To cheaters: Hits start failing, combos drop, reach stops working
 * 
 * Creates uncertainty: "Why does this server feel weird?"
 * 
 * Pressure levels:
 * - Light (score 30-45): Stricter raytrace, minor reach reduction
 * - Medium (score 15-30): + knockback increase, packet order enforcement
 * - Heavy (score 0-15): + additional checks, max restrictions
 */
public class IntegrityPressureManager {
    
    private final EchoPlugin plugin;
    private final PlayerDataManager dataManager;
    
    // Pressure levels by player
    @Getter
    private final Map<UUID, PressureLevel> playerPressure;
    
    // Configuration
    private final double activationThreshold;
    
    public IntegrityPressureManager(EchoPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.playerPressure = new HashMap<>();
        
        this.activationThreshold = plugin.getConfigManager().getDouble("integrity-pressure.activation-threshold", 45.0);
    }
    
    /**
     * Update pressure level for a player based on integrity score
     */
    public void updatePressure(UUID playerId, double integrityScore) {
        PressureLevel level;
        
        if (integrityScore >= activationThreshold) {
            level = PressureLevel.NONE;
        } else if (integrityScore >= 30) {
            level = PressureLevel.LIGHT;
        } else if (integrityScore >= 15) {
            level = PressureLevel.MEDIUM;
        } else {
            level = PressureLevel.HEAVY;
        }
        
        PressureLevel oldLevel = playerPressure.put(playerId, level);
        
        // Log pressure changes
        if (oldLevel != level && level != PressureLevel.NONE) {
            PlayerProfile profile = dataManager.getProfile(playerId, "Unknown");
            plugin.getLogger().warning(String.format(
                    "Pressure level changed for %s: %s -> %s (Score: %.1f)",
                    profile.getPlayerName(),
                    oldLevel != null ? oldLevel : "NONE",
                    level,
                    integrityScore
            ));
        }
    }
    
    /**
     * Get pressure level for player
     */
    public PressureLevel getPressureLevel(UUID playerId) {
        return playerPressure.getOrDefault(playerId, PressureLevel.NONE);
    }
    
    /**
     * Get reach reduction multiplier
     */
    public double getReachReduction(UUID playerId) {
        return switch (getPressureLevel(playerId)) {
            case LIGHT -> 0.05;   // 5% reduction
            case MEDIUM -> 0.10;  // 10% reduction
            case HEAVY -> 0.15;   // 15% reduction
            default -> 0.0;       // No reduction
        };
    }
    
    /**
     * Get knockback increase multiplier
     */
    public double getKnockbackIncrease(UUID playerId) {
        return switch (getPressureLevel(playerId)) {
            case MEDIUM -> 0.05;  // 5% increase
            case HEAVY -> 0.10;   // 10% increase
            default -> 0.0;       // No increase
        };
    }
    
    /**
     * Check if stricter raytrace should be used
     */
    public boolean useStricterRaytrace(UUID playerId) {
        PressureLevel level = getPressureLevel(playerId);
        return level != PressureLevel.NONE;
    }
    
    /**
     * Check if strict packet order enforcement should be used
     */
    public boolean enforcePacketOrder(UUID playerId) {
        PressureLevel level = getPressureLevel(playerId);
        return level == PressureLevel.MEDIUM || level == PressureLevel.HEAVY;
    }
    
    /**
     * Pressure levels
     */
    public enum PressureLevel {
        NONE,
        LIGHT,
        MEDIUM,
        HEAVY
    }
}
