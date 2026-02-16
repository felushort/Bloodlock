package com.void.echo.detection;

import com.void.echo.data.MovementData;
import com.void.echo.util.MathUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Velocity modification detection
 * 
 * Detects clients that modify movement speed, jump height, or velocity handling:
 * - Speed hacks (moving faster than possible)
 * - Flight (ignoring gravity)
 * - No-slowdown (no speed reduction in water/cobweb/soul sand)
 * - Anti-knockback (ignoring velocity changes)
 * - Step hacks (climbing blocks without jumping)
 * 
 * Detection methods:
 * 1. Speed analysis (max speed, acceleration)
 * 2. Vertical movement (jump height, fall patterns)
 * 3. Environmental speed (water/web/soul sand)
 * 4. Knockback analysis (velocity change response)
 * 5. Physics consistency (gravity, friction)
 */
public class VelocityDetector {
    
    // Physics constants
    private static final double MAX_WALK_SPEED = 0.215;      // blocks/tick
    private static final double MAX_SPRINT_SPEED = 0.28;     // blocks/tick
    private static final double MAX_FLY_SPEED = 0.39;        // Creative fly
    private static final double GRAVITY = 0.08;              // blocks/tick²
    private static final double JUMP_VELOCITY = 0.42;        // Initial jump
    private static final double TERMINAL_VELOCITY = 3.92;    // Max fall speed
    
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.75;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.50;
    
    /**
     * Movement sample
     */
    @Data
    public static class MovementSample {
        private final long timestamp;
        private final double horizontalSpeed;
        private final double verticalSpeed;
        private final boolean onGround;
        private final boolean sprinting;
        private final boolean inWater;
        private final boolean inWeb;
        private final boolean inSoulSand;
        private final double expectedMaxSpeed;
        private final boolean receivedKnockback;
        private final double knockbackVelocity;
    }
    
    /**
     * Analyze movement patterns
     */
    public static VelocityAnalysis analyze(List<MovementSample> samples) {
        if (samples.size() < 20) {
            return new VelocityAnalysis();
        }
        
        VelocityAnalysis analysis = new VelocityAnalysis();
        
        // 1. Horizontal speed analysis
        analyzeHorizontalSpeed(samples, analysis);
        
        // 2. Vertical movement analysis
        analyzeVerticalMovement(samples, analysis);
        
        // 3. Environmental speed
        analyzeEnvironmentalSpeed(samples, analysis);
        
        // 4. Knockback analysis
        analyzeKnockbackResponse(samples, analysis);
        
        // 5. Physics consistency
        analyzePhysicsConsistency(samples, analysis);
        
        // 6. Acceleration analysis
        analyzeAcceleration(samples, analysis);
        
        // Calculate final confidence
        double confidence = calculateVelocityConfidence(analysis);
        analysis.setVelocityConfidence(confidence);
        
        return analysis;
    }
    
    /**
     * Analyze horizontal speed
     */
    private static void analyzeHorizontalSpeed(List<MovementSample> samples, VelocityAnalysis analysis) {
        double[] speeds = samples.stream()
                .mapToDouble(MovementSample::getHorizontalSpeed)
                .toArray();
        
        double maxSpeed = MathUtil.max(speeds);
        double avgSpeed = MathUtil.mean(speeds);
        
        analysis.setMaxHorizontalSpeed(maxSpeed);
        analysis.setAverageHorizontalSpeed(avgSpeed);
        
        // Count violations
        long violations = samples.stream()
                .filter(s -> s.getHorizontalSpeed() > s.getExpectedMaxSpeed() * 1.1)
                .count();
        
        analysis.setSpeedViolations((int) violations);
        
        double violationRatio = (double) violations / samples.size();
        analysis.setSpeedViolationRatio(violationRatio);
        
        // Obvious speed hack
        if (maxSpeed > MAX_SPRINT_SPEED * 1.5) {
            analysis.addAnomaly(String.format("Obvious speed hack: %.3f blocks/tick", maxSpeed));
        } else if (maxSpeed > MAX_SPRINT_SPEED * 1.2) {
            analysis.addAnomaly(String.format("Suspicious speed: %.3f blocks/tick", maxSpeed));
        }
    }
    
