package com.void.echo.recording;

import com.void.echo.detection.AimbotDetector;
import com.void.echo.detection.KillAuraDetector;
import com.void.echo.util.MathUtil;
import com.void.echo.util.SignalProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Deep analysis of fight recordings
 * 
 * Performs offline statistical analysis of recorded fights
 * using full data context for maximum accuracy
 */
public class FightAnalyzer {
    
    /**
     * Analyze a complete fight recording
     */
    public static FightRecording.FightAnalysis analyze(FightRecording recording) {
        FightRecording.FightAnalysis analysis = new FightRecording.FightAnalysis();
        
        List<FightRecording.FightFrame> frames = recording.getFrames();
        
        if (frames.size() < 10) {
            // Not enough data
            analysis.setCombatNaturalnessScore(50.0);
            analysis.setSuspicionLevel(0.0);
            return analysis;
        }
        
        // 1. Aim curvature analysis
        analyzeAimCurvature(frames, analysis);
        
        // 2. Aim consistency analysis
        analyzeAimConsistency(frames, analysis);
        
        // 3. Micro-jitter analysis
        analyzeMicroJitter(frames, analysis);
        
        // 4. Snap detection
        analyzeSnaps(frames, analysis);
        
        // 5. Attack timing
        analyzeAttackTiming(frames, analysis);
        
        // 6. Target switching
        analyzeTargetSwitching(frames, analysis);
        
        // 7. Reach consistency
        analyzeReachConsistency(frames, analysis);
        
        // 8. Movement naturalness
        analyzeMovementNaturalness(frames, analysis);
        
        // 9. Strafe patterns
        analyzeStrafePatterns(frames, analysis);
        
        // 10. Velocity consistency
        analyzeVelocityConsistency(frames, analysis);
        
        // Calculate overall scores
        calculateOverallScores(analysis);
        
        return analysis;
    }
    
    /**
     * Analyze aim curvature
     */
    private static void analyzeAimCurvature(List<FightRecording.FightFrame> frames, 
                                           FightRecording.FightAnalysis analysis) {
        // Extract yaw and pitch sequences
        List<Double> yaws = new ArrayList<>();
        List<Double> pitches = new ArrayList<>();
        
        for (FightRecording.FightFrame frame : frames) {
            yaws.add((double) frame.getYaw());
            pitches.add((double) frame.getPitch());
        }
        
        if (yaws.size() < 3) {
            analysis.setAimCurvatureScore(1.0);
            return;
        }
        
        // Calculate yaw and pitch deltas
        double[] yawDeltas = new double[yaws.size() - 1];
        double[] pitchDeltas = new double[pitches.size() - 1];
        
        for (int i = 1; i < yaws.size(); i++) {
            yawDeltas[i-1] = MathUtil.angleDifference(yaws.get(i).floatValue(), yaws.get(i-1).floatValue());
            pitchDeltas[i-1] = pitches.get(i) - pitches.get(i-1);
        }
        
        // Calculate curvature
        double curvature = MathUtil.pathCurvature(yawDeltas, pitchDeltas);
        
        // Normalize (higher curvature = more natural)
        double normalizedCurvature = Math.min(1.0, curvature * 4.0);
        analysis.setAimCurvatureScore(normalizedCurvature);
        
        if (normalizedCurvature < 0.3) {
            analysis.addAnomaly("Suspiciously straight aim path (low curvature)");
        }
    }
    
