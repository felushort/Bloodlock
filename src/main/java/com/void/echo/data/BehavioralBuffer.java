package com.void.echo.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * Stores short-term behavioral data for a single player
 * This is the "rolling buffer" of recent activity
 */
public class BehavioralBuffer {
    
    private final UUID playerId;
    
    // Rolling buffers (last 60 seconds by default)
    @Getter
    private final Deque<RotationData> rotations;
    
    @Getter
    private final Deque<ClickData> clicks;
    
    @Getter
    private final Deque<MovementData> movements;
    
    // Last known values for delta calculations
    private RotationData lastRotation;
    private ClickData lastClick;
    private MovementData lastMovement;
    
    // Buffer size limits
    private static final int MAX_ROTATION_BUFFER = 1000;
    private static final int MAX_CLICK_BUFFER = 500;
    private static final int MAX_MOVEMENT_BUFFER = 1000;
    
    // Time-based cleanup (60 seconds)
    private static final long MAX_AGE_NS = TimeUnit.SECONDS.toNanos(60);
    
    public BehavioralBuffer(UUID playerId) {
        this.playerId = playerId;
        this.rotations = new ConcurrentLinkedDeque<>();
        this.clicks = new ConcurrentLinkedDeque<>();
        this.movements = new ConcurrentLinkedDeque<>();
    }
    
    /**
     * Add a rotation sample
     */
    public void addRotation(float yaw, float pitch) {
        long timestamp = System.nanoTime();
        
        // Calculate deltas
        float yawDelta = 0f;
        float pitchDelta = 0f;
        long tickSpacing = 0;
        
        if (lastRotation != null) {
            yawDelta = yaw - lastRotation.getYaw();
            pitchDelta = pitch - lastRotation.getPitch();
            tickSpacing = timestamp - lastRotation.getTimestamp();
            
            // Normalize yaw delta (-180 to 180)
            while (yawDelta > 180) yawDelta -= 360;
            while (yawDelta < -180) yawDelta += 360;
        }
        
        RotationData data = new RotationData(timestamp, yaw, pitch, yawDelta, pitchDelta, tickSpacing);
        rotations.addLast(data);
        lastRotation = data;
        
        // Trim buffer
        trimBuffer(rotations, MAX_ROTATION_BUFFER);
    }
    
    /**
     * Add a click sample
     */
    public void addClick(int targetEntityId, double distance, float yawAlignment) {
        long timestamp = System.nanoTime();
        
        long timeSinceLastClick = 0;
        if (lastClick != null) {
            timeSinceLastClick = TimeUnit.NANOSECONDS.toMillis(timestamp - lastClick.getTimestamp());
        }
        
        ClickData data = new ClickData(timestamp, targetEntityId, distance, yawAlignment, timeSinceLastClick);
        clicks.addLast(data);
        lastClick = data;
        
        // Trim buffer
        trimBuffer(clicks, MAX_CLICK_BUFFER);
    }
    
    /**
     * Add a movement sample
     */
    public void addMovement(org.bukkit.util.Vector position, org.bukkit.util.Vector velocity,
                           boolean onGround, boolean sprinting, boolean sneaking) {
        long timestamp = System.nanoTime();
        
        MovementData data = new MovementData(timestamp, position, velocity, onGround, sprinting, sneaking);
        movements.addLast(data);
        lastMovement = data;
        
        // Trim buffer
        trimBuffer(movements, MAX_MOVEMENT_BUFFER);
    }
    
    /**
     * Get rotation data within time window (milliseconds)
     */
    public List<RotationData> getRecentRotations(long windowMs) {
        long cutoffTime = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(windowMs);
        List<RotationData> result = new ArrayList<>();
        
        for (RotationData data : rotations) {
            if (data.getTimestamp() >= cutoffTime) {
                result.add(data);
            }
        }
        
        return result;
    }
    
    /**
     * Get click data within time window (milliseconds)
     */
    public List<ClickData> getRecentClicks(long windowMs) {
        long cutoffTime = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(windowMs);
        List<ClickData> result = new ArrayList<>();
        
        for (ClickData data : clicks) {
            if (data.getTimestamp() >= cutoffTime) {
                result.add(data);
            }
        }
        
        return result;
    }
    
    /**
     * Get movement data within time window (milliseconds)
     */
    public List<MovementData> getRecentMovements(long windowMs) {
        long cutoffTime = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(windowMs);
        List<MovementData> result = new ArrayList<>();
        
        for (MovementData data : movements) {
            if (data.getTimestamp() >= cutoffTime) {
                result.add(data);
            }
        }
        
        return result;
    }
    
    /**
     * Trim buffer to maximum size and age
     */
    private <T> void trimBuffer(Deque<T> buffer, int maxSize) {
        // Trim by size
        while (buffer.size() > maxSize) {
            buffer.removeFirst();
        }
        
        // Trim by age
        long cutoffTime = System.nanoTime() - MAX_AGE_NS;
        buffer.removeIf(item -> {
            if (item instanceof RotationData) {
                return ((RotationData) item).getTimestamp() < cutoffTime;
            } else if (item instanceof ClickData) {
                return ((ClickData) item).getTimestamp() < cutoffTime;
            } else if (item instanceof MovementData) {
                return ((MovementData) item).getTimestamp() < cutoffTime;
            }
            return false;
        });
    }
    
    /**
     * Clear all buffers
     */
    public void clear() {
        rotations.clear();
        clicks.clear();
        movements.clear();
        lastRotation = null;
        lastClick = null;
        lastMovement = null;
    }
    
    /**
     * Get sample count for analysis readiness
     */
    public boolean hasEnoughSamples(int minRotations, int minClicks) {
        return rotations.size() >= minRotations && clicks.size() >= minClicks;
    }
}
