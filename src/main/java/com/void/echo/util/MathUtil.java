package com.void.echo.util;

/**
 * Mathematical utilities for statistical analysis
 */
public class MathUtil {
    
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
}
