package com.void.echo.analysis;

import com.void.echo.EchoPlugin;
import com.void.echo.data.BehavioralBuffer;
import com.void.echo.data.ClickData;
import com.void.echo.data.PlayerDataManager;
import com.void.echo.data.RotationData;
import com.void.echo.detection.AimbotDetector;
import com.void.echo.detection.AutoclickerDetector;
import com.void.echo.util.MathUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.DoubleStream;

/**
 * Layer 2: Human Entropy Modeling (Enhanced)
 * 
 * Analyzes behavioral data to determine if it exhibits natural human characteristics:
 * - Inconsistent acceleration
 * - Slight overcorrection
 * - Imperfect micro jitter
 * - Non-linear deceleration
 * - Natural variance over time
 * 
 * Enhanced with advanced detection algorithms:
 * - Aimbot detection via curvature and spectral analysis
 * - Autoclicker detection via pattern and entropy analysis
 * - Multi-dimensional entropy calculations
 * 
 * Calculates a "Human Probability Score" from 0-100.
 */
public class EntropyAnalyzer {
    
    private final EchoPlugin plugin;
    private final PlayerDataManager dataManager;
    
    // Configuration
    private final int minSamples;
    private final double rotationConsistencyThreshold;
    private final double cpsVarianceMin;
    private final double reactionTimeStdDevMin;
    
    // Weights for final score
    private final double yawWeight;
    private final double pitchWeight;
    private final double clickWeight;
    private final double reactionWeight;
    private final double alignmentWeight;
    
    public EntropyAnalyzer(EchoPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        
        // Load configuration
        this.minSamples = plugin.getConfigManager().getInt("entropy.min-samples", 50);
        this.rotationConsistencyThreshold = plugin.getConfigManager().getDouble("entropy.thresholds.rotation-consistency", 0.85);
        this.cpsVarianceMin = plugin.getConfigManager().getDouble("entropy.thresholds.cps-variance-min", 0.15);
        this.reactionTimeStdDevMin = plugin.getConfigManager().getDouble("entropy.thresholds.reaction-time-stdev-min", 25.0);
        
        // Load weights
        this.yawWeight = plugin.getConfigManager().getDouble("entropy.weights.yaw-entropy", 0.20);
        this.pitchWeight = plugin.getConfigManager().getDouble("entropy.weights.pitch-entropy", 0.15);
        this.clickWeight = plugin.getConfigManager().getDouble("entropy.weights.click-distribution", 0.20);
        this.reactionWeight = plugin.getConfigManager().getDouble("entropy.weights.reaction-time", 0.25);
        this.alignmentWeight = plugin.getConfigManager().getDouble("entropy.weights.crosshair-alignment", 0.20);
    }
    
    /**
     * Analyze player's behavioral buffer
     * Returns null if insufficient data
     */
    public EntropyAnalysisResult analyze(UUID playerId) {
        BehavioralBuffer buffer = dataManager.getBuffer(playerId);
        
        // Get recent data (last 30 seconds)
        List<RotationData> rotations = buffer.getRecentRotations(30000);
        List<ClickData> clicks = buffer.getRecentClicks(30000);
        
        // Check if we have enough samples
        if (rotations.size() < minSamples && clicks.size() < minSamples / 2) {
            return null; // Not enough data
        }
        
        EntropyAnalysisResult result = new EntropyAnalysisResult();
        result.setRotationSamples(rotations.size());
        result.setClickSamples(clicks.size());
        
        // Analyze rotation entropy
        if (rotations.size() >= minSamples) {
            analyzeRotationEntropy(rotations, result);
        } else {
            // Default to neutral score if not enough rotation data
            result.setYawEntropyScore(50.0);
            result.setPitchEntropyScore(50.0);
        }
        
        // Analyze click patterns
        if (clicks.size() >= minSamples / 2) {
            analyzeClickPatterns(clicks, result);
            analyzeReactionTimes(clicks, result);
            analyzeCrosshairAlignment(clicks, result);
            
            // Run advanced autoclicker detection
            AutoclickerDetector.AutoclickerAnalysis autoclickerAnalysis = 
                AutoclickerDetector.analyze(clicks);
            result.setAutoclickerAnalysis(autoclickerAnalysis);
        } else {
            // Default to neutral scores
            result.setClickDistributionScore(50.0);
            result.setReactionTimeScore(50.0);
            result.setCrosshairAlignmentScore(50.0);
        }
        
        // Run advanced aimbot detection on rotations
        if (rotations.size() >= minSamples) {
            AimbotDetector.AimbotAnalysis aimbotAnalysis = 
                AimbotDetector.analyze(rotations);
            result.setAimbotAnalysis(aimbotAnalysis);
            
            // Calculate advanced entropy metrics
            double[] yawDeltas = rotations.stream()
                .filter(RotationData::isSignificant)
                .mapToDouble(RotationData::getYawDelta)
                .toArray();
            
            if (yawDeltas.length >= 10) {
                // Permutation entropy
                double permEntropy = MathUtil.permutationEntropy(yawDeltas, 3);
                result.setPermutationEntropy(permEntropy);
                
                // Sample entropy
                double stdDev = MathUtil.standardDeviation(yawDeltas);
                double sampEntropy = MathUtil.sampleEntropy(yawDeltas, 2, 0.2 * stdDev);
                result.setSampleEntropy(sampEntropy);
                
                // Spectral flatness
                double spectral = MathUtil.spectralFlatness(yawDeltas);
                result.setSpectralFlatness(spectral);
                
                // Hurst exponent
                double hurst = MathUtil.hurstExponent(yawDeltas);
                result.setHurstExponent(hurst);
            }
        }
        
        // Calculate final score
        result.calculateFinalScore(yawWeight, pitchWeight, clickWeight, reactionWeight, alignmentWeight);
        
        return result;
    }
    
