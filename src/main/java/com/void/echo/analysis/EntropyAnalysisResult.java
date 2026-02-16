package com.void.echo.analysis;

import com.void.echo.detection.AimbotDetector;
import com.void.echo.detection.AutoclickerDetector;
import lombok.Data;

/**
 * Enhanced results of entropy analysis with advanced detection
 */
@Data
public class EntropyAnalysisResult {
    
    // Individual component scores (0-100, where 100 is most human-like)
    private double yawEntropyScore;
    private double pitchEntropyScore;
    private double clickDistributionScore;
    private double reactionTimeScore;
    private double crosshairAlignmentScore;
    
    // Overall human probability score (0-100)
    private double humanProbabilityScore;
    
    // Detailed metrics
    private double rotationConsistency;     // How consistent (lower = more human)
    private double cpsVariance;             // CPS variance (higher = more human)
    private double reactionTimeStdDev;      // Reaction time std dev
    private double aimedClickRatio;         // Ratio of perfectly aimed clicks
    
    // Advanced detection results
    private AimbotDetector.AimbotAnalysis aimbotAnalysis;
    private AutoclickerDetector.AutoclickerAnalysis autoclickerAnalysis;
    
    // Advanced entropy metrics
    private double permutationEntropy;      // Pattern complexity
    private double sampleEntropy;           // Signal complexity
    private double spectralFlatness;        // Frequency distribution
    private double hurstExponent;           // Long-range dependence
    
    // Flags
    private boolean suspicious;             // Below 40
    private boolean extremelySuspicious;    // Below 20
    private boolean aimbotDetected;
    private boolean autoclickerDetected;
    
    // Sample counts
    private int rotationSamples;
    private int clickSamples;
    
    public EntropyAnalysisResult() {
        this.humanProbabilityScore = 100.0; // Innocent until proven guilty
        this.suspicious = false;
        this.extremelySuspicious = false;
        this.aimbotDetected = false;
        this.autoclickerDetected = false;
    }
    
    /**
     * Calculate final human probability score from components
     */
    public void calculateFinalScore(double yawWeight, double pitchWeight, 
                                   double clickWeight, double reactionWeight, 
                                   double alignmentWeight) {
        // Weighted average
        this.humanProbabilityScore = 
                (yawEntropyScore * yawWeight) +
                (pitchEntropyScore * pitchWeight) +
                (clickDistributionScore * clickWeight) +
                (reactionTimeScore * reactionWeight) +
                (crosshairAlignmentScore * alignmentWeight);
        
        // Apply penalties from advanced detectors
        if (aimbotAnalysis != null && aimbotAnalysis.isLikelyAimbot()) {
            double penalty = aimbotAnalysis.getAimbotConfidence() * 40.0; // Up to 40 point penalty
            this.humanProbabilityScore -= penalty;
            this.aimbotDetected = true;
        }
        
        if (autoclickerAnalysis != null && autoclickerAnalysis.isLikelyAutoclicker()) {
            double penalty = autoclickerAnalysis.getAutoclickerConfidence() * 30.0; // Up to 30 point penalty
            this.humanProbabilityScore -= penalty;
            this.autoclickerDetected = true;
        }
        
        // Clamp to 0-100
        this.humanProbabilityScore = Math.max(0, Math.min(100, this.humanProbabilityScore));
        
        // Set flags
        this.suspicious = this.humanProbabilityScore < 40;
        this.extremelySuspicious = this.humanProbabilityScore < 20;
    }
    
    /**
     * Get a readable description of the result
     */
    public String getDescription() {
        if (aimbotDetected && autoclickerDetected) {
            return "Multiple Cheats Detected (Aimbot + Autoclicker)";
        } else if (aimbotDetected) {
            return "Aimbot Detected (" + aimbotAnalysis.getConfidenceLevel() + " Confidence)";
        } else if (autoclickerDetected) {
            return "Autoclicker Detected (" + autoclickerAnalysis.getConfidenceLevel() + " Confidence)";
        } else if (extremelySuspicious) {
            return "Extremely Unlikely to be Human";
        } else if (suspicious) {
            return "Suspicious Behavior Detected";
        } else if (humanProbabilityScore < 60) {
            return "Minor Anomalies Detected";
        } else if (humanProbabilityScore < 80) {
            return "Normal Behavior";
        } else {
            return "Clean";
        }
    }
    
    /**
     * Get detailed analysis report
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== ECHO Behavioral Analysis Report ===\n");
        report.append(String.format("Human Probability Score: %.1f/100\n", humanProbabilityScore));
        report.append(String.format("Status: %s\n\n", getDescription()));
        
        report.append("--- Component Scores ---\n");
        report.append(String.format("Yaw Entropy: %.1f/100\n", yawEntropyScore));
        report.append(String.format("Pitch Entropy: %.1f/100\n", pitchEntropyScore));
        report.append(String.format("Click Distribution: %.1f/100\n", clickDistributionScore));
        report.append(String.format("Reaction Time: %.1f/100\n", reactionTimeScore));
        report.append(String.format("Crosshair Alignment: %.1f/100\n\n", crosshairAlignmentScore));
        
        if (aimbotAnalysis != null) {
            report.append("--- Aimbot Analysis ---\n");
            report.append(String.format("Confidence: %.1f%% (%s)\n", 
                aimbotAnalysis.getAimbotConfidence() * 100, 
                aimbotAnalysis.getConfidenceLevel()));
            report.append(String.format("Aim Curvature: %.3f\n", aimbotAnalysis.getCurvatureScore()));
            report.append(String.format("Micro Jitter: %.3f\n", aimbotAnalysis.getJitterScore()));
            report.append(String.format("Snap Strength: %.3f\n\n", aimbotAnalysis.getSnapStrength()));
        }
        
        if (autoclickerAnalysis != null) {
            report.append("--- Autoclicker Analysis ---\n");
            report.append(String.format("Confidence: %.1f%% (%s)\n", 
                autoclickerAnalysis.getAutoclickerConfidence() * 100,
                autoclickerAnalysis.getConfidenceLevel()));
            report.append(String.format("Average CPS: %.1f\n", autoclickerAnalysis.getAverageCPS()));
            report.append(String.format("Consistency: %.3f\n", autoclickerAnalysis.getConsistencyScore()));
            report.append(String.format("Periodicity: %.3f\n\n", autoclickerAnalysis.getPeriodicityScore()));
        }
        
        report.append("--- Advanced Metrics ---\n");
        report.append(String.format("Permutation Entropy: %.3f\n", permutationEntropy));
        report.append(String.format("Sample Entropy: %.3f\n", sampleEntropy));
        report.append(String.format("Spectral Flatness: %.3f\n", spectralFlatness));
        report.append(String.format("Hurst Exponent: %.3f\n", hurstExponent));
        
        return report.toString();
    }
}
