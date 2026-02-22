package com.void.echo.listener;

import com.void.echo.EchoPlugin;
import com.void.echo.data.PlayerDataManager;
import com.void.echo.data.PlayerProfile;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bukkit event listener that connects all 10 ECHO layers to the game.
 *
 * Without this class, every manager is initialised but nothing triggers
 * analysis – packets are captured, but combat events, join/quit, and
 * spectator detection are never wired up.
 */
public class EchoEventListener implements Listener {

    private final EchoPlugin plugin;
    private final PlayerDataManager dataManager;

    // attackerId -> victimId -> running hit count (for PostFightReviewer intensity threshold)
    private final Map<UUID, Map<UUID, AtomicInteger>> combatHits = new ConcurrentHashMap<>();

    // attackerId -> victimId -> last-hit timestamp in millis (for time-windowed KillAura)
    private final Map<UUID, Map<UUID, Long>> recentHitTimes = new ConcurrentHashMap<>();

    public EchoEventListener(EchoPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }

    // ── Join / Quit ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Eagerly create buffer and profile so checks are ready from the first packet
        dataManager.getBuffer(player.getUniqueId());
        dataManager.getProfile(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dataManager.removePlayer(player.getUniqueId());
        combatHits.remove(player.getUniqueId());
        recentHitTimes.remove(player.getUniqueId());
    }

    // ── Combat ───────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (attacker.hasPermission("echo.bypass")) return;

        Entity target = event.getEntity();
        UUID attackerId = attacker.getUniqueId();
        UUID targetId = target.getUniqueId();

