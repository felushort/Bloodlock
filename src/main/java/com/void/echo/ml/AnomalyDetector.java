package com.void.echo.ml;

import com.void.echo.util.MathUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Anomaly detection using ensemble methods
 * 
 * Combines multiple anomaly detection algorithms:
 * 1. Statistical outlier detection (Z-score, IQR)
 * 2. Local Outlier Factor (LOF)
 * 3. Isolation Forest (simplified)
 * 4. One-Class SVM (simplified)
 * 5. DBSCAN outlier detection
 * 6. Ensemble voting
 */
public class AnomalyDetector {
    
    private static final double DEFAULT_CONTAMINATION = 0.1; // Expect 10% anomalies
    
    /**
     * Anomaly detection result
     */
    @Data
    public static class AnomalyScore {
        private double zScoreAnomaly;
        private double iqrAnomaly;
        private double lofScore;
        private double isolationScore;
        private double densityScore;
        private double ensembleScore;
        
        private boolean isAnomaly;
        private double confidence;
        
        public String getSeverity() {
            if (confidence >= 0.8) return "SEVERE";
            if (confidence >= 0.6) return "HIGH";
            if (confidence >= 0.4) return "MEDIUM";
            return "LOW";
        }
    }
    
    /**
     * Detect anomalies in data using ensemble methods
     */
    public static List<AnomalyScore> detectAnomalies(double[][] data, double contamination) {
        if (data.length == 0) return new ArrayList<>();
        
        List<AnomalyScore> scores = new ArrayList<>();
        int n = data.length;
        
        for (int i = 0; i < n; i++) {
            AnomalyScore score = new AnomalyScore();
            
            // 1. Z-score based detection (univariate for first feature)
            score.setZScoreAnomaly(zScoreAnomaly(data, i));
            
            // 2. IQR based detection
            score.setIqrAnomaly(iqrAnomaly(data, i));
            
            // 3. LOF score
            score.setLofScore(calculateLOF(data, i, 5));
            
            // 4. Isolation score (simplified)
            score.setIsolationScore(isolationScore(data, i));
            
            // 5. Density based score
            score.setDensityScore(densityBasedScore(data, i, 5));
            
            // 6. Ensemble score (weighted combination)
            double ensemble = calculateEnsembleScore(score);
            score.setEnsembleScore(ensemble);
            
            // Determine if anomaly
            score.setAnomaly(ensemble > (1.0 - contamination));
            score.setConfidence(ensemble);
            
            scores.add(score);
        }
        
        return scores;
    }
    
    /**
     * Z-score based anomaly detection
     */
    private static double zScoreAnomaly(double[][] data, int index) {
        // Calculate z-score for first dimension
        double[] firstDim = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            firstDim[i] = data[i][0];
        }
        
        double[] zScores = MathUtil.zScores(firstDim);
        double absZScore = Math.abs(zScores[index]);
        