    /**
     * Analyze vertical movement
     */
    private static void analyzeVerticalMovement(List<MovementSample> samples, VelocityAnalysis analysis) {
        // Analyze jumps
        List<Double> jumpHeights = new ArrayList<>();
        List<Double> fallSpeeds = new ArrayList<>();
        
        for (int i = 1; i < samples.size(); i++) {
            MovementSample prev = samples.get(i - 1);
            MovementSample curr = samples.get(i);
            
            // Detect jump
            if (prev.isOnGround() && !curr.isOnGround() && curr.getVerticalSpeed() > 0) {
                jumpHeights.add(curr.getVerticalSpeed());
            }
            
            // Detect fall
            if (!curr.isOnGround() && curr.getVerticalSpeed() < 0) {
                fallSpeeds.add(Math.abs(curr.getVerticalSpeed()));
            }
        }
        
        if (!jumpHeights.isEmpty()) {
            double[] jumps = jumpHeights.stream().mapToDouble(Double::doubleValue).toArray();
            double maxJump = MathUtil.max(jumps);
            double avgJump = MathUtil.mean(jumps);
            
            analysis.setMaxJumpHeight(maxJump);
            analysis.setAverageJumpHeight(avgJump);
            
            // High jump detection
            if (maxJump > JUMP_VELOCITY * 1.3) {
                analysis.addAnomaly(String.format("Abnormal jump height: %.3f", maxJump));
            }
        }
        
        if (!fallSpeeds.isEmpty()) {
            double[] falls = fallSpeeds.stream().mapToDouble(Double::doubleValue).toArray();
            double maxFall = MathUtil.max(falls);
            
            analysis.setMaxFallSpeed(maxFall);
            
            // Terminal velocity exceeded
            if (maxFall > TERMINAL_VELOCITY * 1.1) {
                analysis.addAnomaly(String.format("Exceeded terminal velocity: %.3f", maxFall));
            }
        }
        
        // Detect hovering (staying at same Y)
        int hoverCount = 0;
        for (int i = 1; i < samples.size(); i++) {
            MovementSample curr = samples.get(i);
            if (!curr.isOnGround() && Math.abs(curr.getVerticalSpeed()) < 0.01) {
                hoverCount++;
            }
        }
        
        double hoverRatio = (double) hoverCount / samples.size();
        analysis.setHoverRatio(hoverRatio);
        
        if (hoverRatio > 0.3) {
            analysis.addAnomaly("Frequent hovering detected (possible flight)");
        }
    }
    
    /**
     * Analyze environmental speed
     */
    private static void analyzeEnvironmentalSpeed(List<MovementSample> samples, VelocityAnalysis analysis) {
        // Speed in water
        List<MovementSample> waterSamples = samples.stream()
                .filter(MovementSample::isInWater)
                .toList();
        
        if (waterSamples.size() > 5) {
            double[] waterSpeeds = waterSamples.stream()
                    .mapToDouble(MovementSample::getHorizontalSpeed)
                    .toArray();
            
            double avgWaterSpeed = MathUtil.mean(waterSpeeds);
            analysis.setAverageWaterSpeed(avgWaterSpeed);
            
            // Water should slow down significantly
            if (avgWaterSpeed > MAX_WALK_SPEED) {
                analysis.addAnomaly("No slowdown in water");
            }
        }
        
        // Speed in cobweb
        List<MovementSample> webSamples = samples.stream()
                .filter(MovementSample::isInWeb)
                .toList();
        
        if (webSamples.size() > 3) {
            double[] webSpeeds = webSamples.stream()
                    .mapToDouble(MovementSample::getHorizontalSpeed)
                    .toArray();
            
            double avgWebSpeed = MathUtil.mean(webSpeeds);
            analysis.setAverageWebSpeed(avgWebSpeed);
            
            // Cobweb should reduce speed drastically
            if (avgWebSpeed > 0.05) {
                analysis.addAnomaly("No slowdown in cobwebs");
            }
        }
        
        // Speed on soul sand
        List<MovementSample> soulSandSamples = samples.stream()
                .filter(MovementSample::isInSoulSand)
                .toList();
        
        if (soulSandSamples.size() > 3) {
            double[] soulSpeeds = soulSandSamples.stream()
                    .mapToDouble(MovementSample::getHorizontalSpeed)
                    .toArray();
            
            double avgSoulSpeed = MathUtil.mean(soulSpeeds);
            analysis.setAverageSoulSandSpeed(avgSoulSpeed);
            
            // Soul sand should reduce speed
            if (avgSoulSpeed > MAX_WALK_SPEED * 0.9) {
                analysis.addAnomaly("No slowdown on soul sand");
            }
        }
    }
    
    /**
     * Analyze knockback response
     */
    private static void analyzeKnockbackResponse(List<MovementSample> samples, VelocityAnalysis analysis) {
        List<MovementSample> knockbackSamples = samples.stream()
                .filter(MovementSample::isReceivedKnockback)
                .toList();
        
        if (knockbackSamples.isEmpty()) return;
        
        int reducedKnockback = 0;
        int totalKnockback = 0;
        
        for (MovementSample sample : knockbackSamples) {
            totalKnockback++;
            
            // Expected knockback vs actual speed change
            double expected = sample.getKnockbackVelocity();
            double actual = sample.getHorizontalSpeed();
            
            // If actual much less than expected = anti-knockback
            if (actual < expected * 0.5) {
                reducedKnockback++;
            }
        }
        
        double antiKnockbackRatio = (double) reducedKnockback / totalKnockback;
        analysis.setAntiKnockbackRatio(antiKnockbackRatio);
        
        if (antiKnockbackRatio > 0.6) {
            analysis.addAnomaly(String.format("Anti-knockback detected (%.1f%% reduced)", antiKnockbackRatio * 100));
        }
    }
    
