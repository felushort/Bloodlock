package com.void.echo.detection;

import com.void.echo.util.MathUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Timer/Game Speed detection
 * 
 * Timer hacks modify the client's game speed, making everything faster:
 * - Movement speed increases
 * - Attack speed increases  
 * - Block breaking faster
 * - Overall actions per second increase
 * 
 * Detection methods:
 * 1. Packet rate analysis (packets/sec vs expected)
 * 2. Action density (actions within time windows)
 * 3. Movement speed vs tick rate
 * 4. Statistical outlier detection
 * 5. Consistency analysis (timer active for sustained periods)
 */
public class TimerDetector {
    
    private static final double NORMAL_TPS = 20.0;
    private static final double TIMER_THRESHOLD = 1.05; // 5% faster = suspicious
    private static final double OBVIOUS_TIMER = 1.15; // 15% faster = obvious
    
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.75;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.50;
    
    /**
     * Tick sample for analysis
     */
    @Data
    public static class TickSample {
        private final long timestamp;
        private final int packetCount;
        private final int actionCount;
        private final double movementSpeed;
        private final boolean moving;
        private final long tickDuration; // Milliseconds between ticks
    }
    
    /**
     * Analyze tick/timing patterns
     */
    public static TimerAnalysis analyze(List<TickSample> samples) {
        if (samples.size() < 20) {
            return new TimerAnalysis();
        }
        
        TimerAnalysis analysis = new TimerAnalysis();
        
        // 1. Tick rate analysis
        analyzeTickRate(samples, analysis);
        
        // 2. Packet rate analysis
        analyzePacketRate(samples, analysis);
        
        // 3. Action density
        analyzeActionDensity(samples, analysis);
        
        // 4. Movement speed analysis
        analyzeMovementSpeed(samples, analysis);
        
        // 5. Consistency check
        analyzeConsistency(samples, analysis);
        
        // 6. Burst detection
        analyzeBursts(samples, analysis);
        
        // Calculate final confidence
        double confidence = calculateTimerConfidence(analysis);
        analysis.setTimerConfidence(confidence);
        
        return analysis;
    }
    
    /**
     * Analyze tick rate
     * Normal: ~50ms between ticks (20 TPS)
     * Timer: <50ms between ticks
     */
    private static void analyzeTickRate(List<TickSample> samples, TimerAnalysis analysis) {
        double[] tickDurations = samples.stream()
                .mapToDouble(TickSample::getTickDuration)
                .filter(d -> d > 0)
                .toArray();
        
        if (tickDurations.length < 10) return;
        
        double avgTickDuration = MathUtil.mean(tickDurations);
        double expectedTickDuration = 1000.0 / NORMAL_TPS; // 50ms
        
        analysis.setAverageTickDuration(avgTickDuration);
        analysis.setExpectedTickDuration(expectedTickDuration);
        
        // Calculate effective TPS
        double effectiveTPS = 1000.0 / avgTickDuration;
        analysis.setEffectiveTPS(effectiveTPS);
        
        // Timer multiplier
        double timerMultiplier = effectiveTPS / NORMAL_TPS;
        analysis.setTimerMultiplier(timerMultiplier);
        
        if (timerMultiplier > OBVIOUS_TIMER) {
            analysis.addAnomaly(String.format("Obvious timer: %.2fx game speed", timerMultiplier));
        } else if (timerMultiplier > TIMER_THRESHOLD) {
            analysis.addAnomaly(String.format("Suspicious tick rate: %.2fx game speed", timerMultiplier));
        }
    }
    
    /**
     * Analyze packet rate
     * Timer increases packets per second
     */
    private static void analyzePacketRate(List<TickSample> samples, TimerAnalysis analysis) {
        double[] packetCounts = samples.stream()
                .mapToDouble(TickSample::getPacketCount)
                .toArray();
        
        double avgPackets = MathUtil.mean(packetCounts);
        double maxPackets = MathUtil.max(packetCounts);
        double stdDev = MathUtil.standardDeviation(packetCounts);
        
        analysis.setAveragePacketsPerTick(avgPackets);
        analysis.setMaxPacketsPerTick(maxPackets);
        
        // Detect spikes (outliers)
        int spikes = 0;
        for (double count : packetCounts) {
            if (count > avgPackets + 3 * stdDev) {
                spikes++;
            }
        }
        
        analysis.setPacketSpikes(spikes);
        
        // Very high sustained packet rate
        if (avgPackets > 15) {
            analysis.addAnomaly("Abnormally high packet rate");
        }
    }
    
    /**
     * Analyze action density
     * Timer allows more actions in same time period
     */
    private static void analyzeActionDensity(List<TickSample> samples, TimerAnalysis analysis) {
        double[] actionCounts = samples.stream()
                .mapToDouble(TickSample::getActionCount)
                .toArray();
        
        double avgActions = MathUtil.mean(actionCounts);
        double maxActions = MathUtil.max(actionCounts);
        
        analysis.setAverageActionsPerTick(avgActions);
        analysis.setMaxActionsPerTick(maxActions);
        
        // Calculate actions per second
        double avgTickDuration = analysis.getAverageTickDuration();
        if (avgTickDuration > 0) {
            double actionsPerSecond = (avgActions * 1000.0) / avgTickDuration;
            analysis.setActionsPerSecond(actionsPerSecond);
            
            // Superhuman action rate
            if (actionsPerSecond > 25) {
                analysis.addAnomaly(String.format("Superhuman action rate: %.1f actions/sec", actionsPerSecond));
            }
        }
    }
    
