package com.void.echo.ml;

import com.void.echo.util.MathUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Behavioral fingerprinting using feature extraction
 * 
 * Creates unique behavioral signatures for players based on:
 * - Movement patterns
 * - Aim characteristics
 * - Click patterns
 * - Temporal dynamics
 * - Spectral signatures
 * 
 * These fingerprints can be used for:
 * - Identity verification across sessions
 * - Detecting account sharing
 * - Identifying behavioral changes (toggling)
 */
public class BehavioralFingerprint {
    
    /**
     * Feature vector representing a player's unique behavior
     */
    @Data
    public static class FeatureVector {
        // Movement features (32 dimensions)
        private double[] movementSpeedDistribution;      // 8 bins
        private double[] accelerationPattern;            // 8 bins
        private double[] directionChangeFrequency;       // 8 bins
        private double[] sprintTogglePattern;            // 8 bins
        
        // Aim features (32 dimensions)
        private double[] yawDeltaDistribution;           // 8 bins
        private double[] pitchDeltaDistribution;         // 8 bins
        private double[] microJitterSpectrum;            // 8 bins (FFT)
        private double[] aimSmoothness;                  // 8 bins
        
        // Click features (16 dimensions)
        private double[] clickIntervalDistribution;      // 8 bins
        private double[] burstPattern;                   // 8 bins
        
        // Temporal features (16 dimensions)
        private double[] reactionTimeDistribution;       // 8 bins
        private double[] fatiguePattern;                 // 8 bins (performance over time)
        
        // Statistical features (16 dimensions)
        private double avgMouseSensitivity;
        private double aimConsistency;
        private double clickConsistency;
        private double combatAggression;
        private double movementEntropy;
        private double aimEntropy;
        private double clickEntropy;
        private double sessionStability;
        private double performanceVariance;
        private double adaptabilityScore;
        private double microAdjustmentRate;
        private double overcompensationFrequency;
        private double hesitationMetric;
        private double confidenceMetric;
        private double rhythmScore;
        private double naturalness;
        
        // Hardware-related (8 dimensions)
        private double estimatedDPI;
        private double estimatedPollingRate;
        private double inputLatencyEstimate;
        private double frameTimeVariance;
        private double[] dpiSignature;                   // 4 bins
        
        /**
         * Get total dimensionality
         */
        public int getDimensions() {
            return 32 + 32 + 16 + 16 + 16 + 8; // 120 dimensions
        }
        
        /**
         * Convert to flat array for distance calculations
         */
        public double[] toArray() {
            List<Double> features = new ArrayList<>();
            
            // Add all distribution features
            addArray(features, movementSpeedDistribution);
            addArray(features, accelerationPattern);
            addArray(features, directionChangeFrequency);
            addArray(features, sprintTogglePattern);
            addArray(features, yawDeltaDistribution);
            addArray(features, pitchDeltaDistribution);
            addArray(features, microJitterSpectrum);
            addArray(features, aimSmoothness);
            addArray(features, clickIntervalDistribution);
            addArray(features, burstPattern);
            addArray(features, reactionTimeDistribution);
            addArray(features, fatiguePattern);
            addArray(features, dpiSignature);
            
            // Add statistical features
            features.add(avgMouseSensitivity);
            features.add(aimConsistency);
            features.add(clickConsistency);
            features.add(combatAggression);
            features.add(movementEntropy);
            features.add(aimEntropy);
            features.add(clickEntropy);
            features.add(sessionStability);
            features.add(performanceVariance);
            features.add(adaptabilityScore);
            features.add(microAdjustmentRate);
            features.add(overcompensationFrequency);
            features.add(hesitationMetric);
            features.add(confidenceMetric);
            features.add(rhythmScore);
            features.add(naturalness);
            
            // Add hardware features
            features.add(estimatedDPI);
            features.add(estimatedPollingRate);
            features.add(inputLatencyEstimate);
            features.add(frameTimeVariance);
            
            return features.stream().mapToDouble(Double::doubleValue).toArray();
        }
        
        private void addArray(List<Double> list, double[] array) {
            if (array != null) {
                for (double v : array) {
                    list.add(v);
                }
            } else {
                // Add zeros if array is null
                for (int i = 0; i < 8; i++) {
                    list.add(0.0);
                }
            }
        }
    }
    
    /**
     * Calculate similarity between two fingerprints
     * Returns value from 0 (completely different) to 1 (identical)
     */
    public static double calculateSimilarity(FeatureVector fp1, FeatureVector fp2) {
        double[] vec1 = fp1.toArray();
        double[] vec2 = fp2.toArray();
        
        if (vec1.length != vec2.length) {
            return 0.0;
        }
        
        // Use multiple similarity metrics and combine them
        
        // 1. Cosine similarity
        double cosineSim = cosineSimilarity(vec1, vec2);
        
        // 2. Euclidean distance (normalized)
        double euclideanSim = 1.0 / (1.0 + euclideanDistance(vec1, vec2));
        
        // 3. Correlation coefficient
        double correlation = MathUtil.correlation(vec1, vec2);
        double correlationSim = (correlation + 1.0) / 2.0; // Normalize to 0-1
        
        // 4. Manhattan distance (normalized)
        double manhattanSim = 1.0 / (1.0 + manhattanDistance(vec1, vec2));
        
        // Weighted combination
        double similarity = 
            0.35 * cosineSim +
            0.25 * euclideanSim +
            0.25 * correlationSim +
            0.15 * manhattanSim;
        
        return Math.max(0.0, Math.min(1.0, similarity));
    }
    
