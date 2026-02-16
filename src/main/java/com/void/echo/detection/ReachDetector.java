package com.void.echo.detection;

import com.void.echo.data.MovementData;
import com.void.echo.util.MathUtil;
import lombok.Data;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced reach detection with latency compensation
 * 
 * Traditional reach detection is simple: distance > 3.0 = cheat.
 * This is flawed because:
 * - Network latency creates false positives
 * - Movement prediction needed
 * - Server/client desync
 * 
 * ECHO's approach:
 * 1. Latency compensation (ping-based adjustment)
 * 2. Movement prediction (velocity extrapolation)
 * 3. Statistical analysis (consistent max-reach = suspicious)
 * 4. Combined with other metrics (aim, clicks)
 */
public class ReachDetector {
    
    private static final double MAX_LEGIT_REACH = 3.0;
    private static final double SUSPICIOUS_REACH = 3.05;
    private static final double OBVIOUS_REACH = 3.2;
    
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.75;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.50;
    
    /**
     * Hit event with context
     */
    @Data
    public static class HitEvent {
        private final long timestamp;
        private final double distance;
        private final double targetVelocity;
        private final double attackerVelocity;
        private final int attackerPing;
        private final boolean targetMoving;
        private final double yawDifference;
        private final boolean sprinting;
        
        /**
         * Calculate latency-compensated reach
         */
        public double getCompensatedReach() {
            // Base distance
            double compensated = distance;
            
            // Compensate for ping (moving target)
            if (targetMoving && targetVelocity > 0.1) {
                // Estimate target movement during ping
                double pingSeconds = attackerPing / 1000.0;
                double targetMovement = targetVelocity * pingSeconds;
                compensated -= targetMovement;
            }
            
            // Compensate for attacker movement (sprinting)
            if (sprinting && attackerVelocity > 0.1) {
                double pingSeconds = attackerPing / 1000.0;
                double attackerMovement = attackerVelocity * pingSeconds;
                compensated += attackerMovement * 0.5; // Partial compensation
            }
            
            return Math.max(0, compensated);
        }
    }
    
    /**
     * Analyze reach patterns
     */
    public static ReachAnalysis analyze(List<HitEvent> hits) {
        if (hits.size() < 10) {
            return new ReachAnalysis();
        }
        
        ReachAnalysis analysis = new ReachAnalysis();
        
        // 1. Basic reach statistics
        analyzeReachStatistics(hits, analysis);
        
        // 2. Latency-compensated analysis
        analyzeCompensatedReach(hits, analysis);
        
        // 3. Consistency analysis
        analyzeReachConsistency(hits, analysis);
        
        // 4. Pattern detection
        analyzeReachPatterns(hits, analysis);
        
        // 5. Context correlation
        analyzeContextCorrelation(hits, analysis);
        
        // Calculate final confidence
        double confidence = calculateReachConfidence(analysis);
        analysis.setReachConfidence(confidence);
        
        return analysis;
    }
    
    /**
     * Basic reach statistics
     */
    private static void analyzeReachStatistics(List<HitEvent> hits, ReachAnalysis analysis) {
        double[] distances = hits.stream()
                .mapToDouble(HitEvent::getDistance)
                .toArray();
        
        double mean = MathUtil.mean(distances);
        double max = MathUtil.max(distances);
        double stdDev = MathUtil.standardDeviation(distances);
        
        analysis.setAverageReach(mean);
        analysis.setMaxReach(max);
        analysis.setReachStdDev(stdDev);
        
        // Count obvious violations
        long obviousViolations = hits.stream()
                .filter(h -> h.getDistance() > OBVIOUS_REACH)
                .count();
        
        analysis.setObviousViolations((int) obviousViolations);
        
        // Suspicious reach hits
        long suspiciousHits = hits.stream()
                .filter(h -> h.getDistance() > SUSPICIOUS_REACH && h.getDistance() <= OBVIOUS_REACH)
                .count();
        
        analysis.setSuspiciousHits((int) suspiciousHits);
    }
    
    /**
     * Latency-compensated analysis
     */
    private static void analyzeCompensatedReach(List<HitEvent> hits, ReachAnalysis analysis) {
        double[] compensated = hits.stream()
                .mapToDouble(HitEvent::getCompensatedReach)
                .toArray();
        
        double meanCompensated = MathUtil.mean(compensated);
        double maxCompensated = MathUtil.max(compensated);
        
        analysis.setAverageCompensatedReach(meanCompensated);
        analysis.setMaxCompensatedReach(maxCompensated);
        
        // Count violations after compensation
        long compensatedViolations = hits.stream()
                .filter(h -> h.getCompensatedReach() > MAX_LEGIT_REACH)
                .count();
        
        analysis.setCompensatedViolations((int) compensatedViolations);
        
        // Violation ratio
        double violationRatio = (double) compensatedViolations / hits.size();
        analysis.setViolationRatio(violationRatio);
    }
    