    /**
     * Analyze aim consistency
     */
    private static void analyzeAimConsistency(List<FightRecording.FightFrame> frames,
                                             FightRecording.FightAnalysis analysis) {
        List<Double> angularVelocities = new ArrayList<>();
        
        for (int i = 1; i < frames.size(); i++) {
            FightRecording.FightFrame prev = frames.get(i-1);
            FightRecording.FightFrame curr = frames.get(i);
            
            double yawDiff = Math.abs(MathUtil.angleDifference(curr.getYaw(), prev.getYaw()));
            double pitchDiff = Math.abs(curr.getPitch() - prev.getPitch());
            
            double angularVel = Math.sqrt(yawDiff*yawDiff + pitchDiff*pitchDiff);
            angularVelocities.add(angularVel);
        }
        
        if (angularVelocities.isEmpty()) {
            analysis.setAimConsistencyScore(0.5);
            return;
        }
        
        double[] vels = angularVelocities.stream().mapToDouble(Double::doubleValue).toArray();
        double mean = MathUtil.mean(vels);
        double stdDev = MathUtil.standardDeviation(vels);
        
        if (mean < 0.1) {
            analysis.setAimConsistencyScore(0.5);
            return;
        }
        
        double cv = stdDev / mean;
        
        // Low CV = too consistent = suspicious
        double consistencyScore = cv < 0.3 ? (1.0 - cv / 0.3) : 0.0;
        analysis.setAimConsistencyScore(consistencyScore);
        
        if (consistencyScore > 0.7) {
            analysis.addAnomaly(String.format("Unnaturally consistent aim velocity (CV: %.3f)", cv));
        }
    }
    
    /**
     * Analyze micro-jitter
     */
    private static void analyzeMicroJitter(List<FightRecording.FightFrame> frames,
                                          FightRecording.FightAnalysis analysis) {
        if (frames.size() < 10) {
            analysis.setMicroJitterScore(0.5);
            return;
        }
        
        double[] yawDeltas = new double[frames.size() - 1];
        for (int i = 1; i < frames.size(); i++) {
            yawDeltas[i-1] = MathUtil.angleDifference(
                frames.get(i).getYaw(), 
                frames.get(i-1).getYaw()
            );
        }
        
        // Remove low-frequency components
        double[] smoothed = MathUtil.movingAverage(yawDeltas, 5);
        double[] highFreq = new double[yawDeltas.length];
        
        for (int i = 0; i < yawDeltas.length; i++) {
            highFreq[i] = yawDeltas[i] - smoothed[i];
        }
        
        // Calculate energy in high-frequency components
        double energy = 0.0;
        for (double h : highFreq) {
            energy += h * h;
        }
        energy /= highFreq.length;
        
        // Normalize (higher energy = more jitter = more human)
        double jitterScore = Math.min(1.0, energy / 0.01);
        analysis.setMicroJitterScore(jitterScore);
        
        if (jitterScore < 0.2) {
            analysis.addAnomaly("Lack of natural micro-jitter in aim");
        }
    }
    
    /**
     * Analyze snap behavior
     */
    private static void analyzeSnaps(List<FightRecording.FightFrame> frames,
                                    FightRecording.FightAnalysis analysis) {
        List<Double> angularVels = new ArrayList<>();
        
        for (int i = 1; i < frames.size(); i++) {
            FightRecording.FightFrame prev = frames.get(i-1);
            FightRecording.FightFrame curr = frames.get(i);
            
            double yawDiff = Math.abs(MathUtil.angleDifference(curr.getYaw(), prev.getYaw()));
            double pitchDiff = Math.abs(curr.getPitch() - prev.getPitch());
            
            angularVels.add(Math.sqrt(yawDiff*yawDiff + pitchDiff*pitchDiff));
        }
        
        if (angularVels.isEmpty()) {
            analysis.setSnapDetectionScore(0.0);
            return;
        }
        
        double[] vels = angularVels.stream().mapToDouble(Double::doubleValue).toArray();
        double mean = MathUtil.mean(vels);
        double stdDev = MathUtil.standardDeviation(vels);
        
        if (stdDev < 0.1) {
            analysis.setSnapDetectionScore(0.0);
            return;
        }
        
        // Count snaps (> 3 std dev from mean)
        int snapCount = 0;
        for (double vel : vels) {
            if (vel > mean + 3 * stdDev && vel > 10.0) {
                snapCount++;
            }
        }
        
        double snapRatio = (double) snapCount / vels.length;
        double snapScore = Math.min(1.0, snapRatio / 0.1);
        
        analysis.setSnapDetectionScore(snapScore);
        
        if (snapScore > 0.5) {
            analysis.addAnomaly(String.format("Frequent aim snaps detected (%d snaps)", snapCount));
        }
    }
    