    /**
     * Analyze movement speed
     * Timer makes movement faster relative to tick rate
     */
    private static void analyzeMovementSpeed(List<TickSample> samples, TimerAnalysis analysis) {
        double[] speeds = samples.stream()
                .filter(TickSample::isMoving)
                .mapToDouble(TickSample::getMovementSpeed)
                .toArray();
        
        if (speeds.length < 5) return;
        
        double avgSpeed = MathUtil.mean(speeds);
        double maxSpeed = MathUtil.max(speeds);
        
        analysis.setAverageMovementSpeed(avgSpeed);
        analysis.setMaxMovementSpeed(maxSpeed);
        
        // Normal sprint speed: ~0.28 blocks/tick
        // Timer makes this higher
        if (avgSpeed > 0.35) {
            analysis.addAnomaly(String.format("Abnormal movement speed: %.3f blocks/tick", avgSpeed));
        }
    }
    
    /**
     * Analyze consistency
     * Timer is usually active for sustained periods
     */
    private static void analyzeConsistency(List<TickSample> samples, TimerAnalysis analysis) {
        // Calculate tick duration variance
        double[] tickDurations = samples.stream()
                .mapToDouble(TickSample::getTickDuration)
                .filter(d -> d > 0)
                .toArray();
        
        if (tickDurations.length < 10) return;
        
        double variance = MathUtil.variance(tickDurations);
        double stdDev = MathUtil.standardDeviation(tickDurations);
        double mean = MathUtil.mean(tickDurations);
        
        double cv = mean > 0 ? stdDev / mean : 0;
        
        analysis.setTickDurationVariance(variance);
        analysis.setTickDurationCV(cv);
        
        // Very consistent tick duration below 50ms = timer
        if (mean < 47 && cv < 0.15) {
            analysis.addAnomaly("Consistently fast tick rate");
        }
    }
    
    /**
     * Detect bursts
     * Some timer hacks activate in bursts
     */
    private static void analyzeBursts(List<TickSample> samples, TimerAnalysis analysis) {
        // Find windows where tick rate spikes
        int burstCount = 0;
        int windowSize = 5;
        
        for (int i = 0; i <= samples.size() - windowSize; i++) {
            double[] window = new double[windowSize];
            for (int j = 0; j < windowSize; j++) {
                window[j] = samples.get(i + j).getTickDuration();
            }
            
            double windowMean = MathUtil.mean(window);
            
            // Window significantly faster than normal
            if (windowMean < 40) { // <40ms = >25 TPS
                burstCount++;
            }
        }
        
        double burstRatio = (double) burstCount / (samples.size() - windowSize + 1);
        analysis.setBurstRatio(burstRatio);
        
        if (burstRatio > 0.3) {
            analysis.addAnomaly("Frequent timer bursts detected");
        }
    }
    
    /**
     * Calculate final confidence
     */
    private static double calculateTimerConfidence(TimerAnalysis analysis) {
        double confidence = 0.0;
        
        // Timer multiplier (primary indicator)
        if (analysis.getTimerMultiplier() > OBVIOUS_TIMER) {
            confidence += 0.5;
        } else if (analysis.getTimerMultiplier() > TIMER_THRESHOLD) {
            confidence += 0.3 * ((analysis.getTimerMultiplier() - TIMER_THRESHOLD) / (OBVIOUS_TIMER - TIMER_THRESHOLD));
        }
        
        // Movement speed
        if (analysis.getAverageMovementSpeed() > 0.35) {
            confidence += 0.2;
        } else if (analysis.getAverageMovementSpeed() > 0.30) {
            confidence += 0.1;
        }
        
        // Actions per second
        if (analysis.getActionsPerSecond() > 25) {
            confidence += 0.2;
        } else if (analysis.getActionsPerSecond() > 20) {
            confidence += 0.1;
        }
        
        // Consistency (low CV with fast ticks)
        if (analysis.getTickDurationCV() < 0.15 && analysis.getAverageTickDuration() < 47) {
            confidence += 0.15;
        }
        
        // Bursts
        if (analysis.getBurstRatio() > 0.3) {
            confidence += 0.1;
        }
        
        // Anomalies
        confidence += Math.min(0.2, analysis.getAnomalies().size() * 0.1);
        
        return Math.min(1.0, confidence);
    }
    
    /**
     * Analysis result
     */
    @Data
    public static class TimerAnalysis {
        private double averageTickDuration;
        private double expectedTickDuration;
        private double effectiveTPS;
        private double timerMultiplier = 1.0;
        
        private double averagePacketsPerTick;
        private double maxPacketsPerTick;
        private int packetSpikes;
        
        private double averageActionsPerTick;
        private double maxActionsPerTick;
        private double actionsPerSecond;
        
        private double averageMovementSpeed;
        private double maxMovementSpeed;
        
        private double tickDurationVariance;
        private double tickDurationCV;
        
        private double burstRatio;
        
        private double timerConfidence = 0.0;
        
        private List<String> anomalies = new ArrayList<>();
        
        public void addAnomaly(String anomaly) {
            anomalies.add(anomaly);
        }
        
        public String getConfidenceLevel() {
            if (timerConfidence >= HIGH_CONFIDENCE_THRESHOLD) {
                return "HIGH";
            } else if (timerConfidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
                return "MEDIUM";
            } else {
                return "LOW";
            }
        }
        
        public boolean isLikelyTimer() {
            return timerConfidence >= HIGH_CONFIDENCE_THRESHOLD;
        }
        
        public boolean isObviousTimer() {
            return timerMultiplier > OBVIOUS_TIMER;
        }
    }
}
