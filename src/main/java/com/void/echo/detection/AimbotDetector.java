package com.void.echo.detection;

import com.void.echo.data.RotationData;
import com.void.echo.util.MathUtil;
import com.void.echo.util.SignalProcessor;
import lombok.Data;

import java.util.List;

/**
 * Advanced aimbot detection using behavioral analysis
 * 
 * Detection methods:
 * 1. Aim curvature analysis (bots aim in straight lines)
 * 2. Angular velocity consistency (unnatural smoothness)
 * 3. Snap detection (instant target acquisition)
 * 4. Pre-alignment detection (crosshair already near target)
 * 5. Micro-correction absence (humans overshoot and correct)
 * 6. Oscillation patterns (some aimbots oscillate)
 */
public class AimbotDetector {
    
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.75;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.50;
    
    /**
     * Analyze rotation data for aimbot signatures
     */
    public static AimbotAnalysis analyze(List<RotationData> rotations) {
        if (rotations.size() < 10) {
            return new AimbotAnalysis();
        }
        
        AimbotAnalysis analysis = new AimbotAnalysis();
        
        // Extract yaw and pitch deltas
        double[] yawDeltas = rotations.stream()
                .filter(RotationData::isSignificant)
                .mapToDouble(RotationData::getYawDelta)
                .toArray();
        
        double[] pitchDeltas = rotations.stream()
                .filter(RotationData::isSignificant)
                .mapToDouble(RotationData::getPitchDelta)
                .toArray();
        
        if (yawDeltas.length < 5) {
            return analysis;
        }
        
        // 1. Curvature analysis
        double curvature = calculateAimCurvature(yawDeltas, pitchDeltas);
        analysis.setCurvatureScore(curvature);
        
        // 2. Smoothness analysis
        double smoothness = MathUtil.signalSmoothness(yawDeltas);
        analysis.setSmoothnessScore(smoothness);
        
        // 3. Snap detection
        double snapStrength = detectSnaps(yawDeltas, pitchDeltas);
        analysis.setSnapStrength(snapStrength);
        
        // 4. Angular consistency
        double angularConsistency = calculateAngularConsistency(yawDeltas, pitchDeltas);
        analysis.setAngularConsistency(angularConsistency);
        
        // 5. Micro-jitter analysis
        double jitterScore = analyzeMicroJitter(yawDeltas, pitchDeltas);
        analysis.setJitterScore(jitterScore);
        
        // 6. Oscillation detection
        double oscillationScore = detectOscillation(yawDeltas, pitchDeltas);
        analysis.setOscillationScore(oscillationScore);
        
        // 7. Velocity profile analysis
        double velocityConsistency = analyzeVelocityProfile(rotations);
        analysis.setVelocityConsistency(velocityConsistency);
        
        // 8. Spectral analysis
        double spectralAnomalyScore = spectralAnalysis(yawDeltas);
        analysis.setSpectralAnomalyScore(spectralAnomalyScore);
        
        // Calculate final confidence
        double confidence = calculateAimbotConfidence(analysis);
        analysis.setAimbotConfidence(confidence);
        
        return analysis;
    }
    
    /**
     * Calculate aim curvature
     * Bots tend to aim in perfectly straight lines (low curvature)
     * Humans have natural arc to their aim (higher curvature)
     */
    private static double calculateAimCurvature(double[] yawDeltas, double[] pitchDeltas) {
        if (yawDeltas.length < 3) return 1.0; // Assume human if not enough data
        
        double curvature = MathUtil.pathCurvature(yawDeltas, pitchDeltas);
        
        // Normalize: low curvature = suspicious
        // Curvature typically ranges from 0 (straight) to ~0.5 (very curved)
        // We want to score it such that straighter = lower score
        double normalizedCurvature = Math.min(1.0, curvature * 4.0);
        
        return normalizedCurvature;
    }
    
    /**
     * Detect snap-to-target behavior
     * Aimbots often instantly snap to targets with high angular velocity
     */
    private static double detectSnaps(double[] yawDeltas, double[] pitchDeltas) {
        if (yawDeltas.length < 3) return 0.0;
        
        double[] angularVelocities = new double[yawDeltas.length];
        for (int i = 0; i < yawDeltas.length; i++) {
            angularVelocities[i] = Math.sqrt(yawDeltas[i]*yawDeltas[i] + pitchDeltas[i]*pitchDeltas[i]);
        }
        
        // Detect sudden large movements
        double mean = MathUtil.mean(angularVelocities);
        double stdDev = MathUtil.standardDeviation(angularVelocities);
        
        if (stdDev < 0.1) return 0.0;
        
        int snapCount = 0;
        for (double velocity : angularVelocities) {
            // Snap = movement > 3 standard deviations from mean
            if (velocity > mean + 3 * stdDev && velocity > 10.0) {
                snapCount++;
            }
        }
        
        double snapRatio = (double) snapCount / angularVelocities.length;
        
        // High snap ratio is suspicious, but some snaps are normal
        // Suspicious if > 10% of movements are snaps
        return Math.min(1.0, snapRatio / 0.1);
    }
    
