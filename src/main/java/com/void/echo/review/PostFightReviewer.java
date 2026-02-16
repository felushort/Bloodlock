package com.void.echo.review;

import com.void.echo.EchoPlugin;
import com.void.echo.data.BehavioralBuffer;
import com.void.echo.data.PlayerDataManager;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Layer 6: Post-Fight Neural Review
 * 
 * Records intense fights and analyzes them offline with full context.
 * 
 * Real-time analysis is limited by performance constraints.
 * Post-fight analysis can:
 * - Replay fight data deterministically
 * - Run deeper statistical checks
 * - Analyze aim curvature
 * - Perform spectral analysis
 * - Check latency-adjusted reach
 * 
 * Only escalates when multiple fights show persistent non-human patterns.
 */
public class PostFightReviewer {
    
    private final EchoPlugin plugin;
    private final PlayerDataManager dataManager;
    
    // Queue of fights to review
    private final Queue<FightRecord> reviewQueue;
    
    // Configuration
    private final int minIntensity;
    private final int maxQueueSize;
    private final long processIntervalSeconds;
    
    // Processing task
    private BukkitTask processingTask;
    
    public PostFightReviewer(EchoPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.reviewQueue = new ConcurrentLinkedQueue<>();
        
        // Load configuration
        this.minIntensity = plugin.getConfigManager().getInt("neural-review.min-intensity", 10);
        this.maxQueueSize = plugin.getConfigManager().getInt("neural-review.queue.max-size", 1000);
        this.processIntervalSeconds = plugin.getConfigManager().getLong("neural-review.queue.process-interval", 30);
    }
    
    /**
     * Start review processing task
     */
    public void start() {
        long intervalTicks = processIntervalSeconds * 20; // Convert to ticks
        
        processingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            processQueue();
        }, intervalTicks, intervalTicks);
        
        plugin.getLogger().info("Post-fight reviewer started (interval: " + processIntervalSeconds + "s)");
    }
    
    /**
     * Record a fight for later review
     */
    public void recordFight(UUID attackerId, UUID victimId, int hitCount) {
        // Check intensity threshold
        if (hitCount < minIntensity) {
            return;
        }
        
        // Check queue size
        if (reviewQueue.size() >= maxQueueSize) {
            plugin.getLogger().warning("Fight review queue is full! Dropping fight record.");
            return;
        }
        
        // Create fight record
        FightRecord record = new FightRecord(
                attackerId,
                victimId,
                System.currentTimeMillis(),
                hitCount
        );
        
        reviewQueue.offer(record);
    }
    
    /**
     * Process review queue
     */
    private void processQueue() {
        int processed = 0;
        int suspicious = 0;
        
        // Process up to 10 fights per cycle
        for (int i = 0; i < 10 && !reviewQueue.isEmpty(); i++) {
            FightRecord record = reviewQueue.poll();
            if (record == null) break;
            
            boolean isSuspicious = analyzeFight(record);
            processed++;
            
            if (isSuspicious) {
                suspicious++;
            }
        }
        
        if (processed > 0) {
            plugin.getLogger().fine(String.format(
                    "Processed %d fights (%d suspicious). Queue size: %d",
                    processed,
                    suspicious,
                    reviewQueue.size()
            ));
        }
    }
    
    /**
     * Analyze a fight record
     * Returns true if suspicious patterns detected
     */
    private boolean analyzeFight(FightRecord record) {
        // Get behavioral data for attacker
        BehavioralBuffer buffer = dataManager.getBuffer(record.getAttackerId());
        
        // TODO: Implement deep analysis
        // - Aim curvature fitting
        // - Jitter frequency spectrum
        // - Click burst clustering
        // - Latency-adjusted reach reconstruction
        
        // For now, just a placeholder
        var rotations = buffer.getRecentRotations(30000);
        var clicks = buffer.getRecentClicks(30000);
        
        // Simple heuristic: check if rotation count matches click count suspiciously well
        double ratio = rotations.isEmpty() ? 0.0 : (double) clicks.size() / rotations.size();
        
        // If ratio is suspiciously perfect, flag it
        return ratio > 0.9 && ratio < 1.1;
    }
    
    /**
     * Shutdown reviewer
     */
    public void shutdown() {
        if (processingTask != null) {
            processingTask.cancel();
        }
        
        plugin.getLogger().info("Post-fight reviewer stopped. " + reviewQueue.size() + " fights remain in queue.");
    }
    
    /**
     * Fight record for review
     */
    @Data
    public static class FightRecord {
        private final UUID attackerId;
        private final UUID victimId;
        private final long timestamp;
        private final int hitCount;
    }
}
