package com.void.echo.util;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.Arrays;
import java.util.List;

/**
 * Mathematical utilities for advanced statistical and signal processing analysis
 * 
 * Includes:
 * - Basic statistics (mean, variance, std dev)
 * - Advanced entropy calculations (Shannon, approximate Kolmogorov)
 * - Signal processing (FFT, autocorrelation, power spectrum)
 * - Pattern detection (burst detection, periodicity analysis)
 * - Curvature and smoothness analysis
 * - Anomaly detection metrics
 */
public class MathUtil {
    
    private static final double EPSILON = 1e-10;
    
    /**
     * Calculate mean (average) of array
     */
    public static double mean(double[] values) {
        if (values.length == 0) return 0.0;
        
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }
    
    /**
     * Calculate weighted mean
     */
    public static double weightedMean(double[] values, double[] weights) {
        if (values.length == 0 || values.length != weights.length) return 0.0;
        
        double sum = 0.0;
        double weightSum = 0.0;
        
        for (int i = 0; i < values.length; i++) {
            sum += values[i] * weights[i];
            weightSum += weights[i];
        }
        
        return weightSum > 0 ? sum / weightSum : 0.0;
    }
    
    /**
     * Calculate standard deviation
     */
    public static double standardDeviation(double[] values) {
        if (values.length == 0) return 0.0;
        
        double mean = mean(values);
        double sumSquaredDiff = 0.0;
        
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        
        return Math.sqrt(sumSquaredDiff / values.length);
    }
    
    /**
     * Calculate variance
     */
    public static double variance(double[] values) {
        if (values.length == 0) return 0.0;
        
        double mean = mean(values);
        double sumSquaredDiff = 0.0;
        
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        
        return sumSquaredDiff / values.length;
    }
    