    /**
     * Analyze rotation entropy (yaw and pitch)
     * 
     * Humans have natural inconsistency in their mouse movements.
     * Aimbots tend to have artificial smoothness or mathematical patterns.
     */
    private void analyzeRotationEntropy(List<RotationData> rotations, EntropyAnalysisResult result) {
        // Extract significant rotations only
        List<RotationData> significant = rotations.stream()
                .filter(RotationData::isSignificant)
                .toList();
        
        if (significant.isEmpty()) {
            result.setYawEntropyScore(100.0);
            result.setPitchEntropyScore(100.0);
            return;
        }
        
        // Analyze yaw deltas
        double[] yawDeltas = significant.stream()
                .mapToDouble(RotationData::getYawDelta)
                .map(Math::abs)
                .toArray();
        
        // Analyze pitch deltas
        double[] pitchDeltas = significant.stream()
                .mapToDouble(RotationData::getPitchDelta)
                .map(Math::abs)
                .toArray();
        
        // Calculate yaw metrics
        double yawStdDev = MathUtil.standardDeviation(yawDeltas);
        double yawMean = MathUtil.mean(yawDeltas);
        double yawEntropy = MathUtil.entropy(yawDeltas);
        double yawCV = yawMean > 0 ? (yawStdDev / yawMean) : 0.0;
        
        // Calculate pitch metrics
        double pitchStdDev = MathUtil.standardDeviation(pitchDeltas);
        double pitchMean = MathUtil.mean(pitchDeltas);
        double pitchEntropy = MathUtil.entropy(pitchDeltas);
        double pitchCV = pitchMean > 0 ? (pitchStdDev / pitchMean) : 0.0;
        
        // Check for unnatural consistency
        // Low CV = too consistent = bot-like
        // High CV = natural variance = human-like
        
        // Yaw entropy score
        double yawScore = 100.0;
        if (yawCV < 0.3) {
            // Very low variance = suspicious
            yawScore = MathUtil.normalize(yawCV, 0.0, 0.3) * 0.4; // Max 40% if too consistent
        } else if (yawCV < 0.6) {
            // Moderate variance = slightly suspicious
            yawScore = 40.0 + MathUtil.normalize(yawCV, 0.3, 0.6) * 40.0; // 40-80%
        } else {
            // High variance = good
            yawScore = 80.0 + Math.min(20.0, (yawCV - 0.6) * 50.0); // 80-100%
        }
        
        // Pitch entropy score (similar logic)
        double pitchScore = 100.0;
        if (pitchCV < 0.3) {
            pitchScore = MathUtil.normalize(pitchCV, 0.0, 0.3) * 0.4;
        } else if (pitchCV < 0.6) {
            pitchScore = 40.0 + MathUtil.normalize(pitchCV, 0.3, 0.6) * 40.0;
        } else {
            pitchScore = 80.0 + Math.min(20.0, (pitchCV - 0.6) * 50.0);
        }
        
        result.setYawEntropyScore(yawScore);
        result.setPitchEntropyScore(pitchScore);
        result.setRotationConsistency(1.0 - yawCV); // Lower is better for humans
    }
    