    /**
     * Analyze attack timing
     */
    private static void analyzeAttackTiming(List<FightRecording.FightFrame> frames,
                                           FightRecording.FightAnalysis analysis) {
        List<Long> attackTimestamps = new ArrayList<>();
        
        for (FightRecording.FightFrame frame : frames) {
            if (frame.isAttackFrame()) {
                attackTimestamps.add(frame.getTimestamp());
            }
        }
        
        if (attackTimestamps.size() < 3) {
            analysis.setAttackTimingConsistency(0.0);
            return;
        }
        
        // Calculate intervals
        double[] intervals = new double[attackTimestamps.size() - 1];
        for (int i = 1; i < attackTimestamps.size(); i++) {
            intervals[i-1] = attackTimestamps.get(i) - attackTimestamps.get(i-1);
        }
        
        double mean = MathUtil.mean(intervals);
        double stdDev = MathUtil.standardDeviation(intervals);
        
        if (mean < 1.0) {
            analysis.setAttackTimingConsistency(0.0);
            return;
        }
        
        double cv = stdDev / mean;
        
        // Low CV = too consistent = suspicious
        double consistency = cv < 0.15 ? (1.0 - cv / 0.15) : 0.0;
        analysis.setAttackTimingConsistency(consistency);
        
        if (consistency > 0.7) {
            analysis.addAnomaly("Perfect attack timing consistency");
        }
    }
    
    /**
     * Analyze target switching
     */
    private static void analyzeTargetSwitching(List<FightRecording.FightFrame> frames,
                                              FightRecording.FightAnalysis analysis) {
        List<Integer> targets = new ArrayList<>();
        
        for (FightRecording.FightFrame frame : frames) {
            if (frame.isAttackFrame() && frame.getTargetEntityId() != null) {
                targets.add(frame.getTargetEntityId());
            }
        }
        
        if (targets.size() < 3) {
            analysis.setTargetSwitchingScore(0.0);
            return;
        }
        
        // Count switches
        int switches = 0;
        for (int i = 1; i < targets.size(); i++) {
            if (!targets.get(i).equals(targets.get(i-1))) {
                switches++;
            }
        }
        
        double switchRatio = (double) switches / (targets.size() - 1);
        
        // Very high switch rate = suspicious (KillAura)
        double switchScore = switchRatio > 0.7 ? (switchRatio - 0.5) / 0.5 : 0.0;
        analysis.setTargetSwitchingScore(switchScore);
        
        if (switchScore > 0.6) {
            analysis.addAnomaly("Rapid target switching (possible KillAura)");
        }
    }
    
    /**
     * Analyze reach consistency
     */
    private static void analyzeReachConsistency(List<FightRecording.FightFrame> frames,
                                               FightRecording.FightAnalysis analysis) {
        List<Double> reaches = new ArrayList<>();
        
        for (FightRecording.FightFrame frame : frames) {
            if (frame.isAttackFrame() && frame.wasHit()) {
                reaches.add(frame.getTargetDistance());
            }
        }
        
        if (reaches.size() < 5) {
            analysis.setReachConsistency(0.0);
            return;
        }
        
        double[] reachArray = reaches.stream().mapToDouble(Double::doubleValue).toArray();
        
        // Count long-reach hits
        int longReach = 0;
        for (double reach : reachArray) {
            if (reach > 2.9 && reach <= 3.1) {
                longReach++;
            }
        }
        
        double longReachRatio = (double) longReach / reachArray.length;
        
        // High ratio of max-reach hits = suspicious
        double reachScore = longReachRatio > 0.5 ? (longReachRatio - 0.5) / 0.5 : 0.0;
        analysis.setReachConsistency(reachScore);
        
        if (reachScore > 0.6) {
            analysis.addAnomaly(String.format("Consistent maximum reach (%.1f%% at 2.9+ blocks)", 
                longReachRatio * 100));
        }
    }
    