    /**
     * Calculate Shannon entropy
     */
    public static double entropy(double[] values) {
        if (values.length == 0) return 0.0;
        
        // Normalize values to probabilities
        double sum = 0.0;
        for (double value : values) {
            sum += Math.abs(value);
        }
        
        if (sum == 0.0) return 0.0;
        
        double entropy = 0.0;
        for (double value : values) {
            if (value != 0.0) {
                double p = Math.abs(value) / sum;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        
        return entropy;
    }
    
    /**
     * Calculate coefficient of variation (CV)
     * CV = (standard deviation / mean) * 100
     */
    public static double coefficientOfVariation(double[] values) {
        double mean = mean(values);
        if (mean == 0.0) return 0.0;
        
        double stdDev = standardDeviation(values);
        return (stdDev / mean) * 100.0;
    }
    
    /**
     * Calculate Pearson correlation coefficient between two arrays
     */
    public static double correlation(double[] x, double[] y) {
        if (x.length != y.length || x.length == 0) return 0.0;
        
        double meanX = mean(x);
        double meanY = mean(y);
        
        double numerator = 0.0;
        double sumXSquared = 0.0;
        double sumYSquared = 0.0;
        
        for (int i = 0; i < x.length; i++) {
            double diffX = x[i] - meanX;
            double diffY = y[i] - meanY;
            
            numerator += diffX * diffY;
            sumXSquared += diffX * diffX;
            sumYSquared += diffY * diffY;
        }
        
        double denominator = Math.sqrt(sumXSquared * sumYSquared);
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }
    
    /**
     * Calculate median of array
     */
    public static double median(double[] values) {
        if (values.length == 0) return 0.0;
        
        double[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        
        int middle = sorted.length / 2;
        if (sorted.length % 2 == 0) {
            return (sorted[middle - 1] + sorted[middle]) / 2.0;
        } else {
            return sorted[middle];
        }
    }
    
    /**
     * Normalize value to 0-100 range
     */
    public static double normalize(double value, double min, double max) {
        if (max == min) return 50.0;
        return ((value - min) / (max - min)) * 100.0;
    }
    
    /**
     * Clamp value between min and max
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Calculate angle difference (normalized to -180 to 180)
     */
    public static float angleDifference(float angle1, float angle2) {
        float diff = angle1 - angle2;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return diff;
    }
    
    // ==================== ADVANCED STATISTICAL METHODS ====================
    
    /**
     * Calculate skewness (measure of asymmetry in distribution)
     * Positive skew = tail on right, Negative skew = tail on left
     */
    public static double skewness(double[] values) {
        if (values.length < 3) return 0.0;
        
        double mean = mean(values);
        double stdDev = standardDeviation(values);
        if (stdDev < EPSILON) return 0.0;
        
        double sumCubed = 0.0;
        for (double value : values) {
            double z = (value - mean) / stdDev;
            sumCubed += z * z * z;
        }
        
        return sumCubed / values.length;
    }
    
    /**
     * Calculate kurtosis (measure of "tailedness" - how outlier-prone)
     * High kurtosis = heavy tails (more outliers), Low = light tails
     */
    public static double kurtosis(double[] values) {
        if (values.length < 4) return 0.0;
        
        double mean = mean(values);
        double stdDev = standardDeviation(values);
        if (stdDev < EPSILON) return 0.0;
        
        double sumFourth = 0.0;
        for (double value : values) {
            double z = (value - mean) / stdDev;
            sumFourth += z * z * z * z;
        }
        
        return (sumFourth / values.length) - 3.0; // Excess kurtosis (normal = 0)
    }
    
    /**
     * Calculate interquartile range (IQR)
     * Robust measure of statistical dispersion
     */
    public static double iqr(double[] values) {
        if (values.length < 4) return 0.0;
        
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        
        int n = sorted.length;
        int q1Index = n / 4;
        int q3Index = (3 * n) / 4;
        
        return sorted[q3Index] - sorted[q1Index];
    }
    
    /**
     * Calculate percentile
     */
    public static double percentile(double[] values, double p) {
        if (values.length == 0) return 0.0;
        if (p < 0 || p > 100) return 0.0;
        
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        
        double index = (p / 100.0) * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        
        if (lower == upper) {
            return sorted[lower];
        }
        
        double weight = index - lower;
        return sorted[lower] * (1 - weight) + sorted[upper] * weight;
    }
    
    /**
     * Calculate z-score for each value
     */
    public static double[] zScores(double[] values) {
        double mean = mean(values);
        double stdDev = standardDeviation(values);
        
        if (stdDev < EPSILON) {
            return new double[values.length]; // All zeros
        }
        
        double[] zScores = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            zScores[i] = (values[i] - mean) / stdDev;
        }
        
        return zScores;
    }
    
    /**
     * Detect outliers using IQR method
     * Returns boolean array where true = outlier
     */
    public static boolean[] detectOutliersIQR(double[] values, double threshold) {
        if (values.length < 4) return new boolean[values.length];
        
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        
        int n = sorted.length;
        double q1 = sorted[n / 4];
        double q3 = sorted[(3 * n) / 4];
        double iqr = q3 - q1;
        
        double lowerBound = q1 - threshold * iqr;
        double upperBound = q3 + threshold * iqr;
        
        boolean[] outliers = new boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            outliers[i] = values[i] < lowerBound || values[i] > upperBound;
        }
        
        return outliers;
    }
    
    // ==================== ADVANCED ENTROPY METHODS ====================
    
    /**
     * Calculate approximate Kolmogorov complexity
     * (Normalized compression ratio as proxy)
     */
    public static double approximateKolmogorovComplexity(double[] values) {
        if (values.length == 0) return 0.0;
        
        // Count unique values and patterns
        java.util.Set<Double> unique = new java.util.HashSet<>();
        for (double v : values) {
            unique.add(v);
        }
        
        // Normalized unique ratio as complexity measure
        double uniqueRatio = (double) unique.size() / values.length;
        
        // Also consider sequential patterns
        int patterns = 1;
        for (int i = 1; i < values.length; i++) {
            if (Math.abs(values[i] - values[i-1]) > EPSILON) {
                patterns++;
            }
        }
        
        double patternComplexity = (double) patterns / values.length;
        
        // Combine metrics
        return (uniqueRatio + patternComplexity) / 2.0;
    }
    
    /**
     * Calculate permutation entropy
     * Measures complexity of time series by analyzing ordinal patterns
     */
    public static double permutationEntropy(double[] values, int embeddingDim) {
        if (values.length < embeddingDim) return 0.0;
        
        int n = values.length - embeddingDim + 1;
        java.util.Map<String, Integer> patterns = new java.util.HashMap<>();
        
        // Extract permutation patterns
        for (int i = 0; i < n; i++) {
            double[] window = new double[embeddingDim];
            Integer[] indices = new Integer[embeddingDim];
            
            for (int j = 0; j < embeddingDim; j++) {
                window[j] = values[i + j];
                indices[j] = j;
            }
            
            // Sort indices by values
            Arrays.sort(indices, (a, b) -> Double.compare(window[a], window[b]));
            
            // Create pattern signature
            String pattern = Arrays.toString(indices);
            patterns.put(pattern, patterns.getOrDefault(pattern, 0) + 1);
        }
        
        // Calculate entropy from pattern frequencies
        double entropy = 0.0;
        for (int count : patterns.values()) {
            double p = (double) count / n;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        
        return entropy;
    }
    
    /**
     * Calculate sample entropy (SampEn)
     * Measures complexity and irregularity - lower = more regular/predictable
     */
    public static double sampleEntropy(double[] values, int m, double r) {
        if (values.length < m + 1) return 0.0;
        
        int n = values.length;
        int[] count = new int[2];
        
        for (int i = 0; i < n - m; i++) {
            for (int j = i + 1; j < n - m; j++) {
                boolean match = true;
                
                // Check m-length match
                for (int k = 0; k < m && match; k++) {
                    if (Math.abs(values[i + k] - values[j + k]) > r) {
                        match = false;
                    }
                }
                
                if (match) {
                    count[0]++;
                    
                    // Check (m+1)-length match
                    if (Math.abs(values[i + m] - values[j + m]) <= r) {
                        count[1]++;
                    }
                }
            }
        }
        
        if (count[0] == 0 || count[1] == 0) return 0.0;
        
        return -Math.log((double) count[1] / count[0]);
    }
    
    // ==================== SIGNAL PROCESSING METHODS ====================
    
    /**
     * Calculate autocorrelation at lag k
     * Measures how well signal correlates with delayed version of itself
     * Used to detect periodic patterns
     */
    public static double autocorrelation(double[] values, int lag) {
        if (lag >= values.length || lag < 0) return 0.0;
        
        double mean = mean(values);
        int n = values.length - lag;
        
        double numerator = 0.0;
        double denominator = 0.0;
        
        for (int i = 0; i < n; i++) {
            double x1 = values[i] - mean;
            double x2 = values[i + lag] - mean;
            numerator += x1 * x2;
        }
        
        for (int i = 0; i < values.length; i++) {
            double x = values[i] - mean;
            denominator += x * x;
        }
        
        return denominator > 0 ? numerator / denominator : 0.0;
    }
    
    /**
     * Calculate full autocorrelation function
     */
    public static double[] autocorrelationFunction(double[] values, int maxLag) {
        maxLag = Math.min(maxLag, values.length - 1);
        double[] acf = new double[maxLag + 1];
        
        for (int lag = 0; lag <= maxLag; lag++) {
            acf[lag] = autocorrelation(values, lag);
        }
        
        return acf;
    }
    
    /**
     * Detect periodicity using autocorrelation
     * Returns dominant period (0 if not periodic)
     */
    public static int detectPeriod(double[] values, double threshold) {
        if (values.length < 4) return 0;
        
        int maxLag = Math.min(values.length / 2, 100);
        double[] acf = autocorrelationFunction(values, maxLag);
        
        // Find first significant peak after lag 0
        for (int lag = 1; lag < acf.length - 1; lag++) {
            if (acf[lag] > threshold && 
                acf[lag] > acf[lag - 1] && 
                acf[lag] > acf[lag + 1]) {
                return lag;
            }
        }
        
        return 0;
    }
    
    /**
     * Fast Fourier Transform (FFT)
     * Returns power spectrum (squared magnitudes)
     */
    public static double[] powerSpectrum(double[] values) {
        // Pad to next power of 2
        int n = nextPowerOf2(values.length);
        double[] padded = new double[n];
        System.arraycopy(values, 0, padded, 0, values.length);
        
        try {
            FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
            Complex[] spectrum = fft.transform(padded, TransformType.FORWARD);
            
            double[] power = new double[spectrum.length / 2];
            for (int i = 0; i < power.length; i++) {
                power[i] = spectrum[i].abs() * spectrum[i].abs();
            }
            
            return power;
        } catch (Exception e) {
            // Fallback to simple magnitude calculation
            return new double[n / 2];
        }
    }
    
    /**
     * Find next power of 2
     */
    private static int nextPowerOf2(int n) {
        int power = 1;
        while (power < n) {
            power *= 2;
        }
        return power;
    }
    
    /**
     * Detect dominant frequency in signal
     * Returns normalized frequency (0-0.5)
     */
    public static double dominantFrequency(double[] values) {
        double[] power = powerSpectrum(values);
        
        int maxIndex = 0;
        double maxPower = 0.0;
        
        // Skip DC component (index 0)
        for (int i = 1; i < power.length; i++) {
            if (power[i] > maxPower) {
                maxPower = power[i];
                maxIndex = i;
            }
        }
        
        // Normalized frequency
        return (double) maxIndex / (power.length * 2);
    }
    
    /**
     * Calculate spectral flatness (Wiener entropy)
     * Measures how tone-like vs noise-like a signal is
     * Close to 1 = noise-like, Close to 0 = tone-like
     */
    public static double spectralFlatness(double[] values) {
        double[] power = powerSpectrum(values);
        
        double geometricMean = 0.0;
        double arithmeticMean = 0.0;
        int count = 0;
        
        for (double p : power) {
            if (p > EPSILON) {
                geometricMean += Math.log(p);
                arithmeticMean += p;
                count++;
            }
        }
        
        if (count == 0) return 0.0;
        
        geometricMean = Math.exp(geometricMean / count);
        arithmeticMean = arithmeticMean / count;
        
        return arithmeticMean > 0 ? geometricMean / arithmeticMean : 0.0;
    }
    
    // ==================== PATTERN DETECTION METHODS ====================
    
    /**
     * Detect bursts in time series
     * Returns burst strength (0-1)
     */
    public static double detectBurstStrength(double[] intervals, double burstThreshold) {
        if (intervals.length < 3) return 0.0;
        
        double mean = mean(intervals);
        int burstCount = 0;
        int totalCount = 0;
        
        for (double interval : intervals) {
            if (interval < mean * burstThreshold) {
                burstCount++;
            }
            totalCount++;
        }
        
        return (double) burstCount / totalCount;
    }
    
    /**
     * Detect click clustering (autocl icker signature)
     * Returns cluster coefficient (0-1, higher = more clustered)
     */
    public static double clickClusterCoefficient(double[] intervals, int windowSize) {
        if (intervals.length < windowSize) return 0.0;
        
        double[] variances = new double[intervals.length - windowSize + 1];
        
        for (int i = 0; i < variances.length; i++) {
            double[] window = Arrays.copyOfRange(intervals, i, i + windowSize);
            variances[i] = variance(window);
        }
        
        // High global variance but low local variance = clustering
        double globalVar = variance(intervals);
        double avgLocalVar = mean(variances);
        
        if (globalVar < EPSILON) return 0.0;
        
        return 1.0 - (avgLocalVar / globalVar);
    }
    
    /**
     * Calculate curvature of a path (for aim analysis)
     * Higher curvature = more curved (natural)
     * Lower curvature = straighter (bot-like)
     */
    public static double pathCurvature(double[] x, double[] y) {
        if (x.length != y.length || x.length < 3) return 0.0;
        
        double totalCurvature = 0.0;
        int count = 0;
        
        for (int i = 1; i < x.length - 1; i++) {
            // Calculate curvature using three consecutive points
            double dx1 = x[i] - x[i-1];
            double dy1 = y[i] - y[i-1];
            double dx2 = x[i+1] - x[i];
            double dy2 = y[i+1] - y[i];
            
            // Cross product magnitude (proportional to curvature)
            double crossProduct = Math.abs(dx1 * dy2 - dy1 * dx2);
            
            // Distance products
            double d1 = Math.sqrt(dx1*dx1 + dy1*dy1);
            double d2 = Math.sqrt(dx2*dx2 + dy2*dy2);
            
            if (d1 > EPSILON && d2 > EPSILON) {
                totalCurvature += crossProduct / (d1 * d2);
                count++;
            }
        }
        
        return count > 0 ? totalCurvature / count : 0.0;
    }
    
    /**
     * Calculate smoothness of a signal
     * Lower value = smoother (potentially artificial)
     * Higher value = more jittery (natural)
     */
    public static double signalSmoothness(double[] values) {
        if (values.length < 3) return 0.0;
        
        double totalJerk = 0.0;
        
        // Calculate second derivative (acceleration changes = jerk)
        for (int i = 2; i < values.length; i++) {
            double acceleration1 = values[i-1] - values[i-2];
            double acceleration2 = values[i] - values[i-1];
            double jerk = Math.abs(acceleration2 - acceleration1);
            totalJerk += jerk;
        }
        
        return totalJerk / (values.length - 2);
    }
    
    /**
     * Calculate local Hurst exponent
     * H < 0.5: anti-persistent (mean reverting)
     * H = 0.5: random walk
     * H > 0.5: persistent (trending)
     */
    public static double hurstExponent(double[] values) {
        if (values.length < 20) return 0.5;
        
        int n = values.length;
        int[] lags = {2, 4, 8, 16};
        double[] logRS = new double[lags.length];
        double[] logLags = new double[lags.length];
        
        for (int i = 0; i < lags.length; i++) {
            int lag = lags[i];
            if (lag >= n) break;
            
            double rs = calculateRescaledRange(values, lag);
            logRS[i] = Math.log(rs);
            logLags[i] = Math.log(lag);
        }
        
        // Hurst = slope of log(R/S) vs log(lag)
        return linearRegression(logLags, logRS)[0];
    }
    
    /**
     * Calculate rescaled range for Hurst exponent
     */
    private static double calculateRescaledRange(double[] values, int lag) {
        int n = values.length / lag;
        double[] ranges = new double[n];
        
        for (int i = 0; i < n; i++) {
            int start = i * lag;
            int end = Math.min(start + lag, values.length);
            double[] segment = Arrays.copyOfRange(values, start, end);
            
            if (segment.length > 1) {
                double mean = mean(segment);
                double stdDev = standardDeviation(segment);
                
                // Calculate cumulative deviations
                double[] cumDev = new double[segment.length];
                cumDev[0] = segment[0] - mean;
                for (int j = 1; j < segment.length; j++) {
                    cumDev[j] = cumDev[j-1] + (segment[j] - mean);
                }
                
                double range = max(cumDev) - min(cumDev);
                ranges[i] = stdDev > 0 ? range / stdDev : 0;
            }
        }
        
        return mean(ranges);
    }
    
    /**
     * Simple linear regression
     * Returns [slope, intercept]
     */
    private static double[] linearRegression(double[] x, double[] y) {
        if (x.length != y.length || x.length == 0) return new double[]{0, 0};
        
        double meanX = mean(x);
        double meanY = mean(y);
        
        double numerator = 0.0;
        double denominator = 0.0;
        
        for (int i = 0; i < x.length; i++) {
            numerator += (x[i] - meanX) * (y[i] - meanY);
            denominator += (x[i] - meanX) * (x[i] - meanX);
        }
        
        double slope = denominator > 0 ? numerator / denominator : 0;
        double intercept = meanY - slope * meanX;
        
        return new double[]{slope, intercept};
    }
    
    /**
     * Find minimum value in array
     */
    private static double min(double[] values) {
        if (values.length == 0) return 0.0;
        double min = values[0];
        for (double v : values) {
            if (v < min) min = v;
        }
        return min;
    }
    
    /**
     * Find maximum value in array
     */
    private static double max(double[] values) {
        if (values.length == 0) return 0.0;
        double max = values[0];
        for (double v : values) {
            if (v > max) max = v;
        }
        return max;
    }
    
    // ==================== ANOMALY DETECTION METHODS ====================
    
    /**
     * Calculate Mahalanobis distance (multivariate outlier detection)
     * Measures distance of point from distribution center
     */
    public static double mahalanobisDistance(double[] point, double[] mean, double[][] covarianceMatrix) {
        int n = point.length;
        if (n != mean.length) return 0.0;
        
        // Calculate inverse of covariance matrix (simplified for 2D)
        if (n == 2) {
            double det = covarianceMatrix[0][0] * covarianceMatrix[1][1] - 
                        covarianceMatrix[0][1] * covarianceMatrix[1][0];
            
            if (Math.abs(det) < EPSILON) return 0.0;
            
            double[][] inv = new double[2][2];
            inv[0][0] = covarianceMatrix[1][1] / det;
            inv[0][1] = -covarianceMatrix[0][1] / det;
            inv[1][0] = -covarianceMatrix[1][0] / det;
            inv[1][1] = covarianceMatrix[0][0] / det;
            
            double[] diff = new double[]{point[0] - mean[0], point[1] - mean[1]};
            
            double distance = 0.0;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    distance += diff[i] * inv[i][j] * diff[j];
                }
            }
            
            return Math.sqrt(Math.abs(distance));
        }
        
        return 0.0;
    }
    
