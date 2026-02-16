package com.void.echo.data;

import lombok.Data;

import java.util.*;

/**
 * Long-term statistical profile for a player
 * This stores baseline behavioral characteristics over days/weeks
 */
@Data
public class PlayerProfile {
    
    private final UUID playerId;
    private final String playerName;
    
    // Session tracking
    private long firstSeen;
    private long lastSeen;
    private long totalPlaytime; // In milliseconds
    private int sessionCount;
    
    // Baseline statistics - Rotation
    private double avgYawDelta;
    private double stdDevYawDelta;
    private double avgPitchDelta;
    private double stdDevPitchDelta;
    private double rotationEntropy;
    
    // Baseline statistics - Clicking
    private double avgCPS;
    private double stdDevCPS;
    private double clickEntropy;
    private double avgClickInterval;
    private double stdDevClickInterval;
    
    // Baseline statistics - Reaction Time
    private double avgReactionTime;
    private double stdDevReactionTime;
    
    // Baseline statistics - Combat
    private double avgCrosshairAlignment;
    private double avgReach;
    private double hitAccuracy;
    
    // Behavioral fingerprint
    private double[] rotationSignature;      // Spectral signature of rotation patterns
    private double[] clickPatternSignature;  // Signature of click timing distribution
    
    // Integrity tracking
    private double currentIntegrityScore;
    private double lowestIntegrityScore;
    private List<IntegrityViolation> violations;
    
    // Flags
    private boolean baselineEstablished;
    private boolean flaggedForReview;
    
    public PlayerProfile(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.firstSeen = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.totalPlaytime = 0;
        this.sessionCount = 1;
        
        // Default values
        this.currentIntegrityScore = 100.0;
        this.lowestIntegrityScore = 100.0;
        this.violations = new ArrayList<>();
        this.baselineEstablished = false;
        this.flaggedForReview = false;
        
        // Initialize signatures
        this.rotationSignature = new double[64];  // 64-bin spectrum
        this.clickPatternSignature = new double[32];  // 32-bin pattern
    }
    
    /**
     * Update baseline statistics from recent data
     */
    public void updateBaseline(BehavioralBuffer buffer) {
        // Get recent data
        List<RotationData> rotations = buffer.getRecentRotations(30000); // Last 30 seconds
        List<ClickData> clicks = buffer.getRecentClicks(30000);
        
        if (!rotations.isEmpty()) {
            updateRotationBaseline(rotations);
        }
        
        if (!clicks.isEmpty()) {
            updateClickBaseline(clicks);
        }
        
        // Update last seen
        this.lastSeen = System.currentTimeMillis();
    }
    
    /**
     * Update rotation baseline statistics
     */
    private void updateRotationBaseline(List<RotationData> rotations) {
        double[] yawDeltas = rotations.stream()
                .filter(RotationData::isSignificant)
                .mapToDouble(RotationData::getYawDelta)
                .toArray();
        
        double[] pitchDeltas = rotations.stream()
                .filter(RotationData::isSignificant)
                .mapToDouble(RotationData::getPitchDelta)
                .toArray();
        
        if (yawDeltas.length > 0) {
            // Use exponential moving average for smooth baseline updates
            double newAvgYaw = com.void.echo.util.MathUtil.mean(yawDeltas);
            double newStdDevYaw = com.void.echo.util.MathUtil.standardDeviation(yawDeltas);
            
            if (baselineEstablished) {
                this.avgYawDelta = 0.9 * this.avgYawDelta + 0.1 * newAvgYaw;
                this.stdDevYawDelta = 0.9 * this.stdDevYawDelta + 0.1 * newStdDevYaw;
            } else {
                this.avgYawDelta = newAvgYaw;
                this.stdDevYawDelta = newStdDevYaw;
            }
        }
        
        if (pitchDeltas.length > 0) {
            double newAvgPitch = com.void.echo.util.MathUtil.mean(pitchDeltas);
            double newStdDevPitch = com.void.echo.util.MathUtil.standardDeviation(pitchDeltas);
            
            if (baselineEstablished) {
                this.avgPitchDelta = 0.9 * this.avgPitchDelta + 0.1 * newAvgPitch;
                this.stdDevPitchDelta = 0.9 * this.stdDevPitchDelta + 0.1 * newStdDevPitch;
            } else {
                this.avgPitchDelta = newAvgPitch;
                this.stdDevPitchDelta = newStdDevPitch;
            }
        }
    }
    
    /**
     * Update click baseline statistics
     */
    private void updateClickBaseline(List<ClickData> clicks) {
        double[] intervals = clicks.stream()
                .mapToDouble(ClickData::getTimeSinceLastClick)
                .filter(x -> x > 0)
                .toArray();
        
        if (intervals.length > 0) {
            double newAvgInterval = com.void.echo.util.MathUtil.mean(intervals);
            double newStdDevInterval = com.void.echo.util.MathUtil.standardDeviation(intervals);
            double newAvgCPS = 1000.0 / newAvgInterval;
            
            if (baselineEstablished) {
                this.avgClickInterval = 0.9 * this.avgClickInterval + 0.1 * newAvgInterval;
                this.stdDevClickInterval = 0.9 * this.stdDevClickInterval + 0.1 * newStdDevInterval;
                this.avgCPS = 0.9 * this.avgCPS + 0.1 * newAvgCPS;
            } else {
                this.avgClickInterval = newAvgInterval;
                this.stdDevClickInterval = newStdDevInterval;
                this.avgCPS = newAvgCPS;
            }
        }
        
        // Calculate hit accuracy
        long hits = clicks.stream().filter(ClickData::wasHit).count();
        double accuracy = (double) hits / clicks.size();
        
        if (baselineEstablished) {
            this.hitAccuracy = 0.9 * this.hitAccuracy + 0.1 * accuracy;
        } else {
            this.hitAccuracy = accuracy;
        }
    }
    
    /**
     * Add an integrity violation
     */
    public void addViolation(String type, double severity, String description) {
        violations.add(new IntegrityViolation(
                System.currentTimeMillis(),
                type,
                severity,
                description,
                currentIntegrityScore
        ));
        
        // Keep only last 100 violations
        if (violations.size() > 100) {
            violations.remove(0);
        }
    }
    
    /**
     * Update integrity score
     */
    public void updateIntegrityScore(double newScore) {
        this.currentIntegrityScore = Math.max(0, Math.min(100, newScore));
        
        if (this.currentIntegrityScore < this.lowestIntegrityScore) {
            this.lowestIntegrityScore = this.currentIntegrityScore;
        }
        
        // Flag for review if score drops below threshold
        if (this.currentIntegrityScore < 30) {
            this.flaggedForReview = true;
        }
    }
    
    /**
     * Check if baseline is ready
     */
    public boolean canEstablishBaseline() {
        // Need at least 30 minutes of playtime
        return totalPlaytime >= 30 * 60 * 1000;
    }
    
    /**
     * Finalize baseline establishment
     */
    public void establishBaseline() {
        this.baselineEstablished = true;
    }
    
    /**
     * Calculate deviation from baseline
     */
    public double calculateDeviation(double value, double baselineMean, double baselineStdDev) {
        if (baselineStdDev == 0) return 0.0;
        return Math.abs(value - baselineMean) / baselineStdDev;
    }
    
    /**
     * Inner class for integrity violations
     */
    @Data
    public static class IntegrityViolation {
        private final long timestamp;
        private final String type;
        private final double severity;
        private final String description;
        private final double scoreAtTime;
    }
}
