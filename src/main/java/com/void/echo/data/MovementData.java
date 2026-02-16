package com.void.echo.data;

import lombok.Data;
import org.bukkit.util.Vector;

/**
 * Represents a single movement packet capture
 */
@Data
public class MovementData {
    
    private final long timestamp;          // Nanosecond precision timestamp
    private final Vector position;         // Player position
    private final Vector velocity;         // Player velocity
    private final boolean onGround;        // On ground flag
    private final boolean sprinting;       // Sprinting flag
    private final boolean sneaking;        // Sneaking flag
    private final double speed;            // Movement speed
    
    public MovementData(long timestamp, Vector position, Vector velocity, 
                       boolean onGround, boolean sprinting, boolean sneaking) {
        this.timestamp = timestamp;
        this.position = position;
        this.velocity = velocity;
        this.onGround = onGround;
        this.sprinting = sprinting;
        this.sneaking = sneaking;
        this.speed = velocity.length();
    }
    
    /**
     * Calculate acceleration relative to previous movement
     */
    public double getAcceleration(MovementData previous) {
        if (previous == null) return 0.0;
        
        long timeDiff = this.timestamp - previous.timestamp;
        if (timeDiff == 0) return 0.0;
        
        double speedDiff = this.speed - previous.speed;
        return speedDiff / (timeDiff / 1_000_000_000.0); // Convert ns to seconds
    }
}