        // ── Reach check ─────────────────────────────────────────────────────
        if (plugin.getConfigManager().getBoolean("checks.reach.enabled", true)) {
            double distance = attacker.getEyeLocation()
                    .distance(target.getLocation().add(0, target.getHeight() / 2.0, 0));

            double maxReach = getMaxReach(attacker);

            if (distance > maxReach) {
                PlayerProfile profile = dataManager.getProfile(attacker);
                profile.addViolation("REACH", distance - maxReach,
                        String.format("Reach: %.2f blocks (max %.2f)", distance, maxReach));

                plugin.notifyStaff(String.format(
                        "§c[ECHO] §e%s §7reach violation: §f%.2f §7blocks (max §f%.2f§7)",
                        attacker.getName(), distance, maxReach));

                if (plugin.getConfigManager().getBoolean("checks.reach.cancel-on-violation", false)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // ── KillAura: multi-target detection ─────────────────────────────────
        if (plugin.getConfigManager().getBoolean("checks.killaura.enabled", true)
                && target instanceof Player) {
            checkKillAura(attacker, targetId);
            // Record hit time AFTER the check so the window is measured correctly
            recentHitTimes
                    .computeIfAbsent(attackerId, k -> new ConcurrentHashMap<>())
                    .put(targetId, System.currentTimeMillis());
        }

        // ── Post-fight recording ─────────────────────────────────────────────
        if (plugin.getFightReviewer() != null && target instanceof Player) {
            int hits = combatHits
                    .computeIfAbsent(attackerId, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(targetId, k -> new AtomicInteger(0))
                    .incrementAndGet();

            int minIntensity = plugin.getConfigManager().getInt("neural-review.min-intensity", 10);
            if (hits == minIntensity) {
                // Record once the intensity threshold is first reached
                plugin.getFightReviewer().recordFight(attackerId, targetId, hits);
            }
        }

        // ── Identity fingerprint (async) ─────────────────────────────────────
        if (plugin.getIdentityAnchor() != null) {
            plugin.getExecutorService().submit(
                    () -> plugin.getIdentityAnchor().updateFingerprint(attackerId));
        }

        // ── Camouflage tracking (async) ───────────────────────────────────────
        if (plugin.getCamouflageDetector() != null && plugin.getEntropyAnalyzer() != null) {
            plugin.getExecutorService().submit(() -> {
                var result = plugin.getEntropyAnalyzer().analyze(attackerId);
                if (result == null) return;

                boolean spectated = isBeingSpectated(attacker);
                double score = result.getHumanProbabilityScore();

                if (spectated) {
                    plugin.getCamouflageDetector().recordSpectatedBehavior(attackerId, score);
                } else {
                    plugin.getCamouflageDetector().recordNormalBehavior(attackerId, score);
                }

                double adaptation = plugin.getCamouflageDetector().checkCamouflage(attackerId);
                double threshold = plugin.getConfigManager().getDouble(
                        "camouflage-detection.adaptation-threshold", 0.3);

                if (adaptation > threshold) {
                    PlayerProfile profile = dataManager.getProfile(attacker);
                    profile.addViolation("CAMOUFLAGE", adaptation * 100,
                            String.format("Behavior adaptation: %.1f%% change when spectated",
                                    adaptation * 100));

                    plugin.notifyStaff(String.format(
                            "§c[ECHO] §e%s §7camouflage detected: §f%.1f%% §7behavior shift when spectated",
                            attacker.getName(), adaptation * 100));
                }
            });
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Calculate the maximum allowable reach for this player in blocks,
     * applying ping compensation and integrity pressure reduction.
     */
    private double getMaxReach(Player player) {
        double base = switch (player.getGameMode()) {
            case CREATIVE -> plugin.getConfigManager().getDouble("checks.reach.creative-reach", 5.0);
            case SPECTATOR -> Double.MAX_VALUE;
            default -> plugin.getConfigManager().getDouble("checks.reach.survival-reach", 3.0);
        };

        // Ping compensation: allow up to configured max extra reach for latency
        int ping = player.getPing();
        double maxComp = plugin.getConfigManager().getDouble("checks.reach.max-ping-compensation", 1.0);
        double comp = Math.min(maxComp, ping / 1000.0 * 3.0); // ~0.3 blocks per 100 ms

        // Integrity pressure silently reduces effective reach for suspicious players
        if (plugin.getPressureManager() != null) {
            base -= base * plugin.getPressureManager().getReachReduction(player.getUniqueId());
        }

        return base + comp;
    }

    /**
     * Detect KillAura by checking how many distinct players the attacker has hit
     * within a short time window (excluding the current target).
     */
    private void checkKillAura(Player attacker, UUID newTargetId) {
        long windowMs = plugin.getConfigManager().getLong("checks.killaura.multi-target-window-ms", 150);
        int maxTargets = plugin.getConfigManager().getInt("checks.killaura.max-targets-per-window", 1);

        Map<UUID, Long> hitTimes = recentHitTimes.get(attacker.getUniqueId());
        if (hitTimes == null) return; // No prior hits recorded yet

        long now = System.currentTimeMillis();
        long distinctOtherTargets = hitTimes.entrySet().stream()
                .filter(e -> !e.getKey().equals(newTargetId))            // exclude current target
                .filter(e -> now - e.getValue() <= windowMs)             // within time window
                .count();

        if (distinctOtherTargets >= maxTargets) {
            PlayerProfile profile = dataManager.getProfile(attacker);
            profile.addViolation("KILLAURA", distinctOtherTargets,
                    String.format("Multi-target: hit %d distinct other players within %d ms",
                            distinctOtherTargets, windowMs));

            plugin.notifyStaff(String.format(
                    "§c[ECHO] §e%s §7possible killaura: §f%d §7other targets in §f%d§7ms window",
                    attacker.getName(), distinctOtherTargets, windowMs));
        }
    }

    /**
     * Check whether any online staff member is directly spectating this player.
     */
    private boolean isBeingSpectated(Player player) {
        for (Player other : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (!other.hasPermission("echo.admin")) continue;
            if (other.getGameMode() != GameMode.SPECTATOR) continue;
            Entity spectatorTarget = other.getSpectatorTarget();
            if (spectatorTarget != null
                    && spectatorTarget.getUniqueId().equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }
}
