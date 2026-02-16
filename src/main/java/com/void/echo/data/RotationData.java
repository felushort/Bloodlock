package com.void.echo.data;

import lombok.Data;

/**
 * Represents a single rotation packet capture
 */
@Data
public class RotationData {
    
    private final long timestamp;      // Nanosecond precision timestamp
    private final float yaw;            // Yaw rotation
    private final float pitch;          // Pitch rotation
    private final float yawDelta;       // Change from previous yaw
    private final float pitchDelta;     // Change from previous pitch
    private final long tickSpacing;     // Ticks since last rotation
    
    public RotationData(long timestamp, float yaw, float pitch, float yawDelta, float pitchDelta, long tickSpacing) {
        this.timestamp = timestamp;
        this.yaw = yaw;
        this.pitch = pitch;
        this.yawDelta = yawDelta;
        this.pitchDelta = pitchDelta;
        this.tickSpacing = tickSpacing;
    }
    
    /**
     * Get the magnitude of rotation change
     */
    public double getRotationMagnitude() {
        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }
    
    /**
     * Check if this is a significant rotation
     */
    public boolean isSignificant() {
        return Math.abs(yawDelta) > 0.1 || Math.abs(pitchDelta) > 0.1;
    }
}
