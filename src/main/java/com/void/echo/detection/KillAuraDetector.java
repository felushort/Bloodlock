package com.void.echo.detection;

import com.void.echo.data.MovementData;
import com.void.echo.util.MathUtil;
import lombok.Data;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * KillAura/Combat detection using attack pattern analysis
 * 
 * Detection methods:
 * 1. Multi-target engagement analysis (attacking multiple entities rapidly)
 * 2. Attack angle consistency (perfect angles to all targets)
 * 3. Attack range analysis
 * 4. Head rotation vs attack direction mismatch
 * 5. Attack rate analysis (superhuman attack speeds)
 * 6. Target switching patterns
 */
public class KillAuraDetector {
    
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.75;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.50;
    private static final double MAX_LEGIT_REACH = 3.1;
    
    /**
     * Attack event data
     */
    @Data
    public static class AttackEvent {
        private final long timestamp;
        private final int targetEntityId;
        private final double distance;
        private final float yaw;
        private final float pitch;
        private final float yawToTarget;
        private final float pitchToTarget;
        private final boolean wasHit;
        
        public double getAngleDifference() {
            double yawDiff = Math.abs(MathUtil.angleDifference(yaw, yawToTarget));
            double pitchDiff = Math.abs(pitch - pitchToTarget);
            return Math.sqrt(yawDiff*yawDiff + pitchDiff*pitchDiff);
        }
    }
    
    /**
     * Analyze attack pattern for KillAura signatures
     */
    public static KillAuraAnalysis analyze(List<AttackEvent> attacks) {
        if (attacks.size() < 10) {
            return new KillAuraAnalysis();
        }
        
        KillAuraAnalysis analysis = new KillAuraAnalysis();
        
        // 1. Multi-target analysis
        double multiTargetScore = analyzeMultiTarget(attacks);
        analysis.setMultiTargetScore(multiTargetScore);
        
        // 2. Attack angle consistency
        double angleConsistency = analyzeAngleConsistency(attacks);
        analysis.setAngleConsistency(angleConsistency);
        
        // 3. Reach analysis
        double reachScore = analyzeReach(attacks);
        analysis.setReachScore(reachScore);
        
        // 4. Attack rate analysis
        double attackRate = analyzeAttackRate(attacks);
        analysis.setAttackRate(attackRate);
        
        // 5. Perfect hit ratio
        double hitRatio = analyzePerfectHits(attacks);
        analysis.setPerfectHitRatio(hitRatio);
        
        // 6. Target switch analysis
        double switchingScore = analyzeTargetSwitching(attacks);
        analysis.setTargetSwitchingScore(switchingScore);
        
        // 7. Head rotation mismatch
        double rotationMismatch = analyzeRotationMismatch(attacks);
        analysis.setRotationMismatchScore(rotationMismatch);
        
        // 8. Attack pattern regularity
        double regularity = analyzeAttackRegularity(attacks);
        analysis.setAttackRegularity(regularity);
        
        // Calculate final confidence
        double confidence = calculateKillAuraConfidence(analysis);
        analysis.setKillAuraConfidence(confidence);
        
        return analysis;
    }
    
    /**
     * Analyze multi-target engagement
     * KillAura hits multiple targets in rapid succession
     */
    private static double analyzeMultiTarget(List<AttackEvent> attacks) {
        if (attacks.size() < 5) return 0.0;
        
        // Count unique targets in short time windows
        int windowMs = 1000; // 1 second
        double maxTargetsPerSecond = 0;
        
        for (int i = 0; i < attacks.size(); i++) {
            long windowStart = attacks.get(i).getTimestamp();
            java.util.Set<Integer> uniqueTargets = new java.util.HashSet<>();
            
            for (int j = i; j < attacks.size(); j++) {
                long timestamp = attacks.get(j).getTimestamp();
                if (timestamp - windowStart > windowMs) break;
                
                uniqueTargets.add(attacks.get(j).getTargetEntityId());
            }
            
            if (uniqueTargets.size() > maxTargetsPerSecond) {
                maxTargetsPerSecond = uniqueTargets.size();
            }
        }
        
        // More than 3 different targets in 1 second is suspicious
        if (maxTargetsPerSecond >= 5) {
            return 1.0;
        } else if (maxTargetsPerSecond >= 3) {
            return (maxTargetsPerSecond - 3) / 2.0;
        }
        
        return 0.0;
    }
    
