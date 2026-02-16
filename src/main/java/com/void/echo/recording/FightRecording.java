package com.void.echo.recording;

import com.void.echo.data.ClickData;
import com.void.echo.data.MovementData;
import com.void.echo.data.RotationData;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records a complete fight for later replay and deep analysis
 * 
 * This is Layer 6: Post-Fight Neural Review
 * 
 * Captures:
 * - All player actions (movement, rotation, attacks)
 * - All entity states (positions, velocities)
 * - Timing information (tick-perfect)
 * - Combat outcomes
 * 
 * Can be replayed deterministically for offline analysis
 */
@Data
public class FightRecording {
    
    private final UUID recordingId;
    private final UUID attackerId;
    private final String attackerName;
    private final long startTime;
    private long endTime;
    
    private final List<FightFrame> frames;
    private final Map<UUID, String> involvedPlayers;
    private final Map<Integer, String> involvedEntities;
    
    // Fight metadata
    private int totalHits;
    private int totalMisses;
    private double avgReachDistance;
    private double maxReachDistance;
    private int comboDuration; // Consecutive hits
    private boolean fightWon;
    
    // Analysis results (computed post-fight)
    private FightAnalysis analysis;
    
    public FightRecording(UUID playerId, String playerName) {
        this.recordingId = UUID.randomUUID();
        this.attackerId = playerId;
        this.attackerName = playerName;
        this.startTime = System.currentTimeMillis();
        this.frames = new ArrayList<>();
        this.involvedPlayers = new HashMap<>();
        this.involvedEntities = new HashMap<>();
        
        this.involvedPlayers.put(playerId, playerName);
    }
    
    /**
     * Add a frame to the recording
     */
    public void addFrame(FightFrame frame) {
        frames.add(frame);
        
        // Update statistics
        if (frame.isAttackFrame()) {
            if (frame.wasHit()) {
                totalHits++;
                
                if (frame.getTargetDistance() > maxReachDistance) {
                    maxReachDistance = frame.getTargetDistance();
                }
            } else {
                totalMisses++;
            }
        }
    }
    
    /**
     * Finalize the recording
     */
    public void finalize(boolean won) {
        this.endTime = System.currentTimeMillis();
        this.fightWon = won;
        
        // Calculate averages
        if (totalHits > 0) {
            double totalReach = frames.stream()
                    .filter(FightFrame::isAttackFrame)
                    .filter(FightFrame::wasHit)
                    .mapToDouble(FightFrame::getTargetDistance)
                    .sum();
            this.avgReachDistance = totalReach / totalHits;
        }
        
        // Calculate combo duration
        calculateComboDuration();
    }
    
    /**
     * Calculate longest combo
     */
    private void calculateComboDuration() {
        int currentCombo = 0;
        int maxCombo = 0;
        
        for (FightFrame frame : frames) {
            if (frame.isAttackFrame() && frame.wasHit()) {
                currentCombo++;
                if (currentCombo > maxCombo) {
                    maxCombo = currentCombo;
                }
            } else if (frame.isAttackFrame()) {
                currentCombo = 0;
            }
        }
        
        this.comboDuration = maxCombo;
    }
    
    /**
     * Get fight duration in milliseconds
     */
    public long getDuration() {
        return endTime - startTime;
    }
    
    /**
     * Get hit accuracy
     */
    public double getAccuracy() {
        int totalAttempts = totalHits + totalMisses;
        return totalAttempts > 0 ? (double) totalHits / totalAttempts : 0.0;
    }
    
    /**
     * Individual frame in fight recording
     */
    @Data
    public static class FightFrame {
        private final long timestamp;
        private final int tick;
        
        // Player state
        private final Location playerLocation;
        private final float yaw;
        private final float pitch;
        private final double velocityX;
        private final double velocityY;
        private final double velocityZ;
        private final boolean sprinting;
        private final boolean sneaking;
        private final boolean onGround;
        
