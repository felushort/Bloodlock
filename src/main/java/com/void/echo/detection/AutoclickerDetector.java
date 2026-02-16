package com.void.echo.detection;

import com.void.echo.data.ClickData;
import com.void.echo.util.MathUtil;
import lombok.Data;

import java.util.List;

/**
 * Advanced autoclicker detection using statistical analysis
 * 
 * Detection methods:
 * 1. Interval consistency (autoclickers have very consistent timing)
 * 2. Burst pattern analysis (human bursts vs mechanical bursts)
 * 3. Periodicity detection (regular click patterns)
 * 4. Statistical distribution analysis
 * 5. Clustering coefficient
 * 6. Long-range dependence analysis
 */
public class AutoclickerDetector {
    
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.75;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.50;
    
    /**
     * Analyze click data for autoclicker signatures
     */
    public static AutoclickerAnalysis analyze(List<ClickData> clicks) {
        if (clicks.size() < 20) {
            return new AutoclickerAnalysis();
        }
        
        AutoclickerAnalysis analysis = new AutoclickerAnalysis();
        
        // Extract click intervals
        double[] intervals = clicks.stream()
                .mapToDouble(ClickData::getTimeSinceLastClick)
                .filter(x -> x > 0 && x < 5000) // Filter out unrealistic intervals
                .toArray();
        
        if (intervals.length < 10) {
            return analysis;
        }
        
        // 1. Interval consistency analysis
        double consistency = analyzeConsistency(intervals);
        analysis.setConsistencyScore(consistency);
        
        // 2. CPS (Clicks Per Second) analysis
        double cps = 1000.0 / MathUtil.mean(intervals);
        analysis.setAverageCPS(cps);
        
        // 3. Variance analysis
        double variance = MathUtil.variance(intervals);
        double stdDev = MathUtil.standardDeviation(intervals);
        double cv = stdDev / MathUtil.mean(intervals);
        analysis.setCoefficientOfVariation(cv);
        
        // 4. Burst detection
        double burstScore = analyzeBurstPatterns(intervals);
        analysis.setBurstScore(burstScore);
        
        // 5. Periodicity detection
        double periodicity = detectPeriodicity(intervals);
        analysis.setPeriodicityScore(periodicity);
        
        // 6. Distribution analysis
        double distributionAnomaly = analyzeDistribution(intervals);
        analysis.setDistributionAnomalyScore(distributionAnomaly);
        
        // 7. Clustering analysis
        double clustering = MathUtil.clickClusterCoefficient(intervals, 5);
        analysis.setClusteringScore(clustering);
        
        // 8. Entropy analysis
        double entropy = MathUtil.entropy(intervals);
        analysis.setEntropyScore(entropy);
        
        // 9. Sample entropy (complexity measure)
        double sampleEntropy = MathUtil.sampleEntropy(intervals, 2, 0.2 * stdDev);
        analysis.setSampleEntropy(sampleEntropy);
        
        // 10. Permutation entropy
        double permEntropy = MathUtil.permutationEntropy(intervals, 3);
        analysis.setPermutationEntropy(permEntropy);
        
        // 11. Hurst exponent (long-range dependence)
        double hurst = MathUtil.hurstExponent(intervals);
        analysis.setHurstExponent(hurst);
        
        // 12. Spectral analysis
        double spectralFlatness = MathUtil.spectralFlatness(intervals);
        analysis.setSpectralFlatness(spectralFlatness);
        
        // Calculate final confidence
        double confidence = calculateAutoclickerConfidence(analysis);
        analysis.setAutoclickerConfidence(confidence);
        
        return analysis;
    }
    
    /**
     * Analyze interval consistency
     * Autoclickers have very low variance in click intervals
     */
    private static double analyzeConsistency(double[] intervals) {
        if (intervals.length < 5) return 0.0;
        
        double mean = MathUtil.mean(intervals);
        double stdDev = MathUtil.standardDeviation(intervals);
        
        if (mean < 1.0) return 0.0;
        
        double cv = stdDev / mean;
        
        // Very low CV = very consistent = suspicious
        // Human CV typically > 0.15
        // Autoclicker CV typically < 0.05
        
        if (cv < 0.05) {
            return 1.0;
        } else if (cv < 0.15) {
            return (0.15 - cv) / 0.10;
        }
        
        return 0.0;
    }
    
