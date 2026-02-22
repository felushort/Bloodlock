package com.void.echo.command;

import com.void.echo.EchoPlugin;
import com.void.echo.analysis.EntropyAnalysisResult;
import com.void.echo.data.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command executor for /echo commands
 */
public class EchoCommandExecutor implements CommandExecutor, TabCompleter {
    
    private final EchoPlugin plugin;
    
    public EchoCommandExecutor(EchoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "status" -> handleStatus(sender);
            case "check" -> handleCheck(sender, args);
            case "profile" -> handleProfile(sender, args);
            case "reload" -> handleReload(sender);
            case "alerts" -> handleAlerts(sender, args);
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage("§c[ECHO] Unknown subcommand. Use /echo help");
        }
        
        return true;
    }
    
    /**
     * Handle /echo status
     */
    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("§a§lECHO §8- §7Behavioral Integrity Engine");
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("");
        sender.sendMessage("§7Version: §f1.0.0-ALPHA");
        sender.sendMessage("§7Online Players: §f" + Bukkit.getOnlinePlayers().size());
        sender.sendMessage("");
        sender.sendMessage("§7Data Statistics:");
        sender.sendMessage("§8  • §7" + plugin.getDataManager().getStats());
        sender.sendMessage("§8  • §7Packets Processed: §f" + 
                (plugin.getCaptureManager() != null ? plugin.getCaptureManager().getPacketsProcessed() : 0));
        sender.sendMessage("");
        
        // Layer status
        sender.sendMessage("§7Active Layers:");
        sender.sendMessage("§8  • §a✓ §7Layer 1: Input Capture");
        sender.sendMessage("§8  • §a✓ §7Layer 2: Entropy Analysis");
        sender.sendMessage("§8  • §a✓ §7Layer 3: Baseline Profiling");
        sender.sendMessage("§8  • §a✓ §7Layer 4: Physics Drift");
        sender.sendMessage("§8  • §a✓ §7Layer 5: Integrity Pressure");
        sender.sendMessage("§8  • §a✓ §7Layer 6: Post-Fight Review");
        sender.sendMessage("§8  • §a✓ §7Layer 7: Camouflage Detection");
        sender.sendMessage("§8  • §a✓ §7Layer 8: Identity Anchoring");
        sender.sendMessage("§8  • §a✓ §7Layer 9: Transparency Mode");
        sender.sendMessage("§8  • §a✓ §7Layer 10: Chaos Events");
        sender.sendMessage("§7§m                                           ");
    }
    
    /**
     * Handle /echo check <player>
     */
    private void handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.viewscore")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c[ECHO] Usage: /echo check <player>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[ECHO] Player not found.");
            return;
        }
        
        // Run analysis
        if (plugin.getEntropyAnalyzer() == null) {
            sender.sendMessage("§e[ECHO] Entropy analyzer is disabled.");
            return;
        }
        EntropyAnalysisResult analysis = plugin.getEntropyAnalyzer().analyze(target.getUniqueId());
        PlayerProfile profile = plugin.getDataManager().getProfile(target);
        
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("§a§lECHO §8- §7Integrity Check: §f" + target.getName());
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("");
        
        if (analysis != null) {
            sender.sendMessage("§7Human Probability: §f" + String.format("%.1f/100", analysis.getHumanProbabilityScore()));
            sender.sendMessage("§7Status: §f" + analysis.getDescription());
            sender.sendMessage("");
            sender.sendMessage("§7Component Scores:");
            sender.sendMessage("§8  • §7Yaw Entropy: §f" + String.format("%.1f", analysis.getYawEntropyScore()));
            sender.sendMessage("§8  • §7Pitch Entropy: §f" + String.format("%.1f", analysis.getPitchEntropyScore()));
            sender.sendMessage("§8  • §7Click Distribution: §f" + String.format("%.1f", analysis.getClickDistributionScore()));
            sender.sendMessage("§8  • §7Reaction Time: §f" + String.format("%.1f", analysis.getReactionTimeScore()));
            sender.sendMessage("§8  • §7Crosshair Alignment: §f" + String.format("%.1f", analysis.getCrosshairAlignmentScore()));
            sender.sendMessage("");
            sender.sendMessage("§7Samples: §f" + analysis.getRotationSamples() + " rotations, " + 
                    analysis.getClickSamples() + " clicks");
        } else {
            sender.sendMessage("§eInsufficient data for analysis.");
            sender.sendMessage("§7The player needs more activity to establish a pattern.");
        }
        
        sender.sendMessage("");
        sender.sendMessage("§7Current Integrity: §f" + String.format("%.1f/100", profile.getCurrentIntegrityScore()));
        sender.sendMessage("§7Baseline Established: §f" + (profile.isBaselineEstablished() ? "Yes" : "No"));
        sender.sendMessage("§7Flagged for Review: §f" + (profile.isFlaggedForReview() ? "§cYes" : "§aNo"));
        sender.sendMessage("§7§m                                           ");
    }
    
    /**
     * Handle /echo profile <player>
     */
    private void handleProfile(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c[ECHO] Usage: /echo profile <player>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[ECHO] Player not found.");
            return;
        }
        
        PlayerProfile profile = plugin.getDataManager().getProfile(target);
        
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("§a§lECHO §8- §7Player Profile: §f" + target.getName());
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("");
        sender.sendMessage("§7Session Info:");
        sender.sendMessage("§8  • §7Session Count: §f" + profile.getSessionCount());
        sender.sendMessage("§8  • §7Total Playtime: §f" + (profile.getTotalPlaytime() / 60000) + " minutes");
        sender.sendMessage("");
        sender.sendMessage("§7Baseline Statistics:");
        sender.sendMessage("§8  • §7Avg CPS: §f" + String.format("%.1f", profile.getAvgCPS()));
        sender.sendMessage("§8  • §7CPS StdDev: §f" + String.format("%.1f", profile.getStdDevCPS()));
        sender.sendMessage("§8  • §7Avg Reaction: §f" + String.format("%.0f ms", profile.getAvgReactionTime()));
        sender.sendMessage("§8  • §7Hit Accuracy: §f" + String.format("%.1f%%", profile.getHitAccuracy() * 100));
        sender.sendMessage("");
        sender.sendMessage("§7Integrity:");
        sender.sendMessage("§8  • §7Current Score: §f" + String.format("%.1f/100", profile.getCurrentIntegrityScore()));
        sender.sendMessage("§8  • §7Lowest Score: §f" + String.format("%.1f/100", profile.getLowestIntegrityScore()));
        sender.sendMessage("§8  • §7Violations: §f" + profile.getViolations().size());
        sender.sendMessage("§7§m                                           ");
    }
    
    /**
     * Handle /echo reload
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        sender.sendMessage("§a[ECHO] Reloading configuration...");
        plugin.reload();
        sender.sendMessage("§a[ECHO] Configuration reloaded successfully!");
    }
    
    /**
     * Handle /echo alerts [on|off]
     */
    private void handleAlerts(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.alerts")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[ECHO] This command can only be used by players.");
            return;
        }

        boolean enableAlerts;
        if (args.length >= 2) {
            enableAlerts = args[1].equalsIgnoreCase("on");
        } else {
            // Toggle: if currently opted out, enable; if currently receiving, disable
            enableAlerts = !plugin.getAlertOptOut().contains(player.getUniqueId());
        }

        if (enableAlerts) {
            plugin.getAlertOptOut().remove(player.getUniqueId());
            sender.sendMessage("§a[ECHO] Alerts §2enabled§a. You will receive ECHO notifications.");
        } else {
            plugin.getAlertOptOut().add(player.getUniqueId());
            sender.sendMessage("§a[ECHO] Alerts §cdisabled§a. You will no longer receive ECHO notifications.");
        }
    }
    
    /**
     * Send help message
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("§a§lECHO §8- §7Available Commands");
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("§a/echo status §8- §7View system status");
        sender.sendMessage("§a/echo check <player> §8- §7Check player integrity");
        sender.sendMessage("§a/echo profile <player> §8- §7View player profile");
        sender.sendMessage("§a/echo reload §8- §7Reload configuration");
        sender.sendMessage("§a/echo alerts [on|off] §8- §7Toggle alerts");
        sender.sendMessage("§7§m                                           ");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("status", "check", "profile", "reload", "alerts", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("profile"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("alerts")) {
            return Arrays.asList("on", "off");
        }
        
        return new ArrayList<>();
    }
}
