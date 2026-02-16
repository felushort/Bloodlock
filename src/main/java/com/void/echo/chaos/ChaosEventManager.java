package com.void.echo.chaos;

import com.void.echo.EchoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Layer 10: Controlled Chaos Events
 * 
 * Periodically runs "integrity stress tests" during peak fights.
 * 
 * Slightly adjusts combat tolerances in ways that:
 * - Legit players adapt to unconsciously
 * - Rigid automation fails
 * 
 * Measures adaptability curves.
 * 
 * Example: Temporarily adjust hit validation timing by 5ms
 * - Human players won't notice
 * - Automation tuned for exact timing will fail
 */
public class ChaosEventManager {
    
    private final EchoPlugin plugin;
    private final Random random;
    
    // Configuration
    private final boolean enabled;
    private final long frequencyMinutes;
    private final long durationSeconds;
    private final double intensity;
    
    // State
    private boolean eventActive = false;
    private ChaosEvent currentEvent = null;
    
    // Tasks
    private BukkitTask eventTriggerTask;
    private BukkitTask eventEndTask;
    
    public ChaosEventManager(EchoPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        
        // Load configuration
        this.enabled = plugin.getConfigManager().getBoolean("chaos-events.enabled", true);
        this.frequencyMinutes = plugin.getConfigManager().getLong("chaos-events.frequency", 20);
        this.durationSeconds = plugin.getConfigManager().getLong("chaos-events.duration", 30);
        this.intensity = plugin.getConfigManager().getDouble("chaos-events.intensity", 0.05);
    }
    
    /**
     * Start chaos event system
     */
    public void start() {
        if (!enabled) {
            return;
        }
        
        long frequencyTicks = frequencyMinutes * 60 * 20; // Convert to ticks
        
        eventTriggerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            triggerChaosEvent();
        }, frequencyTicks, frequencyTicks);
        
        plugin.getLogger().info("Chaos event system started (frequency: " + frequencyMinutes + " mins)");
    }
    
    /**
     * Trigger a chaos event
     */
    private void triggerChaosEvent() {
        if (eventActive) {
            return; // Event already running
        }
        
        // Check if enough players are online
        if (Bukkit.getOnlinePlayers().size() < 3) {
            return; // Not enough players
        }
        
        // Pick random event type
        ChaosEvent event = pickRandomEvent();
        currentEvent = event;
        eventActive = true;
        
        plugin.getLogger().fine("Chaos event started: " + event.getName());
        
        // Schedule event end
        long durationTicks = durationSeconds * 20;
        eventEndTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endChaosEvent();
        }, durationTicks);
    }
    
    /**
     * End current chaos event
     */
    private void endChaosEvent() {
        if (!eventActive) {
            return;
        }
        
        plugin.getLogger().fine("Chaos event ended: " + currentEvent.getName());
        
        eventActive = false;
        currentEvent = null;
        
        if (eventEndTask != null) {
            eventEndTask.cancel();
            eventEndTask = null;
        }
    }
    
    /**
     * Pick random chaos event
     */
    private ChaosEvent pickRandomEvent() {
        List<ChaosEvent> events = Arrays.asList(
                new ChaosEvent("Hit Validation Shift", intensity),
                new ChaosEvent("Knockback Variance", intensity),
                new ChaosEvent("Movement Friction Adjust", intensity),
                new ChaosEvent("Attack Cooldown Drift", intensity)
        );
        
        return events.get(random.nextInt(events.size()));
    }
    
    /**
     * Check if chaos event is active
     */
    public boolean isEventActive() {
        return eventActive;
    }
    
    /**
     * Get current event
     */
    public ChaosEvent getCurrentEvent() {
        return currentEvent;
    }
    
    /**
     * Shutdown chaos manager
     */
    public void shutdown() {
        if (eventTriggerTask != null) {
            eventTriggerTask.cancel();
        }
        if (eventEndTask != null) {
            eventEndTask.cancel();
        }
    }
    
    /**
     * Chaos event definition
     */
    public static class ChaosEvent {
        private final String name;
        private final double intensity;
        
        public ChaosEvent(String name, double intensity) {
            this.name = name;
            this.intensity = intensity;
        }
        
        public String getName() {
            return name;
        }
        
        public double getIntensity() {
            return intensity;
        }
    }
}