    /**
     * Analyze burst patterns
     * Humans have natural burst clusters
     * Autoclickers have mechanical burst patterns
     */
    private static double analyzeBurstPatterns(double[] intervals) {
        if (intervals.length < 10) return 0.0;
        
        // Detect bursts (intervals significantly below mean)
        double mean = MathUtil.mean(intervals);
        double threshold = mean * 0.6; // 60% of mean
        
        int burstLength = 0;
        int maxBurstLength = 0;
        int totalBursts = 0;
        
        for (double interval : intervals) {
            if (interval < threshold) {
                burstLength++;
            } else {
                if (burstLength > 0) {
                    totalBursts++;
                    if (burstLength > maxBurstLength) {
                        maxBurstLength = burstLength;
                    }
                }
                burstLength = 0;
            }
        }
        
        // Very long consistent bursts are suspicious
        // Humans typically don't maintain perfect bursts > 10 clicks
        if (maxBurstLength > 15) {
            return 0.8;
        } else if (maxBurstLength > 10) {
            return 0.4;
        }
        
        // Analyze burst interval consistency
        if (totalBursts > 2) {
            // Check if burst intervals are suspiciously consistent
            java.util.List<Double> burstIntervals = new java.util.ArrayList<>();
            boolean inBurst = false;
            
            for (double interval : intervals) {
                if (interval < threshold) {
                    if (inBurst) {
                        burstIntervals.add(interval);
                    }
                    inBurst = true;
                } else {
                    inBurst = false;
                }
            }
            
            if (burstIntervals.size() > 5) {
                double[] burstArray = burstIntervals.stream().mapToDouble(Double::doubleValue).toArray();
                double burstStdDev = MathUtil.standardDeviation(burstArray);
                double burstMean = MathUtil.mean(burstArray);
                
                if (burstMean > 0) {
                    double burstCV = burstStdDev / burstMean;
                    
                    // Very consistent burst clicking = suspicious
                    if (burstCV < 0.08) {
                        return 0.9;
                    } else if (burstCV < 0.15) {
                        return 0.5;
                    }
                }
            }
        }
        
        return 0.0;
    }
    
    /**
     * Detect periodicity in click pattern
     * Autoclickers often have regular periodic patterns
     */
    private static double detectPeriodicity(double[] intervals) {
        if (intervals.length < 10) return 0.0;
        
        // Use autocorrelation to detect periodicity
        int period = MathUtil.detectPeriod(intervals, 0.6);
        
        if (period > 0 && period < intervals.length / 3) {
            // Strong periodicity detected = suspicious
            
            // Calculate strength of periodicity
            double acf = MathUtil.autocorrelation(intervals, period);
            
            if (acf > 0.8) {
                return 1.0;
            } else if (acf > 0.6) {
                return (acf - 0.6) / 0.2;
            }
        }
        
        return 0.0;
    }
    
    /**
     * Analyze statistical distribution
     * Natural clicks follow certain distributions
     * Mechanical clicks show different patterns
     */
    private static double analyzeDistribution(double[] intervals) {
        if (intervals.length < 20) return 0.0;
        
        // Calculate skewness and kurtosis
        double skewness = MathUtil.skewness(intervals);
        double kurtosis = MathUtil.kurtosis(intervals);
        
        double anomalyScore = 0.0;
        
        // Human click intervals typically have positive skew (some longer intervals)
        // Autoclickers tend to have near-zero skew (symmetric)
        if (Math.abs(skewness) < 0.3) {
            anomalyScore += 0.5;
        }
        
        // Kurtosis analysis
        // High kurtosis = heavy tails (many outliers) = human
        // Low kurtosis = light tails (few outliers) = autoclicker
        if (kurtosis < 0.5) {
            anomalyScore += 0.5;
        }
        
        return Math.min(1.0, anomalyScore);
    }
    