    /**
     * Calculate angular consistency
     * Aimbots maintain very consistent angular velocity during aim
     */
    private static double calculateAngularConsistency(double[] yawDeltas, double[] pitchDeltas) {
        if (yawDeltas.length < 5) return 0.0;
        
        double[] angularVelocities = new double[yawDeltas.length];
        for (int i = 0; i < yawDeltas.length; i++) {
            angularVelocities[i] = Math.sqrt(yawDeltas[i]*yawDeltas[i] + pitchDeltas[i]*pitchDeltas[i]);
        }
        
        // Calculate coefficient of variation
        double mean = MathUtil.mean(angularVelocities);
        double stdDev = MathUtil.standardDeviation(angularVelocities);
        
        if (mean < 0.1) return 0.0;
        
        double cv = stdDev / mean;
        
        // Low CV = very consistent = suspicious
        // CV < 0.3 is very consistent
        if (cv < 0.3) {
            return 1.0 - (cv / 0.3);
        }
        
        return 0.0;
    }
    
    /**
     * Analyze micro-jitter (natural hand tremor)
     * Humans have high-frequency micro-jitter
     * Aimbots lack this natural noise
     */
    private static double analyzeMicroJitter(double[] yawDeltas, double[] pitchDeltas) {
        if (yawDeltas.length < 10) return 0.5; // Not enough data
        
        // Remove low-frequency components (overall aim movement)
        double[] yawHighFreq = removeMovingAverage(yawDeltas, 5);
        double[] pitchHighFreq = removeMovingAverage(pitchDeltas, 5);
        
        // Calculate energy in high-frequency components
        double yawEnergy = 0.0;
        for (double y : yawHighFreq) {
            yawEnergy += y * y;
        }
        
        double pitchEnergy = 0.0;
        for (double p : pitchHighFreq) {
            pitchEnergy += p * p;
        }
        
        double totalEnergy = yawEnergy + pitchEnergy;
        
        // Normalize by number of samples
        double avgEnergy = totalEnergy / (yawHighFreq.length + pitchHighFreq.length);
        
        // High energy in high frequencies = natural jitter
        // Low energy = artificially smooth = suspicious
        // Typical human jitter energy > 0.01
        return Math.min(1.0, avgEnergy / 0.01);
    }
    
    /**
     * Remove moving average to isolate high-frequency components
     */
    private static double[] removeMovingAverage(double[] signal, int windowSize) {
        double[] smoothed = MathUtil.movingAverage(signal, windowSize);
        double[] highFreq = new double[signal.length];
        
        for (int i = 0; i < signal.length; i++) {
            highFreq[i] = signal[i] - smoothed[i];
        }
        
        return highFreq;
    }
    
    /**
     * Detect oscillation patterns
     * Some aimbots create regular oscillations
     */
    private static double detectOscillation(double[] yawDeltas, double[] pitchDeltas) {
        if (yawDeltas.length < 10) return 0.0;
        
        // Detect periodicity
        int yawPeriod = MathUtil.detectPeriod(yawDeltas, 0.5);
        int pitchPeriod = MathUtil.detectPeriod(pitchDeltas, 0.5);
        
        // If strong periodicity detected = suspicious
        if (yawPeriod > 0 && yawPeriod < yawDeltas.length / 3) {
            return 0.8;
        }
        
        if (pitchPeriod > 0 && pitchPeriod < pitchDeltas.length / 3) {
            return 0.8;
        }
        
        return 0.0;
    }
    
