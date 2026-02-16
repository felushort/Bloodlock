package com.void.echo.camouflage;

import com.void.echo.EchoPlugin;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Layer 7: Social Camouflage Detection
 * 
 * Tracks if players modify their behavior when:
 * - Staff are spectating
 * - In ranked vs casual fights
 * - Being recorded
 * 
 * If someone's aim entropy increases dramatically when spectated
 * but decreases to machine-like precision in normal fights...
 * 
 * That conditional behavior itself becomes evidence.
 */
public class CamouflageDetector {
    
    private final EchoPlugin plugin;
    
    // Tracking maps
    private final Map<UUID, BehaviorContext> normalBehavior;
    private final Map<UUID, BehaviorContext> spectatedBehavior;
    
    // Configuration
    private final double adaptationThreshold;
    
    public CamouflageDetector(EchoPlugin plugin) {
        this.plugin = plugin;
        this.normalBehavior = new HashMap<>();
        this.spectatedBehavior = new HashMap<>();
        
        this.adaptationThreshold = plugin.getConfigManager().getDouble("camouflage-detection.adaptation-threshold", 0.3);
    }
    
    /**
     * Record behavior in normal context
     */
    public void recordNormalBehavior(UUID playerId, double entropyScore) {
        BehaviorContext context = normalBehavior.computeIfAbsent(playerId, id -> new BehaviorContext());
        context.addSample(entropyScore);
    }
    
    /**
     * Record behavior while being spectated
     */
    public void recordSpectatedBehavior(UUID playerId, double entropyScore) {
        BehaviorContext context = spectatedBehavior.computeIfAbsent(playerId, id -> new BehaviorContext());
        context.addSample(entropyScore);
    }
    
    /**
     * Check if player exhibits camouflage behavior
     * Returns adaptation percentage (0.0 to 1.0)
     */
    public double checkCamouflage(UUID playerId) {
        BehaviorContext normal = normalBehavior.get(playerId);
        BehaviorContext spectated = spectatedBehavior.get(playerId);
        
        if (normal == null || spectated == null) {
            return 0.0;
        }
        
        if (!normal.hasEnoughSamples() || !spectated.hasEnoughSamples()) {
            return 0.0;
        }
        
        // Calculate difference in behavior
        double normalAvg = normal.getAverageScore();
        double spectatedAvg = spectated.getAverageScore();
        
        // Significant increase in score when spectated = suspicious
        double adaptation = (spectatedAvg - normalAvg) / 100.0;
        
        return Math.max(0.0, adaptation);
    }
    
    /**
     * Behavior context tracker
     */
    @Data
    private static class BehaviorContext {
        private double totalScore = 0.0;
        private int sampleCount = 0;
        
        public void addSample(double score) {
            totalScore += score;
            sampleCount++;
            
            // Keep rolling average (last 100 samples)
            if (sampleCount > 100) {
                totalScore = totalScore * 0.99;
                sampleCount = (int) (sampleCount * 0.99);
            }
        }
        
        public double getAverageScore() {
            return sampleCount > 0 ? totalScore / sampleCount : 0.0;
        }
        
        public boolean hasEnoughSamples() {
            return sampleCount >= 10;
        }
    }
}