    /**
     * Cosine similarity between vectors
     */
    private static double cosineSimilarity(double[] vec1, double[] vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator > 0 ? dotProduct / denominator : 0.0;
    }
    
    /**
     * Euclidean distance between vectors
     */
    private static double euclideanDistance(double[] vec1, double[] vec2) {
        double sumSquares = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            double diff = vec1[i] - vec2[i];
            sumSquares += diff * diff;
        }
        
        return Math.sqrt(sumSquares);
    }
    
    /**
     * Manhattan distance between vectors
     */
    private static double manhattanDistance(double[] vec1, double[] vec2) {
        double sum = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            sum += Math.abs(vec1[i] - vec2[i]);
        }
        
        return sum;
    }
    
    /**
     * Detect if fingerprint has changed significantly (toggling detection)
     * Returns confidence that behavior has changed (0-1)
     */
    public static double detectBehavioralChange(FeatureVector baseline, FeatureVector current) {
        double similarity = calculateSimilarity(baseline, current);
        
        // Calculate change score (inverse of similarity)
        double changeScore = 1.0 - similarity;
        
        // Additional checks for specific dramatic changes
        
        // 1. Dramatic aim consistency change
        double aimConsistencyChange = Math.abs(baseline.getAimConsistency() - current.getAimConsistency());
        if (aimConsistencyChange > 0.3) {
            changeScore += 0.2;
        }
        
        // 2. Click pattern change
        double clickConsistencyChange = Math.abs(baseline.getClickConsistency() - current.getClickConsistency());
        if (clickConsistencyChange > 0.3) {
            changeScore += 0.2;
        }
        
        // 3. Naturalness change
        double naturalnessChange = Math.abs(baseline.getNaturalness() - current.getNaturalness());
        if (naturalnessChange > 0.4) {
            changeScore += 0.3;
        }
        
        return Math.min(1.0, changeScore);
    }
    
    /**
     * Create histogram from data
     */
    public static double[] createHistogram(double[] data, int bins, double min, double max) {
        double[] histogram = new double[bins];
        
        if (data.length == 0) return histogram;
        
        double binWidth = (max - min) / bins;
        
        for (double value : data) {
            if (value >= min && value <= max) {
                int binIndex = (int) Math.min(bins - 1, (value - min) / binWidth);
                histogram[binIndex]++;
            }
        }
        
        // Normalize
        double sum = 0.0;
        for (double count : histogram) {
            sum += count;
        }
        
        if (sum > 0) {
            for (int i = 0; i < histogram.length; i++) {
                histogram[i] /= sum;
            }
        }
        
        return histogram;
    }
    
    /**
     * Estimate DPI from micro-movements
     */
    public static double estimateDPI(double[] microMovements) {
        if (microMovements.length < 10) return 800.0; // Default assumption
        
        // Calculate typical micro-movement size
        double avgMicroMovement = MathUtil.mean(microMovements);
        
        // Typical correlation: higher DPI = smaller pixel movements
        // This is a rough heuristic
        if (avgMicroMovement < 0.5) {
            return 1600.0; // High DPI
        } else if (avgMicroMovement < 1.0) {
            return 1200.0;
        } else if (avgMicroMovement < 2.0) {
            return 800.0; // Standard DPI
        } else {
            return 400.0; // Low DPI
        }
    }
    
    /**
     * Calculate behavioral naturalness score
     * Combines multiple metrics to assess if behavior appears human
     */
    public static double calculateNaturalness(FeatureVector features) {
        double score = 100.0;
        
        // Penalize perfect consistency (unnatural)
        if (features.getAimConsistency() > 0.95) {
            score -= 20.0;
        }
        
        if (features.getClickConsistency() > 0.95) {
            score -= 20.0;
        }
        
        // Reward high entropy
        if (features.getAimEntropy() < 0.3) {
            score -= 15.0;
        }
        
        if (features.getClickEntropy() < 0.3) {
            score -= 15.0;
        }
        
        // Reward micro-adjustments (humans constantly adjust)
        if (features.getMicroAdjustmentRate() < 0.1) {
            score -= 10.0;
        }
        
        // Reward natural hesitation patterns
        if (features.getHesitationMetric() < 0.05) {
            score -= 10.0;
        }
        
        return Math.max(0.0, Math.min(100.0, score)) / 100.0;
    }
}