    /**
     * Analyze velocity profile over time
     * Bots often have very consistent velocity curves
     */
    private static double analyzeVelocityProfile(List<RotationData> rotations) {
        if (rotations.size() < 10) return 0.0;
        
        // Extract angular velocities over time
        double[] velocities = new double[rotations.size() - 1];
        
        for (int i = 1; i < rotations.size(); i++) {
            RotationData prev = rotations.get(i - 1);
            RotationData curr = rotations.get(i);
            
            double yawDiff = Math.abs(MathUtil.angleDifference(curr.getYaw(), prev.getYaw()));
            double pitchDiff = Math.abs(curr.getPitch() - prev.getPitch());
            
            velocities[i - 1] = Math.sqrt(yawDiff*yawDiff + pitchDiff*pitchDiff);
        }
        
        // Analyze consistency of acceleration patterns
        double[] accelerations = new double[velocities.length - 1];
        for (int i = 1; i < velocities.length; i++) {
            accelerations[i - 1] = velocities[i] - velocities[i - 1];
        }
        
        // Calculate consistency metrics
        double stdDev = MathUtil.standardDeviation(accelerations);
        double mean = MathUtil.mean(accelerations);
        
        if (Math.abs(mean) < 0.01) return 0.0;
        
        double cv = stdDev / Math.abs(mean);
        
        // Low CV in acceleration = very consistent = suspicious
        if (cv < 0.4) {
            return 1.0 - (cv / 0.4);
        }
        
        return 0.0;
    }
    
    /**
     * Spectral analysis of aim patterns
     * Detects artificial frequency signatures
     */
    private static double spectralAnalysis(double[] yawDeltas) {
        if (yawDeltas.length < 16) return 0.0;
        
        // Calculate spectral flatness (Wiener entropy)
        double flatness = MathUtil.spectralFlatness(yawDeltas);
        
        // Natural human aim has high spectral flatness (noise-like)
        // Artificial aim has low spectral flatness (tone-like patterns)
        // Flatness typically 0.3-0.8 for humans, < 0.2 for bots
        
        if (flatness < 0.2) {
            return 1.0;
        } else if (flatness < 0.4) {
            return (0.4 - flatness) / 0.2;
        }
        
        return 0.0;
    }
    
    /**
     * Calculate final aimbot confidence score
     * Combines all metrics with appropriate weighting
     */
    private static double calculateAimbotConfidence(AimbotAnalysis analysis) {
        // Weights for different metrics
        double curvatureWeight = 0.20;
        double smoothnessWeight = 0.15;
        double snapWeight = 0.15;
        double consistencyWeight = 0.15;
        double jitterWeight = 0.15;
        double oscillationWeight = 0.10;
        double velocityWeight = 0.05;
        double spectralWeight = 0.05;
        
        // Combine scores
        // Low curvature score = suspicious
        double curvatureSuspicion = 1.0 - analysis.getCurvatureScore();
        
        // Low smoothness = suspicious
        double smoothnessSuspicion = analysis.getSmoothnessScore() < 0.05 ? 1.0 : 0.0;
        
        // Low jitter score = suspicious
        double jitterSuspicion = 1.0 - analysis.getJitterScore();
        
        double totalSuspicion = 
            curvatureSuspicion * curvatureWeight +
            smoothnessSuspicion * smoothnessWeight +
            analysis.getSnapStrength() * snapWeight +
            analysis.getAngularConsistency() * consistencyWeight +
            jitterSuspicion * jitterWeight +
            analysis.getOscillationScore() * oscillationWeight +
            analysis.getVelocityConsistency() * velocityWeight +
            analysis.getSpectralAnomalyScore() * spectralWeight;
        
        return Math.min(1.0, Math.max(0.0, totalSuspicion));
    }
    
    /**
     * Analysis result container
     */
    @Data
    public static class AimbotAnalysis {
        private double curvatureScore = 1.0;        // 0-1, higher = more curved (human-like)
        private double smoothnessScore = 0.0;       // 0+, higher = more jittery (human-like)
        private double snapStrength = 0.0;          // 0-1, higher = more snaps (suspicious)
        private double angularConsistency = 0.0;    // 0-1, higher = more consistent (suspicious)
        private double jitterScore = 0.5;           // 0-1, higher = more jitter (human-like)
        private double oscillationScore = 0.0;      // 0-1, higher = periodic (suspicious)
        private double velocityConsistency = 0.0;   // 0-1, higher = consistent velocity (suspicious)
        private double spectralAnomalyScore = 0.0;  // 0-1, higher = artificial frequency pattern
        
        private double aimbotConfidence = 0.0;      // 0-1, overall confidence
        
        public String getConfidenceLevel() {
            if (aimbotConfidence >= HIGH_CONFIDENCE_THRESHOLD) {
                return "HIGH";
            } else if (aimbotConfidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
                return "MEDIUM";
            } else {
                return "LOW";
            }
        }
        
        public boolean isLikelyAimbot() {
            return aimbotConfidence >= HIGH_CONFIDENCE_THRESHOLD;
        }
    }
}