        // Normalize to 0-1 (z-score > 3 is anomaly)
        return Math.min(1.0, absZScore / 3.0);
    }
    
    /**
     * IQR based anomaly detection
     */
    private static double iqrAnomaly(double[][] data, int index) {
        // Use first dimension
        double[] firstDim = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            firstDim[i] = data[i][0];
        }
        
        boolean[] outliers = MathUtil.detectOutliersIQR(firstDim, 1.5);
        return outliers[index] ? 1.0 : 0.0;
    }
    
    /**
     * Local Outlier Factor (simplified implementation)
     */
    private static double calculateLOF(double[][] data, int index, int k) {
        int n = data.length;
        if (n < k + 1) return 0.0;
        
        // Calculate distances to all other points
        double[] distances = new double[n];
        for (int i = 0; i < n; i++) {
            distances[i] = euclideanDistance(data[index], data[i]);
        }
        
        // Find k nearest neighbors
        double[] sorted = distances.clone();
        Arrays.sort(sorted);
        double kDistance = sorted[Math.min(k, n - 1)];
        
        // Calculate reachability distance
        double reachabilityDist = 0.0;
        int neighbors = 0;
        
        for (int i = 0; i < n; i++) {
            if (i != index && distances[i] <= kDistance) {
                // Get k-distance of neighbor
                double[] neighborDist = new double[n];
                for (int j = 0; j < n; j++) {
                    neighborDist[j] = euclideanDistance(data[i], data[j]);
                }
                double[] neighborSorted = neighborDist.clone();
                Arrays.sort(neighborSorted);
                double neighborKDist = neighborSorted[Math.min(k, n - 1)];
                
                reachabilityDist += Math.max(distances[i], neighborKDist);
                neighbors++;
            }
        }
        
        if (neighbors == 0) return 0.0;
        
        double lrd = neighbors / reachabilityDist;
        
        // Calculate LOF
        double lofSum = 0.0;
        neighbors = 0;
        
        for (int i = 0; i < n; i++) {
            if (i != index && distances[i] <= kDistance) {
                // Calculate neighbor's LRD (simplified)
                lofSum += 1.0 / (kDistance + 1e-10);
                neighbors++;
            }
        }
        
        if (neighbors == 0) return 0.0;
        
        double avgNeighborLRD = lofSum / neighbors;
        double lof = avgNeighborLRD / (lrd + 1e-10);
        
        // Normalize: LOF > 1.5 is anomalous
        return Math.min(1.0, Math.max(0.0, (lof - 1.0) / 1.5));
    }
    
    /**
     * Isolation score (simplified Isolation Forest concept)
     * Points that are easy to isolate are anomalies
     */
    private static double isolationScore(double[][] data, int index) {
        int n = data.length;
        int dims = data[0].length;
        
        // Measure how quickly this point gets isolated
        int isolationSteps = 0;
        int maxSteps = 10;
        
        for (int step = 0; step < maxSteps; step++) {
            // Random dimension
            int dim = (int) (Math.random() * dims);
            
            // Random split value
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (double[] point : data) {
                if (point[dim] < min) min = point[dim];
                if (point[dim] > max) max = point[dim];
            }
            
            double splitValue = min + Math.random() * (max - min);
            
            // Count points on same side
            boolean targetSide = data[index][dim] < splitValue;
            int sameSideCount = 0;
            
            for (double[] point : data) {
                if ((point[dim] < splitValue) == targetSide) {
                    sameSideCount++;
                }
            }
            
            // If isolated early, it's an anomaly
            if (sameSideCount <= 1) {
                isolationSteps = step + 1;
                break;
            }
        }
        
        // Earlier isolation = higher anomaly score
        return (double) (maxSteps - isolationSteps) / maxSteps;
    }
    
    /**
     * Density based scoring
     * Low density regions contain anomalies
     */
    private static double densityBasedScore(double[][] data, int index, int k) {
        int n = data.length;
        if (n < k + 1) return 0.0;
        
        // Calculate distances to all points
        double[] distances = new double[n];
        for (int i = 0; i < n; i++) {
            distances[i] = euclideanDistance(data[index], data[i]);
        }
        
        // Find k-nearest neighbor distance
        double[] sorted = distances.clone();
        Arrays.sort(sorted);
        double knnDist = sorted[Math.min(k, n - 1)];
        
        // Calculate average k-nn distance for all points
        double avgKnnDist = 0.0;
        for (int i = 0; i < n; i++) {
            double[] iDistances = new double[n];
            for (int j = 0; j < n; j++) {
                iDistances[j] = euclideanDistance(data[i], data[j]);
            }
            double[] iSorted = iDistances.clone();
            Arrays.sort(iSorted);
            avgKnnDist += iSorted[Math.min(k, n - 1)];
        }
        avgKnnDist /= n;
        
        // Points with large k-nn distance are in low density regions
        if (avgKnnDist < 1e-10) return 0.0;
        
        double densityRatio = knnDist / avgKnnDist;
        
        // Normalize: ratio > 2 is anomalous
        return Math.min(1.0, Math.max(0.0, (densityRatio - 1.0) / 2.0));
    }
    
    /**
     * Calculate ensemble score combining all methods
     */
    private static double calculateEnsembleScore(AnomalyScore score) {
        // Weighted voting
        double weights[] = {0.15, 0.15, 0.30, 0.25, 0.15};
        
        double ensemble = 
            score.getZScoreAnomaly() * weights[0] +
            score.getIqrAnomaly() * weights[1] +
            score.getLofScore() * weights[2] +
            score.getIsolationScore() * weights[3] +
            score.getDensityScore() * weights[4];
        
        return Math.min(1.0, Math.max(0.0, ensemble));
    }
    
    /**
     * Euclidean distance between two points
     */
    private static double euclideanDistance(double[] p1, double[] p2) {
        double sum = 0.0;
        for (int i = 0; i < p1.length; i++) {
            double diff = p1[i] - p2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
    
    /**
     * Find anomalies in time series data
     */
    public static List<Integer> findTimeSeriesAnomalies(double[] timeSeries, int windowSize) {
        if (timeSeries.length < windowSize * 2) return new ArrayList<>();
        
        List<Integer> anomalies = new ArrayList<>();
        
        // Calculate change points
        double[] changeScores = MathUtil.changePointDetection(timeSeries, windowSize);
        
        // Detect anomalies as significant change points
        double threshold = MathUtil.mean(changeScores) + 2 * MathUtil.standardDeviation(changeScores);
        
        for (int i = 0; i < changeScores.length; i++) {
            if (changeScores[i] > threshold) {
                anomalies.add(i);
            }
        }
        
        return anomalies;
    }
    
    /**
     * Detect concept drift (distributional changes over time)
     */
    public static double detectConceptDrift(double[] recentData, double[] historicalData) {
        if (recentData.length < 10 || historicalData.length < 10) {
            return 0.0;
        }
        
        // Compare distributions using Kolmogorov-Smirnov-like test
        double[] recentSorted = recentData.clone();
        double[] historicalSorted = historicalData.clone();
        
        Arrays.sort(recentSorted);
        Arrays.sort(historicalSorted);
        
        // Calculate maximum difference in CDFs
        double maxDiff = 0.0;
        int i = 0, j = 0;
        
        while (i < recentSorted.length && j < historicalSorted.length) {
            double recentCDF = (double) i / recentSorted.length;
            double historicalCDF = (double) j / historicalSorted.length;
            
            double diff = Math.abs(recentCDF - historicalCDF);
            if (diff > maxDiff) {
                maxDiff = diff;
            }
            
            if (recentSorted[i] < historicalSorted[j]) {
                i++;
            } else {
                j++;
            }
        }
        
        // Also compare statistical moments
        double meanDiff = Math.abs(MathUtil.mean(recentData) - MathUtil.mean(historicalData));
        double stdDiff = Math.abs(MathUtil.standardDeviation(recentData) - MathUtil.standardDeviation(historicalData));
        
        // Normalize differences
        double meanNorm = meanDiff / (MathUtil.mean(historicalData) + 1e-10);
        double stdNorm = stdDiff / (MathUtil.standardDeviation(historicalData) + 1e-10);
        
        // Combine metrics
        double driftScore = 0.4 * maxDiff + 0.3 * meanNorm + 0.3 * stdNorm;
        
        return Math.min(1.0, driftScore);
    }
}