    /**
     * Calculate final autoclicker confidence
     */
    private static double calculateAutoclickerConfidence(AutoclickerAnalysis analysis) {
        // Weights for different metrics
        double consistencyWeight = 0.20;
        double burstWeight = 0.15;
        double periodicityWeight = 0.15;
        double distributionWeight = 0.10;
        double clusteringWeight = 0.10;
        double sampleEntropyWeight = 0.10;
        double permEntropyWeight = 0.10;
        double spectralWeight = 0.10;
        
        double totalSuspicion = 0.0;
        
        // High consistency = suspicious
        totalSuspicion += analysis.getConsistencyScore() * consistencyWeight;
        
        // Burst patterns
        totalSuspicion += analysis.getBurstScore() * burstWeight;
        
        // Periodicity
        totalSuspicion += analysis.getPeriodicityScore() * periodicityWeight;
        
        // Distribution anomalies
        totalSuspicion += analysis.getDistributionAnomalyScore() * distributionWeight;
        
        // Clustering
        totalSuspicion += analysis.getClusteringScore() * clusteringWeight;
        
        // Low sample entropy = low complexity = suspicious
        // Normalize to 0-1 where lower is more suspicious
        double sampleEntropyNorm = 1.0 - Math.min(1.0, analysis.getSampleEntropy() / 2.0);
        totalSuspicion += sampleEntropyNorm * sampleEntropyWeight;
        
        // Low permutation entropy = suspicious
        double permEntropyNorm = 1.0 - Math.min(1.0, analysis.getPermutationEntropy() / 3.0);
        totalSuspicion += permEntropyNorm * permEntropyWeight;
        
        // Low spectral flatness = tone-like = suspicious
        double spectralNorm = 1.0 - analysis.getSpectralFlatness();
        totalSuspicion += spectralNorm * spectralWeight;
        
        // CPS bonus check - superhuman CPS increases suspicion
        if (analysis.getAverageCPS() > 20) {
            totalSuspicion += 0.2; // Bonus suspicion for superhuman CPS
        } else if (analysis.getAverageCPS() > 16) {
            totalSuspicion += 0.1;
        }
        
        // CV bonus check - very low CV is highly suspicious
        if (analysis.getCoefficientOfVariation() < 0.05) {
            totalSuspicion += 0.2;
        }
        
        return Math.min(1.0, Math.max(0.0, totalSuspicion));
    }
    
    /**
     * Analysis result container
     */
    @Data
    public static class AutoclickerAnalysis {
        private double consistencyScore = 0.0;          // 0-1, higher = more consistent (suspicious)
        private double averageCPS = 0.0;                // Clicks per second
        private double coefficientOfVariation = 0.0;    // CV of intervals
        private double burstScore = 0.0;                // 0-1, higher = suspicious bursts
        private double periodicityScore = 0.0;          // 0-1, higher = periodic (suspicious)
        private double distributionAnomalyScore = 0.0;  // 0-1, higher = anomalous distribution
        private double clusteringScore = 0.0;           // 0-1, higher = clustered (suspicious)
        private double entropyScore = 0.0;              // Shannon entropy
        private double sampleEntropy = 0.0;             // Sample entropy (complexity)
        private double permutationEntropy = 0.0;        // Permutation entropy
        private double hurstExponent = 0.5;             // Long-range dependence
        private double spectralFlatness = 0.5;          // Spectral flatness
        
        private double autoclickerConfidence = 0.0;     // 0-1, overall confidence
        
        public String getConfidenceLevel() {
            if (autoclickerConfidence >= HIGH_CONFIDENCE_THRESHOLD) {
                return "HIGH";
            } else if (autoclickerConfidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
                return "MEDIUM";
            } else {
                return "LOW";
            }
        }
        
        public boolean isLikelyAutoclicker() {
            return autoclickerConfidence >= HIGH_CONFIDENCE_THRESHOLD;
        }
        
        public boolean isSuperhumanCPS() {
            return averageCPS > 20;
        }
    }
}
