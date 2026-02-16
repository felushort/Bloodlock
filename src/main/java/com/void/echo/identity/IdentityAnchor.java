package com.void.echo.identity;

import com.void.echo.EchoPlugin;
import com.void.echo.data.BehavioralBuffer;
import com.void.echo.data.ClickData;
import com.void.echo.data.PlayerDataManager;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Layer 8: Anti-Spoof Identity Anchoring
 * 
 * Builds a behavioral biometric fingerprint for each player.
 * 
 * Clients can spoof:
 * - Brand strings
 * - Mod lists
 * - Randomized rotations
 * 
 * But they can't easily fake:
 * - Long-term behavioral continuity
 * - Fatigue drift over time
 * - Cross-session mouse signature
 * - Hardware DPI-based micro patterns
 * 
 * If someone logs in from a "different client" but claims to be the same person,
 * behavioral mismatch exposes it.
 */
public class IdentityAnchor {
    
    private final EchoPlugin plugin;
    private final PlayerDataManager dataManager;
    
    // Fingerprints by player
    private final Map<UUID, BehavioralFingerprint> fingerprints;
    
    // Configuration
    private final double similarityThreshold;
    
    public IdentityAnchor(EchoPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.fingerprints = new HashMap<>();
        
        this.similarityThreshold = plugin.getConfigManager().getDouble("identity.session-validation.similarity-threshold", 0.75);
    }
    
    /**
     * Build or update fingerprint for player
     */
    public void updateFingerprint(UUID playerId) {
        BehavioralBuffer buffer = dataManager.getBuffer(playerId);
        
        // Get behavioral data
        var rotations = buffer.getRecentRotations(60000); // Last minute
        var clicks = buffer.getRecentClicks(60000);
        
        if (rotations.size() < 50 || clicks.size() < 20) {
            return; // Not enough data
        }
        
        // Calculate fingerprint features
        double[] features = extractFeatures(buffer);
        
        // Update or create fingerprint
        BehavioralFingerprint fingerprint = fingerprints.computeIfAbsent(playerId, id -> new BehavioralFingerprint());
        fingerprint.update(features);
    }
    
    /**
     * Extract behavioral features for fingerprinting
     */
    private double[] extractFeatures(BehavioralBuffer buffer) {
        var rotations = buffer.getRecentRotations(60000);
        var clicks = buffer.getRecentClicks(60000);
        
        // Extract various behavioral features
        double[] features = new double[10];
        
        // Feature 1: Average yaw delta magnitude
        features[0] = rotations.stream()
                .mapToDouble(r -> Math.abs(r.getYawDelta()))
                .average()
                .orElse(0.0);
        
        // Feature 2: Average pitch delta magnitude
        features[1] = rotations.stream()
                .mapToDouble(r -> Math.abs(r.getPitchDelta()))
                .average()
                .orElse(0.0);
        
        // Feature 3: Rotation variance
        features[2] = com.void.echo.util.MathUtil.standardDeviation(
                rotations.stream()
                        .mapToDouble(r -> r.getRotationMagnitude())
                        .toArray()
        );
        
        // Feature 4: Average click interval
        features[3] = clicks.stream()
                .mapToDouble(ClickData::getTimeSinceLastClick)
                .filter(x -> x > 0)
                .average()
                .orElse(0.0);
        
        // Feature 5: Click interval variance
        features[4] = com.void.echo.util.MathUtil.standardDeviation(
                clicks.stream()
                        .mapToDouble(ClickData::getTimeSinceLastClick)
                        .filter(x -> x > 0)
                        .toArray()
        );
        
        // Feature 6-10: Reserved for additional features
        // (Could add: DPI patterns, acceleration curves, etc.)
        
        return features;
    }
    
    /**
     * Validate if current behavior matches established fingerprint
     * Returns similarity score (0.0 to 1.0)
     */
    public double validateIdentity(UUID playerId) {
        BehavioralFingerprint fingerprint = fingerprints.get(playerId);
        if (fingerprint == null || !fingerprint.isEstablished()) {
            return 1.0; // No baseline yet, assume valid
        }
        
        // Extract current features
        BehavioralBuffer buffer = dataManager.getBuffer(playerId);
        double[] currentFeatures = extractFeatures(buffer);
        
        // Calculate similarity
        return fingerprint.calculateSimilarity(currentFeatures);
    }
    
    /**
     * Behavioral fingerprint
     */
    @Data
    private static class BehavioralFingerprint {
        private double[] baselineFeatures;
        private int updateCount = 0;
        private static final int MIN_UPDATES = 5;
        
        public void update(double[] features) {
            if (baselineFeatures == null) {
                baselineFeatures = features.clone();
            } else {
                // Exponential moving average
                for (int i = 0; i < baselineFeatures.length && i < features.length; i++) {
                    baselineFeatures[i] = 0.9 * baselineFeatures[i] + 0.1 * features[i];
                }
            }
            
            updateCount++;
        }
        
        public boolean isEstablished() {
            return updateCount >= MIN_UPDATES;
        }
        
        public double calculateSimilarity(double[] features) {
            if (baselineFeatures == null || features == null) {
                return 1.0;
            }
            
            // Calculate cosine similarity
            double dotProduct = 0.0;
            double normA = 0.0;
            double normB = 0.0;
            
            for (int i = 0; i < Math.min(baselineFeatures.length, features.length); i++) {
                dotProduct += baselineFeatures[i] * features[i];
                normA += baselineFeatures[i] * baselineFeatures[i];
                normB += features[i] * features[i];
            }
            
            double denominator = Math.sqrt(normA) * Math.sqrt(normB);
            return denominator > 0 ? dotProduct / denominator : 1.0;
        }
    }
}