    /**
     * Analyze physics consistency
     */
    private static void analyzePhysicsConsistency(List<MovementSample> samples, VelocityAnalysis analysis) {
        // Check gravity application
        int gravityViolations = 0;
        
        for (int i = 1; i < samples.size(); i++) {
            MovementSample prev = samples.get(i - 1);
            MovementSample curr = samples.get(i);
            
            // If in air, should be accelerating downward
            if (!prev.isOnGround() && !curr.isOnGround() && 
                !prev.isInWater() && !curr.isInWater()) {
                
                double expectedVel = prev.getVerticalSpeed() - GRAVITY;
                double actualVel = curr.getVerticalSpeed();
                
                // Allow some tolerance
                if (Math.abs(actualVel - expectedVel) > 0.1 && actualVel > expectedVel) {
                    gravityViolations++;
                }
            }
        }
        
        double gravityViolationRatio = (double) gravityViolations / samples.size();
        analysis.setGravityViolationRatio(gravityViolationRatio);
        
        if (gravityViolationRatio > 0.2) {
            analysis.addAnomaly("Inconsistent gravity application");
        }
    }
    
    /**
     * Analyze acceleration
     */
    private static void analyzeAcceleration(List<MovementSample> samples, VelocityAnalysis analysis) {
        List<Double> accelerations = new ArrayList<>();
        
        for (int i = 1; i < samples.size(); i++) {
            MovementSample prev = samples.get(i - 1);
            MovementSample curr = samples.get(i);
            
            double acceleration = curr.getHorizontalSpeed() - prev.getHorizontalSpeed();
            accelerations.add(Math.abs(acceleration));
        }
        
        if (accelerations.isEmpty()) return;
        
        double[] accelArray = accelerations.stream().mapToDouble(Double::doubleValue).toArray();
        double maxAccel = MathUtil.max(accelArray);
        double avgAccel = MathUtil.mean(accelArray);
        
        analysis.setMaxAcceleration(maxAccel);
        analysis.setAverageAcceleration(avgAccel);
        
        // Instant max speed = suspicious
        if (maxAccel > 0.2) {
            analysis.addAnomaly(String.format("Instant acceleration: %.3f", maxAccel));
        }
    }
    
    /**
     * Calculate final confidence
     */
    private static double calculateVelocityConfidence(VelocityAnalysis analysis) {
        double confidence = 0.0;
        
        // Speed violations
        if (analysis.getSpeedViolationRatio() > 0.5) {
            confidence += 0.4;
        } else if (analysis.getSpeedViolationRatio() > 0.2) {
            confidence += 0.2;
        }
        
        // Obvious speed
        if (analysis.getMaxHorizontalSpeed() > MAX_SPRINT_SPEED * 1.5) {
            confidence += 0.3;
        } else if (analysis.getMaxHorizontalSpeed() > MAX_SPRINT_SPEED * 1.2) {
            confidence += 0.15;
        }
        
        // Anti-knockback
        if (analysis.getAntiKnockbackRatio() > 0.6) {
            confidence += 0.2;
        }
        
        // Environmental speed (no slowdown)
        if (analysis.getAverageWaterSpeed() > MAX_WALK_SPEED ||
            analysis.getAverageWebSpeed() > 0.05) {
            confidence += 0.15;
        }
        
        // Hovering (flight)
        if (analysis.getHoverRatio() > 0.3) {
            confidence += 0.15;
        }
        
        // Gravity violations
        if (analysis.getGravityViolationRatio() > 0.2) {
            confidence += 0.1;
        }
        
        // Anomalies
        confidence += Math.min(0.2, analysis.getAnomalies().size() * 0.05);
        
        return Math.min(1.0, confidence);
    }
    
    /**
     * Analysis result
     */
    @Data
    public static class VelocityAnalysis {
        private double maxHorizontalSpeed;
        private double averageHorizontalSpeed;
        private int speedViolations;
        private double speedViolationRatio;
        
        private double maxJumpHeight;
        private double averageJumpHeight;
        private double maxFallSpeed;
        private double hoverRatio;
        
        private double averageWaterSpeed;
        private double averageWebSpeed;
        private double averageSoulSandSpeed;
        
        private double antiKnockbackRatio;
        private double gravityViolationRatio;
        
        private double maxAcceleration;
        private double averageAcceleration;
        
        private double velocityConfidence = 0.0;
        
        private List<String> anomalies = new ArrayList<>();
        
        public void addAnomaly(String anomaly) {
            anomalies.add(anomaly);
        }
        
        public String getConfidenceLevel() {
            if (velocityConfidence >= HIGH_CONFIDENCE_THRESHOLD) {
                return "HIGH";
            } else if (velocityConfidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
                return "MEDIUM";
            } else {
                return "LOW";
            }
        }
        
        public boolean isLikelyVelocityMod() {
            return velocityConfidence >= HIGH_CONFIDENCE_THRESHOLD;
        }
        
        public boolean hasSpeedHack() {
            return maxHorizontalSpeed > MAX_SPRINT_SPEED * 1.3;
        }
        
        public boolean hasFlight() {
            return hoverRatio > 0.3 || gravityViolationRatio > 0.3;
        }
        
        public boolean hasAntiKnockback() {
            return antiKnockbackRatio > 0.6;
        }
    }
}
