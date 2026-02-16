package com.void.echo.profile;

import com.void.echo.EchoPlugin;
import com.void.echo.analysis.EntropyAnalysisResult;
import com.void.echo.analysis.EntropyAnalyzer;
import com.void.echo.data.BehavioralBuffer;
import com.void.echo.data.PlayerDataManager;
import com.void.echo.data.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Layer 3: Personal Baseline Profiling
 * 
 * Compares players to THEMSELVES rather than global averages.
 * 
 * Key concept: If a player's behavior suddenly changes dramatically,
 * that suggests toggling even if the behavior stays within "normal" thresholds.
 * 
 * For example:
 * - Average reaction time is 210ms ± 40ms
 * - Suddenly drops to 80ms ± 5ms for 10 minutes
 * - That deviation is suspicious regardless of absolute values
 */
public class ProfileManager {
    
    private final EchoPlugin plugin;
    private final PlayerDataManager dataManager;
    private final EntropyAnalyzer entropyAnalyzer;
    
    // Configuration
    private final long minPlaytimeMs;
    private final double reactionTimeDeviationThreshold;
    private final double rotationNoiseDeviationThreshold;
    private final double clickPatternDeviationThreshold;
    
    // Background task
    private BukkitTask updateTask;
    
    public ProfileManager(EchoPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.entropyAnalyzer = plugin.getEntropyAnalyzer();
        
        // Load configuration
        int minPlaytimeMins = plugin.getConfigManager().getInt("profiling.min-playtime", 30);
        this.minPlaytimeMs = minPlaytimeMins * 60 * 1000L;
        
        this.reactionTimeDeviationThreshold = plugin.getConfigManager().getDouble("profiling.deviation-thresholds.reaction-time", 2.5);
        this.rotationNoiseDeviationThreshold = plugin.getConfigManager().getDouble("profiling.deviation-thresholds.rotation-noise", 2.0);
        this.clickPatternDeviationThreshold = plugin.getConfigManager().getDouble("profiling.deviation-thresholds.click-pattern", 2.5);
        
        // Start background update task (every 30 seconds)
        startUpdateTask();
    }
    