    /**
     * Consistency analysis
     * Reach hacks often hit at very consistent max range
     */
    private static void analyzeReachConsistency(List<HitEvent> hits, ReachAnalysis analysis) {
        // Count hits near maximum reach (2.9-3.0)
        long maxReachHits = hits.stream()
                .filter(h -> h.getDistance() >= 2.9 && h.getDistance() <= MAX_LEGIT_REACH + 0.1)
                .count();
        
        double maxReachRatio = (double) maxReachHits / hits.size();
        analysis.setMaxReachRatio(maxReachRatio);
        
        // Calculate reach variance
        double[] distances = hits.stream()
                .mapToDouble(HitEvent::getDistance)
                .toArray();
        
        double variance = MathUtil.variance(distances);
        analysis.setReachVariance(variance);
        
        // Low variance + high max reach ratio = suspicious
        if (variance < 0.05 && maxReachRatio > 0.6) {
            analysis.addAnomaly("Very consistent maximum reach hits");
        }
    }
    
    /**
     * Pattern detection
     */
    private static void analyzeReachPatterns(List<HitEvent> hits, ReachAnalysis analysis) {
        // Check if reach increases when sprinting
        List<HitEvent> sprintHits = hits.stream()
                .filter(HitEvent::isSprinting)
                .toList();
        
        List<HitEvent> walkHits = hits.stream()
                .filter(h -> !h.isSprinting())
                .toList();
        
        if (sprintHits.size() > 5 && walkHits.size() > 5) {
            double[] sprintDistances = sprintHits.stream()
                    .mapToDouble(HitEvent::getDistance)
                    .toArray();
            
            double[] walkDistances = walkHits.stream()
                    .mapToDouble(HitEvent::getDistance)
                    .toArray();
            
            double sprintMean = MathUtil.mean(sprintDistances);
            double walkMean = MathUtil.mean(walkDistances);
            
            // Reach should be slightly higher when sprinting (legit)
            // But if it's MUCH higher, that's suspicious
            double difference = sprintMean - walkMean;
            
            if (difference > 0.3) {
                analysis.addAnomaly(String.format("Unusually higher reach when sprinting (+%.2f blocks)", difference));
            }
        }
        
        // Check correlation with aim quality
        // Bad aim + max reach = more suspicious
        double[] yawDiffs = hits.stream()
                .mapToDouble(HitEvent::getYawDifference)
                .toArray();
        
        double avgYawDiff = MathUtil.mean(yawDiffs);
        
        if (avgYawDiff > 15.0 && analysis.getMaxReachRatio() > 0.7) {
            analysis.addAnomaly("High reach with poor aim alignment");
        }
    }
    
    /**
     * Context correlation
     */
    private static void analyzeContextCorrelation(List<HitEvent> hits, ReachAnalysis analysis) {
        // Analyze reach vs ping
        // High ping should correlate with more variance
        double[] pings = hits.stream()
                .mapToDouble(HitEvent::getAttackerPing)
                .toArray();
        
        double[] distances = hits.stream()
                .mapToDouble(HitEvent::getDistance)
                .toArray();
        
        if (pings.length > 10) {
            double correlation = MathUtil.correlation(pings, distances);
            analysis.setPingReachCorrelation(correlation);
            
            // No correlation with ping but consistent high reach = suspicious
            if (Math.abs(correlation) < 0.1 && analysis.getMaxReachRatio() > 0.6) {
                analysis.addAnomaly("Reach unaffected by network latency");
            }
        }
    }
    
    /**
     * Calculate final confidence
     */
    private static double calculateReachConfidence(ReachAnalysis analysis) {
        double confidence = 0.0;
        
        // Obvious violations
        if (analysis.getObviousViolations() > 0) {
            confidence += 0.5;
        }
        
        // Violation ratio after compensation
        if (analysis.getViolationRatio() > 0.3) {
            confidence += 0.3;
        } else if (analysis.getViolationRatio() > 0.1) {
            confidence += 0.15;
        }
        
        // Max reach ratio (consistent maximum reach)
        if (analysis.getMaxReachRatio() > 0.7) {
            confidence += 0.2;
        } else if (analysis.getMaxReachRatio() > 0.5) {
            confidence += 0.1;
        }
        
        // Low variance (too consistent)
        if (analysis.getReachVariance() < 0.05) {
            confidence += 0.15;
        }
        
        // Average reach very high
        if (analysis.getAverageReach() > 2.95) {
            confidence += 0.1;
        }
        
        // Anomalies detected
        confidence += Math.min(0.2, analysis.getAnomalies().size() * 0.1);
        
        return Math.min(1.0, confidence);
    }
    
    /**
     * Analysis result
     */
    @Data
    public static class ReachAnalysis {
        private double averageReach;
        private double maxReach;
        private double reachStdDev;
        private double reachVariance;
        
        private double averageCompensatedReach;
        private double maxCompensatedReach;
        
        private int obviousViolations;
        private int suspiciousHits;
        private int compensatedViolations;
        
        private double violationRatio;
        private double maxReachRatio;
        private double pingReachCorrelation;
        
        private double reachConfidence = 0.0;
        
        private List<String> anomalies = new ArrayList<>();
        
        public void addAnomaly(String anomaly) {
            anomalies.add(anomaly);
        }
        
        public String getConfidenceLevel() {
            if (reachConfidence >= HIGH_CONFIDENCE_THRESHOLD) {
                return "HIGH";
            } else if (reachConfidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
                return "MEDIUM";
            } else {
                return "LOW";
            }
        }
        
        public boolean isLikelyReach() {
            return reachConfidence >= HIGH_CONFIDENCE_THRESHOLD;
        }
        
        public boolean hasObviousViolations() {
            return obviousViolations > 0;
        }
    }
}
