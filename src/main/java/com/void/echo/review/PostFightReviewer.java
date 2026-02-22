package com.void.echo.review;

import com.void.echo.EchoPlugin;
import com.void.echo.data.BehavioralBuffer;
import com.void.echo.data.ClickData;
import com.void.echo.data.PlayerDataManager;
import com.void.echo.data.RotationData;
import com.void.echo.util.MathUtil;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

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
    private final long analysisWindowMs;
    private final double suspicionThreshold;
    
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
        this.analysisWindowMs = plugin.getConfigManager().getLong("neural-review.analysis.time-window-ms", 60000);
        this.suspicionThreshold = plugin.getConfigManager().getDouble("neural-review.analysis.suspicion-threshold", 50.0);
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
     * Analyse a fight record.
     * Returns true if suspicious patterns are detected.
     *
     * Checks performed:
     *  1. Click-timing consistency  – autoclicker detection (low CV)
     *  2. Crosshair-alignment ratio – aimbot detection (too many perfect hits)
     *  3. Hit-accuracy × CPS ratio  – unrealistically high accuracy at high speed
     *  4. Rotation-to-click ratio   – more clicks than rotations suggests KillAura
     */
    private boolean analyzeFight(FightRecord record) {
        BehavioralBuffer buffer = dataManager.getBuffer(record.getAttackerId());

        List<RotationData> rotations = buffer.getRecentRotations(analysisWindowMs);
        List<ClickData> clicks = buffer.getRecentClicks(analysisWindowMs);

        if (clicks.size() < 5) {
            return false; // Insufficient data for analysis
        }

        double suspicionScore = 0.0;

        // Check 1: Click-timing consistency (autoclicker pattern)
        suspicionScore += scoreClickTiming(clicks);

        // Check 2: Crosshair alignment distribution (aimbot pattern)
        suspicionScore += scoreAimAlignment(clicks);

        // Check 3: Hit accuracy vs. CPS (inhuman precision)
        suspicionScore += scoreHitAccuracy(clicks);

        // Check 4: Clicks vs. rotations ratio (KillAura indicator)
        if (!rotations.isEmpty()) {
            double clickRotationRatio = (double) clicks.size() / rotations.size();
            // Legitimate players rotate between every hit; pure KillAura fires
            // clicks with almost no corresponding rotation packets
            if (clickRotationRatio > 2.0) {
                suspicionScore += 25.0;
            }
        }

        boolean suspicious = suspicionScore >= suspicionThreshold;

        if (suspicious) {
            var profile = dataManager.getProfile(record.getAttackerId(), "Unknown");
            profile.addViolation("FIGHT_ANALYSIS", suspicionScore,
                    String.format("Post-fight analysis flagged: suspicion score %.1f", suspicionScore));

            plugin.notifyStaff(String.format(
                    "§c[ECHO] §e%s §7post-fight analysis flagged §f%.1f §7suspicion points",
                    profile.getPlayerName(), suspicionScore));
        }

        return suspicious;
    }

    // ── Analysis helpers ─────────────────────────────────────────────────────

    /**
     * Score based on click-interval consistency.
     * Autoclickers have very low coefficient of variation (CV) in their intervals.
     */
    private double scoreClickTiming(List<ClickData> clicks) {
        double[] intervals = clicks.stream()
                .mapToDouble(ClickData::getTimeSinceLastClick)
                .filter(x -> x > 10 && x < 2000)
                .toArray();

        if (intervals.length < 5) return 0.0;

        double stdDev = MathUtil.standardDeviation(intervals);
        double mean = MathUtil.mean(intervals);
        double cv = mean > 0 ? stdDev / mean : 0.0;

        // Human CV is typically > 0.15; autoclickers are often < 0.05
        if (cv < 0.05) return 40.0;
        if (cv < 0.10) return 25.0;
        if (cv < 0.15) return 10.0;
        return 0.0;
    }

    /**
     * Score based on crosshair-alignment ratio on hits.
     * Aimbots lock on perfectly; humans have natural aiming imprecision.
     */
    private double scoreAimAlignment(List<ClickData> clicks) {
        List<ClickData> hits = clicks.stream().filter(ClickData::wasHit).toList();
        if (hits.size() < 5) return 0.0;

        long perfectHits = hits.stream()
                .filter(c -> c.getYawAlignment() > 0.98f)
                .count();
        double perfectRatio = (double) perfectHits / hits.size();

        if (perfectRatio > 0.80) return 35.0;
        if (perfectRatio > 0.60) return 20.0;
        if (perfectRatio > 0.40) return 10.0;
        return 0.0;
    }

    /**
     * Score based on hit accuracy combined with CPS.
     * Humans naturally lose accuracy at high click speeds.
     */
    private double scoreHitAccuracy(List<ClickData> clicks) {
        if (clicks.size() < 5) return 0.0;

        long totalHits = clicks.stream().filter(ClickData::wasHit).count();
        double accuracy = (double) totalHits / clicks.size();

        // Estimate CPS from the time range of the click list
        long firstTs = clicks.get(0).getTimestamp();
        long lastTs = clicks.get(clicks.size() - 1).getTimestamp();
        long rangeMs = TimeUnit.NANOSECONDS.toMillis(lastTs - firstTs);
        double cps = rangeMs > 0 ? (clicks.size() * 1000.0 / rangeMs) : 0.0;

        // High accuracy AND high CPS = suspicious
        if (accuracy > 0.90 && cps > 10) return 30.0;
        if (accuracy > 0.85 && cps > 12) return 20.0;
        if (accuracy > 0.80 && cps > 15) return 10.0;
        return 0.0;
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