    /**
     * Analyze attack angle consistency
     * KillAura often has perfect angles to all targets
     */
    private static double analyzeAngleConsistency(List<AttackEvent> attacks) {
        if (attacks.size() < 5) return 0.0;
        
        double[] angleDifferences = attacks.stream()
                .mapToDouble(AttackEvent::getAngleDifference)
                .toArray();
        
        // Count perfect angles (< 5 degrees difference)
        long perfectAngles = 0;
        for (double diff : angleDifferences) {
            if (diff < 5.0) {
                perfectAngles++;
            }
        }
        
        double perfectRatio = (double) perfectAngles / angleDifferences.length;
        
        // Very high perfect ratio with multi-target = suspicious
        // Humans can't maintain perfect angles when rapidly switching targets
        if (perfectRatio > 0.8) {
            return 1.0;
        } else if (perfectRatio > 0.6) {
            return (perfectRatio - 0.6) / 0.2;
        }
        
        return 0.0;
    }
    
    /**
     * Analyze reach patterns
     * Consistent hits at maximum reach is suspicious
     */
    private static double analyzeReach(List<AttackEvent> attacks) {
        if (attacks.size() < 10) return 0.0;
        
        double[] distances = attacks.stream()
                .filter(AttackEvent::wasHit)
                .mapToDouble(AttackEvent::getDistance)
                .toArray();
        
        if (distances.length < 5) return 0.0;
        
        // Count hits near maximum reach (> 2.9 blocks)
        long longReachHits = 0;
        for (double dist : distances) {
            if (dist > 2.9 && dist <= MAX_LEGIT_REACH) {
                longReachHits++;
            }
        }
        
        double longReachRatio = (double) longReachHits / distances.length;
        
        // High ratio of long-reach hits is suspicious
        if (longReachRatio > 0.5) {
            return (longReachRatio - 0.5) / 0.5;
        }
        
        return 0.0;
    }
    
    /**
     * Analyze attack rate
     * Superhuman attack speeds
     */
    private static double analyzeAttackRate(List<AttackEvent> attacks) {
        if (attacks.size() < 3) return 0.0;
        
        // Calculate intervals
        double[] intervals = new double[attacks.size() - 1];
        for (int i = 1; i < attacks.size(); i++) {
            intervals[i-1] = attacks.get(i).getTimestamp() - attacks.get(i-1).getTimestamp();
        }
        
        double avgInterval = MathUtil.mean(intervals);
        double attacksPerSecond = 1000.0 / avgInterval;
        
        // More than 15 attacks/second is suspicious
        // Minecraft has attack cooldown, but some mods bypass it
        if (attacksPerSecond > 20) {
            return 1.0;
        } else if (attacksPerSecond > 15) {
            return (attacksPerSecond - 15) / 5.0;
        }
        
        return 0.0;
    }
    
    /**
     * Analyze perfect hit ratio
     * KillAura rarely misses
     */
    private static double analyzePerfectHits(List<AttackEvent> attacks) {
        long hits = attacks.stream().filter(AttackEvent::wasHit).count();
        double hitRatio = (double) hits / attacks.size();
        
        // Perfect or near-perfect hit ratio is suspicious when combined with other factors
        if (hitRatio > 0.95) {
            return 0.6;
        } else if (hitRatio > 0.90) {
            return 0.3;
        }
        
        return 0.0;
    }
    
    /**
     * Analyze target switching patterns
     * KillAura switches targets very rapidly and consistently
     */
    private static double analyzeTargetSwitching(List<AttackEvent> attacks) {
        if (attacks.size() < 5) return 0.0;
        
        // Count target switches
        int switches = 0;
        for (int i = 1; i < attacks.size(); i++) {
            if (attacks.get(i).getTargetEntityId() != attacks.get(i-1).getTargetEntityId()) {
                switches++;
            }
        }
        
        double switchRatio = (double) switches / (attacks.size() - 1);
        
        // Very high switch rate with perfect angles = suspicious
        if (switchRatio > 0.7) {
            return 1.0;
        } else if (switchRatio > 0.5) {
            return (switchRatio - 0.5) / 0.2;
        }
        
        return 0.0;
    }
    