    /**
     * Analyze click patterns
     * 
     * Humans have natural variance in clicking speed.
     * Autoclickers have mathematical precision.
     */
    private void analyzeClickPatterns(List<ClickData> clicks, EntropyAnalysisResult result) {
        // Extract click intervals (milliseconds between clicks)
        double[] intervals = clicks.stream()
                .mapToDouble(ClickData::getTimeSinceLastClick)
                .filter(x -> x > 0) // Skip first click
                .toArray();
        
        if (intervals.length < 5) {
            result.setClickDistributionScore(50.0);
            return;
        }
        
        // Calculate variance
        double stdDev = MathUtil.standardDeviation(intervals);
        double mean = MathUtil.mean(intervals);
        double cv = mean > 0 ? (stdDev / mean) : 0.0;
        
        result.setCpsVariance(cv);
        
        // Score based on variance
        // Too consistent = autoclicker
        // Natural variance = human
        double clickScore = 100.0;
        
        if (cv < cpsVarianceMin) {
            // Very consistent = very suspicious
            clickScore = MathUtil.normalize(cv, 0.0, cpsVarianceMin) * 30.0;
        } else if (cv < 0.3) {
            clickScore = 30.0 + MathUtil.normalize(cv, cpsVarianceMin, 0.3) * 50.0;
        } else {
            clickScore = 80.0 + Math.min(20.0, (cv - 0.3) * 40.0);
        }
        
        result.setClickDistributionScore(clickScore);
    }
    
    /**
     * Analyze reaction times
     * 
     * Humans have variance in reaction time.
     * Aimbots react with machine precision.
     */
    private void analyzeReactionTimes(List<ClickData> clicks, EntropyAnalysisResult result) {
        // For each hit, estimate reaction time
        // (This is simplified - in production would correlate with rotation packets)
        
        List<ClickData> hits = clicks.stream()
                .filter(ClickData::wasHit)
                .toList();
        
        if (hits.size() < 5) {
            result.setReactionTimeScore(50.0);
            return;
        }
        
        // Use click intervals as proxy for reaction times
        double[] reactionTimes = hits.stream()
                .mapToDouble(ClickData::getTimeSinceLastClick)
                .filter(x -> x > 0 && x < 1000) // Between 0-1000ms
                .toArray();
        
        if (reactionTimes.length < 5) {
            result.setReactionTimeScore(50.0);
            return;
        }
        
        double stdDev = MathUtil.standardDeviation(reactionTimes);
        result.setReactionTimeStdDev(stdDev);
        
        // Low standard deviation = too consistent = suspicious
        double reactionScore = 100.0;
        
        if (stdDev < reactionTimeStdDevMin) {
            // Too consistent
            reactionScore = MathUtil.normalize(stdDev, 0.0, reactionTimeStdDevMin) * 40.0;
        } else if (stdDev < 60.0) {
            reactionScore = 40.0 + MathUtil.normalize(stdDev, reactionTimeStdDevMin, 60.0) * 40.0;
        } else {
            reactionScore = 80.0 + Math.min(20.0, (stdDev - 60.0) * 0.5);
        }
        
        result.setReactionTimeScore(reactionScore);
    }
    
    /**
     * Analyze crosshair alignment
     * 
     * Humans rarely have perfect crosshair alignment on every hit.
     * Aimbots snap perfectly to targets.
     */
    private void analyzeCrosshairAlignment(List<ClickData> clicks, EntropyAnalysisResult result) {
        List<ClickData> hits = clicks.stream()
                .filter(ClickData::wasHit)
                .toList();
        
        if (hits.isEmpty()) {
            result.setCrosshairAlignmentScore(50.0);
            return;
        }
        
        // Calculate how many hits had very high alignment (> 0.95)
        long perfectHits = hits.stream()
                .filter(c -> c.getYawAlignment() > 0.95)
                .count();
        
        double perfectRatio = (double) perfectHits / hits.size();
        result.setAimedClickRatio(perfectRatio);
        
        // High perfect ratio = suspicious
        double alignmentScore = 100.0;
        
        if (perfectRatio > 0.8) {
            // More than 80% perfect = very suspicious
            alignmentScore = (1.0 - perfectRatio) * 100.0;
        } else if (perfectRatio > 0.5) {
            // 50-80% perfect = moderately suspicious
            alignmentScore = 20.0 + (0.8 - perfectRatio) / 0.3 * 60.0;
        } else {
            // Less than 50% perfect = normal
            alignmentScore = 80.0 + (0.5 - perfectRatio) * 40.0;
        }
        
        result.setCrosshairAlignmentScore(alignmentScore);
    }
}