    /**
     * Calculate Local Outlier Factor (LOF)
     * Simplified version for 1D data
     */
    public static double[] localOutlierFactors(double[] values, int k) {
        if (values.length < k + 1) return new double[values.length];
        
        double[] lof = new double[values.length];
        
        for (int i = 0; i < values.length; i++) {
            // Find k nearest neighbors
            double[] distances = new double[values.length];
            for (int j = 0; j < values.length; j++) {
                distances[j] = Math.abs(values[i] - values[j]);
            }
            
            // Sort and get k-th distance
            double[] sortedDist = distances.clone();
            Arrays.sort(sortedDist);
            double kDist = sortedDist[Math.min(k, sortedDist.length - 1)];
            
            // Calculate local reachability density
            double lrd = 1.0 / (kDist + EPSILON);
            
            // LOF is ratio of neighbor densities to point density
            lof[i] = Math.max(1.0, lrd / (lrd + EPSILON));
        }
        
        return lof;
    }
    
    /**
     * Calculate Grubbs' test statistic for outlier detection
     * Returns G-value (compare with critical value)
     */
    public static double grubbsTest(double[] values) {
        if (values.length < 3) return 0.0;
        
        double mean = mean(values);
        double stdDev = standardDeviation(values);
        
        if (stdDev < EPSILON) return 0.0;
        
        double maxDeviation = 0.0;
        for (double value : values) {
            double deviation = Math.abs(value - mean);
            if (deviation > maxDeviation) {
                maxDeviation = deviation;
            }
        }
        
        return maxDeviation / stdDev;
    }
    