        // Action data
        private final boolean isAttackFrame;
        private final boolean wasHit;
        private final Integer targetEntityId;
        private final double targetDistance;
        private final float yawToTarget;
        private final float pitchToTarget;
        
        // Context
        private final int nearbyEntities;
        private final boolean underAttack;
        
        /**
         * Get angle difference to target
         */
        public double getAngleDifference() {
            if (!isAttackFrame || targetEntityId == null) return 0.0;
            
            double yawDiff = Math.abs(normalizeAngle(yaw - yawToTarget));
            double pitchDiff = Math.abs(pitch - pitchToTarget);
            
            return Math.sqrt(yawDiff*yawDiff + pitchDiff*pitchDiff);
        }
        
        private double normalizeAngle(float angle) {
            while (angle > 180) angle -= 360;
            while (angle < -180) angle += 360;
            return angle;
        }
    }
    
    /**
     * Deep analysis results of a fight
     */
    @Data
    public static class FightAnalysis {
        // Aim analysis
        private double aimCurvatureScore;
        private double aimConsistencyScore;
        private double microJitterScore;
        private double snapDetectionScore;
        
        // Attack pattern analysis
        private double attackTimingConsistency;
        private double targetSwitchingScore;
        private double reachConsistency;
        
        // Movement analysis
        private double movementNaturalness;
        private double strafePatternScore;
        private double velocityConsistency;
        
        // Overall scores
        private double combatNaturalnessScore; // 0-100
        private double suspicionLevel;         // 0-1
        
        private List<String> anomalies;
        
        public FightAnalysis() {
            this.anomalies = new ArrayList<>();
        }
        
        public void addAnomaly(String description) {
            anomalies.add(description);
        }
        
        public boolean hasCriticalAnomalies() {
            return suspicionLevel > 0.75;
        }
        
        public String getSuspicionCategory() {
            if (suspicionLevel >= 0.8) return "CRITICAL";
            if (suspicionLevel >= 0.6) return "HIGH";
            if (suspicionLevel >= 0.4) return "MEDIUM";
            if (suspicionLevel >= 0.2) return "LOW";
            return "CLEAN";
        }
    }
    
    /**
     * Simplified builder for creating frames
     */
    public static class FrameBuilder {
        private long timestamp;
        private int tick;
        private Location location;
        private float yaw, pitch;
        private double vx, vy, vz;
        private boolean sprinting, sneaking, onGround;
        private boolean isAttack;
        private boolean hit;
        private Integer targetId;
        private double distance;
        private float yawToTarget, pitchToTarget;
        private int nearby;
        private boolean underAttack;
        
        public FrameBuilder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public FrameBuilder tick(int tick) {
            this.tick = tick;
            return this;
        }
        
        public FrameBuilder location(Location loc) {
            this.location = loc;
            return this;
        }
        
        public FrameBuilder rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            return this;
        }
        
        public FrameBuilder velocity(double x, double y, double z) {
            this.vx = x;
            this.vy = y;
            this.vz = z;
            return this;
        }
        
        public FrameBuilder state(boolean sprinting, boolean sneaking, boolean onGround) {
            this.sprinting = sprinting;
            this.sneaking = sneaking;
            this.onGround = onGround;
            return this;
        }
        
        public FrameBuilder attack(int targetId, double distance, float yawTo, float pitchTo, boolean hit) {
            this.isAttack = true;
            this.targetId = targetId;
            this.distance = distance;
            this.yawToTarget = yawTo;
            this.pitchToTarget = pitchTo;
            this.hit = hit;
            return this;
        }
        
        public FrameBuilder context(int nearbyEntities, boolean underAttack) {
            this.nearby = nearbyEntities;
            this.underAttack = underAttack;
            return this;
        }
        
        public FightFrame build() {
            return new FightFrame(
                timestamp, tick, location, yaw, pitch, vx, vy, vz,
                sprinting, sneaking, onGround,
                isAttack, hit, targetId, distance, yawToTarget, pitchToTarget,
                nearby, underAttack
            );
        }
    }
}
