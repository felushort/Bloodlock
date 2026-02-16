package com.void.echo.physics;

import com.void.echo.EchoPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Layer 4: Dynamic Physics Drift
 * 
 * Introduces microscopic physics variations per player or match.
 * 
 * Key idea: Cheat clients are calibrated for vanilla physics constants.
 * By introducing tiny, imperceptible variations, we destabilize
 * automation that relies on predictable physics.
 * 
 * Examples:
 * - Knockback vector scaling ±2%
 * - Air acceleration ±1.5%
 * - Friction rounding ±1%
 * 
 * Legit players won't notice.
 * Precise automation will become unreliable.
 */
public class PhysicsDriftManager {
    
    private final EchoPlugin plugin;
    private final Random random;
    
    // Per-player physics modifiers
    @Getter
    private final Map<UUID, PhysicsModifiers> playerModifiers;
    
    // Configuration
    private final double knockbackVariance;
    private final double airAccelVariance;
    private final double frictionVariance;
    private final long rotationIntervalMinutes;
    
    // Rotation task
    private BukkitTask rotationTask;
    
    public PhysicsDriftManager(EchoPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.playerModifiers = new HashMap<>();
        
        // Load configuration
        this.knockbackVariance = plugin.getConfigManager().getDouble("physics-drift.variance.knockback-scaling", 0.02);
        this.airAccelVariance = plugin.getConfigManager().getDouble("physics-drift.variance.air-acceleration", 0.015);
        this.frictionVariance = plugin.getConfigManager().getDouble("physics-drift.variance.friction", 0.01);
        this.rotationIntervalMinutes = plugin.getConfigManager().getLong("physics-drift.rotation-interval", 15);
    }
    
    /**
     * Start physics drift rotation
     */
    public void start() {
        // Rotate physics modifiers periodically
        long intervalTicks = rotationIntervalMinutes * 60 * 20; // Convert minutes to ticks
        
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            rotatePhysicsModifiers();
        }, intervalTicks, intervalTicks);
        
        plugin.getLogger().info("Physics drift rotation started (interval: " + rotationIntervalMinutes + " mins)");
    }
    
    /**
     * Get physics modifiers for a player
     */
    public PhysicsModifiers getModifiers(UUID playerId) {
        return playerModifiers.computeIfAbsent(playerId, id -> generateModifiers());
    }
    
    /**
     * Generate random physics modifiers
     */
    private PhysicsModifiers generateModifiers() {
        return new PhysicsModifiers(
                1.0 + randomVariance(knockbackVariance),
                1.0 + randomVariance(airAccelVariance),
                1.0 + randomVariance(frictionVariance),
                1.0 + randomVariance(0.005) // Sprint timing
        );
    }
    
    /**
     * Generate random variance within range
     */
    private double randomVariance(double range) {
        return (random.nextDouble() * 2.0 - 1.0) * range; // -range to +range
    }
    
    /**
     * Rotate physics modifiers for all online players
     */
    private void rotatePhysicsModifiers() {
        int rotated = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("echo.bypass")) continue;
            
            // Generate new modifiers
            playerModifiers.put(player.getUniqueId(), generateModifiers());
            rotated++;
        }
        
        plugin.getLogger().fine("Rotated physics modifiers for " + rotated + " players");
    }
    
    /**
     * Shutdown physics drift
     */
    public void shutdown() {
        if (rotationTask != null) {
            rotationTask.cancel();
        }
    }
    
    /**
     * Physics modifiers for a player
     */
    @Getter
    public static class PhysicsModifiers {
        private final double knockbackMultiplier;
        private final double airAccelMultiplier;
        private final double frictionMultiplier;
        private final double sprintTimingMultiplier;
        
        public PhysicsModifiers(double knockback, double airAccel, double friction, double sprintTiming) {
            this.knockbackMultiplier = knockback;
            this.airAccelMultiplier = airAccel;
            this.frictionMultiplier = friction;
            this.sprintTimingMultiplier = sprintTiming;
        }
    }
}
