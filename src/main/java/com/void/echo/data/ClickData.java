package com.void.echo.data;

import lombok.Data;

/**
 * Represents a single click/attack event
 */
@Data
public class ClickData {
    
    private final long timestamp;          // Nanosecond precision timestamp
    private final int entityId;            // Target entity ID (-1 if no target)
    private final double distance;         // Distance to target
    private final float yawAlignment;      // How aligned crosshair was to target
    private final long timeSinceLastClick; // Time since previous click (ms)
    
    public ClickData(long timestamp, int entityId, double distance, float yawAlignment, long timeSinceLastClick) {
        this.timestamp = timestamp;
        this.entityId = entityId;
        this.distance = distance;
        this.yawAlignment = yawAlignment;
        this.timeSinceLastClick = timeSinceLastClick;
    }
    
    /**
     * Calculate clicks per second (CPS)
     */
    public double getCPS() {
        if (timeSinceLastClick == 0) return 0.0;
        return 1000.0 / timeSinceLastClick;
    }
    
    /**
     * Check if this was a hit on target
     */
    public boolean wasHit() {
        return entityId != -1;
    }
}