    /**
     * Start background profile update task
     */
    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("echo.bypass")) continue;
                
                updatePlayerProfile(player);
            }
        }, 600L, 600L); // Every 30 seconds (600 ticks)
    }
    
    /**
     * Update a player's profile with current behavioral data
     */
    public void updatePlayerProfile(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Get player profile and buffer
        PlayerProfile profile = dataManager.getProfile(player);
        BehavioralBuffer buffer = dataManager.getBuffer(playerId);
        
        // Update playtime
        profile.setLastSeen(System.currentTimeMillis());
        
        // Check if we have enough data to analyze
        if (!buffer.hasEnoughSamples(50, 25)) {
            return; // Not enough samples yet
        }
        
        // Update baseline statistics
        profile.updateBaseline(buffer);
        
        // Run entropy analysis
        EntropyAnalysisResult analysis = entropyAnalyzer.analyze(playerId);
        if (analysis == null) {
            return;
        }
        
        // Update integrity score
        double newScore = calculateIntegrityScore(profile, analysis, buffer);
        double oldScore = profile.getCurrentIntegrityScore();
        profile.updateIntegrityScore(newScore);
        
        // Check for baseline deviation (toggling detection)
        if (profile.isBaselineEstablished()) {
            checkForDeviations(profile, analysis, buffer);
        } else if (profile.canEstablishBaseline()) {
            profile.establishBaseline();
            plugin.getLogger().info("Established baseline for " + player.getName() + 
                    " (Playtime: " + (profile.getTotalPlaytime() / 60000) + " mins)");
        }
        
        // Log significant score changes
        if (Math.abs(newScore - oldScore) > 10) {
            plugin.getLogger().warning(String.format(
                    "%s integrity score changed: %.1f -> %.1f (%s)",
                    player.getName(),
                    oldScore,
                    newScore,
                    analysis.getDescription()
            ));
        }
    }
    
    /**
     * Calculate integrity score from analysis and profile
     */
    private double calculateIntegrityScore(PlayerProfile profile, EntropyAnalysisResult analysis, BehavioralBuffer buffer) {
        // Start with entropy analysis score
        double score = analysis.getHumanProbabilityScore();
        
        // If baseline is established, apply deviation penalties
        if (profile.isBaselineEstablished()) {
            // Check for behavioral shifts
            double rotationShift = checkRotationShift(profile, buffer);
            double clickShift = checkClickShift(profile, buffer);
            
            // Apply penalties for deviations
            if (rotationShift > rotationNoiseDeviationThreshold) {
                double penalty = (rotationShift - rotationNoiseDeviationThreshold) * 10.0;
                score -= Math.min(penalty, 30.0); // Max 30 point penalty
            }
            
            if (clickShift > clickPatternDeviationThreshold) {
                double penalty = (clickShift - clickPatternDeviationThreshold) * 10.0;
                score -= Math.min(penalty, 30.0); // Max 30 point penalty
            }
        }
        
        // Clamp to 0-100
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Check for rotation pattern shifts from baseline
     * Returns deviation in standard deviations
     */
    private double checkRotationShift(PlayerProfile profile, BehavioralBuffer buffer) {
        var rotations = buffer.getRecentRotations(30000);
        if (rotations.isEmpty()) return 0.0;
        
        // Calculate current rotation noise
        double[] yawDeltas = rotations.stream()
                .filter(r -> r.isSignificant())
                .mapToDouble(r -> Math.abs(r.getYawDelta()))
                .toArray();
        
        if (yawDeltas.length < 10) return 0.0;
        
        double currentStdDev = com.void.echo.util.MathUtil.standardDeviation(yawDeltas);
        
        // Compare to baseline
        return profile.calculateDeviation(
                currentStdDev,
                profile.getStdDevYawDelta(),
                profile.getStdDevYawDelta() * 0.5 // Use half of std dev as the variance metric
        );
    }
    
    /**
     * Check for click pattern shifts from baseline
     * Returns deviation in standard deviations
     */
    private double checkClickShift(PlayerProfile profile, BehavioralBuffer buffer) {
        var clicks = buffer.getRecentClicks(30000);
        if (clicks.size() < 10) return 0.0;
        
        // Calculate current click pattern
        double[] intervals = clicks.stream()
                .mapToDouble(c -> c.getTimeSinceLastClick())
                .filter(x -> x > 0)
                .toArray();
        
        if (intervals.length < 5) return 0.0;
        
        double currentStdDev = com.void.echo.util.MathUtil.standardDeviation(intervals);
        
        // Compare to baseline
        return profile.calculateDeviation(
                currentStdDev,
                profile.getStdDevClickInterval(),
                profile.getStdDevClickInterval() * 0.5
        );
    }
    
    /**
     * Check for suspicious deviations from baseline
     */
    private void checkForDeviations(PlayerProfile profile, EntropyAnalysisResult analysis, BehavioralBuffer buffer) {
        double rotationShift = checkRotationShift(profile, buffer);
        double clickShift = checkClickShift(profile, buffer);
        
        // Log significant deviations
        if (rotationShift > rotationNoiseDeviationThreshold) {
            String desc = String.format(
                    "Rotation pattern shifted %.1f standard deviations from baseline",
                    rotationShift
            );
            profile.addViolation("ROTATION_SHIFT", rotationShift, desc);
            
            plugin.getLogger().warning(profile.getPlayerName() + ": " + desc);
        }
        
        if (clickShift > clickPatternDeviationThreshold) {
            String desc = String.format(
                    "Click pattern shifted %.1f standard deviations from baseline",
                    clickShift
            );
            profile.addViolation("CLICK_SHIFT", clickShift, desc);
            
            plugin.getLogger().warning(profile.getPlayerName() + ": " + desc);
        }
        
        // Check for extremely suspicious scores
        if (analysis.isExtremelySuspicious()) {
            profile.addViolation(
                    "EXTREMELY_SUSPICIOUS",
                    100 - analysis.getHumanProbabilityScore(),
                    "Human probability score: " + String.format("%.1f", analysis.getHumanProbabilityScore())
            );
            
            // Notify staff
            notifyStaff(profile, analysis);
        }
    }
    
    /**
     * Notify staff of suspicious behavior
     */
    private void notifyStaff(PlayerProfile profile, EntropyAnalysisResult analysis) {
        String message = String.format(
                "§c[ECHO] §e%s §cis extremely suspicious! Score: §4%.1f/100 §c(%s)",
                profile.getPlayerName(),
                analysis.getHumanProbabilityScore(),
                analysis.getDescription()
        );
        
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("echo.alerts")) {
                staff.sendMessage(message);
            }
        }
    }
    
    /**
     * Shutdown profile manager
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }
}