    /**
     * Moving average filter
     */
    public static double[] movingAverage(double[] values, int windowSize) {
        if (values.length < windowSize) return values.clone();
        
        double[] smoothed = new double[values.length];
        
        for (int i = 0; i < values.length; i++) {
            int start = Math.max(0, i - windowSize / 2);
            int end = Math.min(values.length, i + windowSize / 2 + 1);
            
            double sum = 0.0;
            for (int j = start; j < end; j++) {
                sum += values[j];
            }
            
            smoothed[i] = sum / (end - start);
        }
        
        return smoothed;
    }
    
    /**
     * Exponential moving average
     */
    public static double[] exponentialMovingAverage(double[] values, double alpha) {
        if (values.length == 0) return new double[0];
        
        double[] ema = new double[values.length];
        ema[0] = values[0];
        
        for (int i = 1; i < values.length; i++) {
            ema[i] = alpha * values[i] + (1 - alpha) * ema[i-1];
        }
        
        return ema;
    }
    
    /**
     * Calculate change point score (detects sudden behavioral shifts)
     * Higher score = more likely a change point
     */
    public static double[] changePointDetection(double[] values, int windowSize) {
        if (values.length < windowSize * 2) return new double[values.length];
        
        double[] scores = new double[values.length];
        
        for (int i = windowSize; i < values.length - windowSize; i++) {
            double[] before = Arrays.copyOfRange(values, i - windowSize, i);
            double[] after = Arrays.copyOfRange(values, i, i + windowSize);
            
            double meanBefore = mean(before);
            double meanAfter = mean(after);
            double varBefore = variance(before);
            double varAfter = variance(after);
            
            // Combined metric: difference in means and variances
            double meanDiff = Math.abs(meanAfter - meanBefore);
            double varDiff = Math.abs(varAfter - varBefore);
            
            scores[i] = meanDiff + varDiff;
        }
        
        return scores;
    }
    