    /**
     * Analyze movement naturalness
     */
    private static void analyzeMovementNaturalness(List<FightRecording.FightFrame> frames,
                                                  FightRecording.FightAnalysis analysis) {
        if (frames.size() < 10) {
            analysis.setMovementNaturalness(1.0);
            return;
        }
        
        // Extract velocity magnitudes
        double[] velocities = new double[frames.size()];
        for (int i = 0; i < frames.size(); i++) {
            FightRecording.FightFrame frame = frames.get(i);
            velocities[i] = Math.sqrt(
                frame.getVelocityX() * frame.getVelocityX() +
                frame.getVelocityZ() * frame.getVelocityZ()
            );
        }
        
        // Calculate variance
        double variance = MathUtil.variance(velocities);
        double entropy = MathUtil.entropy(velocities);
        
        // High variance and entropy = natural movement
        double naturalness = Math.min(1.0, (variance * 0.5 + entropy * 0.1));
        analysis.setMovementNaturalness(naturalness);
        
        if (naturalness < 0.3) {
            analysis.addAnomaly("Unnatural movement pattern (low variance)");
        }
    }
    
    /**
     * Analyze strafe patterns
     */
    private static void analyzeStrafePatterns(List<FightRecording.FightFrame> frames,
                                             FightRecording.FightAnalysis analysis) {
        // This would analyze W-tapping, S-tapping, etc.
        // Simplified for now
        analysis.setStrafePatternScore(0.0);
    }
    
    /**
     * Analyze velocity consistency
     */
    private static void analyzeVelocityConsistency(List<FightRecording.FightFrame> frames,
                                                  FightRecording.FightAnalysis analysis) {
        double[] velocities = new double[frames.size()];
        
        for (int i = 0; i < frames.size(); i++) {
            FightRecording.FightFrame frame = frames.get(i);
            velocities[i] = Math.sqrt(
                frame.getVelocityX() * frame.getVelocityX() +
                frame.getVelocityY() * frame.getVelocityY() +
                frame.getVelocityZ() * frame.getVelocityZ()
            );
        }
        
        double mean = MathUtil.mean(velocities);
        double stdDev = MathUtil.standardDeviation(velocities);
        
        if (mean < 0.01) {
            analysis.setVelocityConsistency(0.0);
            return;
        }
        
        double cv = stdDev / mean;
        
        // Very low CV = suspicious (velocity modification)
        double consistency = cv < 0.2 ? (1.0 - cv / 0.2) : 0.0;
        analysis.setVelocityConsistency(consistency);
        
        if (consistency > 0.7) {
            analysis.addAnomaly("Unnaturally consistent velocity");
        }
    }
    
    /**
     * Calculate overall scores
     */
    private static void calculateOverallScores(FightRecording.FightAnalysis analysis) {
        // Combine all metrics into overall scores
        
        // Combat naturalness (0-100, higher = more natural)
        double naturalness = 100.0;
        
        // Penalize for suspicious aim
        naturalness -= analysis.getAimConsistencyScore() * 20.0;
        naturalness -= (1.0 - analysis.getAimCurvatureScore()) * 20.0;
        naturalness -= (1.0 - analysis.getMicroJitterScore()) * 15.0;
        naturalness -= analysis.getSnapDetectionScore() * 15.0;
        
        // Penalize for suspicious attacks
        naturalness -= analysis.getAttackTimingConsistency() * 10.0;
        naturalness -= analysis.getTargetSwitchingScore() * 10.0;
        naturalness -= analysis.getReachConsistency() * 10.0;
        
        analysis.setCombatNaturalnessScore(Math.max(0, Math.min(100, naturalness)));
        
        // Suspicion level (0-1, higher = more suspicious)
        double suspicion = 0.0;
        
        suspicion += analysis.getAimConsistencyScore() * 0.15;
        suspicion += (1.0 - analysis.getAimCurvatureScore()) * 0.15;
        suspicion += (1.0 - analysis.getMicroJitterScore()) * 0.10;
        suspicion += analysis.getSnapDetectionScore() * 0.15;
        suspicion += analysis.getAttackTimingConsistency() * 0.10;
        suspicion += analysis.getTargetSwitchingScore() * 0.15;
        suspicion += analysis.getReachConsistency() * 0.10;
        suspicion += analysis.getVelocityConsistency() * 0.10;
        
        analysis.setSuspicionLevel(Math.max(0, Math.min(1.0, suspicion)));
    }
}