    /**
     * Analyze head rotation vs attack direction mismatch
     * Some KillAuras attack entities not in view
     */
    private static double analyzeRotationMismatch(List<AttackEvent> attacks) {
        if (attacks.size() < 5) return 0.0;
        
        // Count attacks with large angle difference
        int mismatchCount = 0;
        for (AttackEvent attack : attacks) {
            double angleDiff = attack.getAngleDifference();
            
            // Attack more than 45 degrees off from where looking = suspicious
            if (angleDiff > 45.0 && attack.wasHit()) {
                mismatchCount++;
            }
        }
        
        double mismatchRatio = (double) mismatchCount / attacks.size();
        
        if (mismatchRatio > 0.3) {
            return 1.0;
        } else if (mismatchRatio > 0.15) {
            return (mismatchRatio - 0.15) / 0.15;
        }
        
        return 0.0;
    }
    
    /**
     * Analyze attack pattern regularity
     * Perfect timing consistency suggests automation
     */
    private static double analyzeAttackRegularity(List<AttackEvent> attacks) {
        if (attacks.size() < 10) return 0.0;
        
        // Calculate attack intervals
        double[] intervals = new double[attacks.size() - 1];
        for (int i = 1; i < attacks.size(); i++) {
            intervals[i-1] = attacks.get(i).getTimestamp() - attacks.get(i-1).getTimestamp();
        }
        
        // Check for regularity
        double stdDev = MathUtil.standardDeviation(intervals);
        double mean = MathUtil.mean(intervals);
        
        if (mean < 1.0) return 0.0;
        
        double cv = stdDev / mean;
        
        // Very low CV = very regular = suspicious
        if (cv < 0.15) {
            return 1.0 - (cv / 0.15);
        }
        
        return 0.0;
    }
    
    /**
     * Calculate final KillAura confidence
     */
    private static double calculateKillAuraConfidence(KillAuraAnalysis analysis) {
        // Weights
        double multiTargetWeight = 0.25;
        double angleWeight = 0.20;
        double reachWeight = 0.15;
        double rateWeight = 0.10;
        double hitRatioWeight = 0.10;
        double switchingWeight = 0.10;
        double mismatchWeight = 0.05;
        double regularityWeight = 0.05;
        
        double totalSuspicion = 
            analysis.getMultiTargetScore() * multiTargetWeight +
            analysis.getAngleConsistency() * angleWeight +
            analysis.getReachScore() * reachWeight +
            analysis.getAttackRate() * rateWeight +
            analysis.getPerfectHitRatio() * hitRatioWeight +
            analysis.getTargetSwitchingScore() * switchingWeight +
            analysis.getRotationMismatchScore() * mismatchWeight +
            analysis.getAttackRegularity() * regularityWeight;
        
        return Math.min(1.0, Math.max(0.0, totalSuspicion));
    }
    
    /**
     * Analysis result container
     */
    @Data
    public static class KillAuraAnalysis {
        private double multiTargetScore = 0.0;       // 0-1, attacks multiple targets rapidly
        private double angleConsistency = 0.0;       // 0-1, perfect angles to all targets
        private double reachScore = 0.0;             // 0-1, consistent maximum reach
        private double attackRate = 0.0;             // 0-1, superhuman attack speed
        private double perfectHitRatio = 0.0;        // 0-1, rarely misses
        private double targetSwitchingScore = 0.0;   // 0-1, rapid target switching
        private double rotationMismatchScore = 0.0;  // 0-1, attacks outside view
        private double attackRegularity = 0.0;       // 0-1, perfect timing consistency
        
        private double killAuraConfidence = 0.0;     // 0-1, overall confidence
        
        public String getConfidenceLevel() {
            if (killAuraConfidence >= HIGH_CONFIDENCE_THRESHOLD) {
                return "HIGH";
            } else if (killAuraConfidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
                return "MEDIUM";
            } else {
                return "LOW";
            }
        }
        
        public boolean isLikelyKillAura() {
            return killAuraConfidence >= HIGH_CONFIDENCE_THRESHOLD;
        }
    }
}
