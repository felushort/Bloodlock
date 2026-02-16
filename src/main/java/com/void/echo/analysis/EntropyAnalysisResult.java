package com.void.echo.analysis;

import lombok.Data;

/**
 * Results of entropy analysis
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
    
    // Flags
    private boolean suspicious;             // Below 40
    private boolean extremelySuspicious;    // Below 20
    
    // Sample counts
    private int rotationSamples;
    private int clickSamples;
    
    public EntropyAnalysisResult() {
        this.humanProbabilityScore = 100.0; // Innocent until proven guilty
        this.suspicious = false;
        this.extremelySuspicious = false;
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
        if (extremelySuspicious) {
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
}