    /**
     * Calculate Jensen-Shannon divergence between two distributions
     * Symmetric measure of similarity (0 = identical, 1 = completely different)
     */
    public static double jensenShannonDivergence(double[] p, double[] q) {
        if (p.length != q.length || p.length == 0) return 0.0;
        
        // Normalize to probability distributions
        double sumP = 0.0, sumQ = 0.0;
        for (int i = 0; i < p.length; i++) {
            sumP += Math.abs(p[i]);
            sumQ += Math.abs(q[i]);
        }
        
        if (sumP < EPSILON || sumQ < EPSILON) return 0.0;
        
        double[] normP = new double[p.length];
        double[] normQ = new double[q.length];
        for (int i = 0; i < p.length; i++) {
            normP[i] = Math.abs(p[i]) / sumP;
            normQ[i] = Math.abs(q[i]) / sumQ;
        }
        
        // Calculate midpoint distribution
        double[] m = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            m[i] = (normP[i] + normQ[i]) / 2.0;
        }
        
        // Calculate KL divergences
        double klPM = kullbackLeiblerDivergence(normP, m);
        double klQM = kullbackLeiblerDivergence(normQ, m);
        
        return (klPM + klQM) / 2.0;
    }
    
    /**
     * Calculate Kullback-Leibler divergence
     */
    private static double kullbackLeiblerDivergence(double[] p, double[] q) {
        double divergence = 0.0;
        
        for (int i = 0; i < p.length; i++) {
            if (p[i] > EPSILON && q[i] > EPSILON) {
                divergence += p[i] * Math.log(p[i] / q[i]);
            }
        }
        
        return divergence;
    }
}
