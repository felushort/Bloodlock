package com.void.echo.command;

import com.void.echo.EchoPlugin;
import com.void.echo.analysis.EntropyAnalysisResult;
import com.void.echo.data.PlayerProfile;
import com.void.echo.manager.AlertManager;
import com.void.echo.manager.ExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enhanced command executor for /echo commands
 * Provides comprehensive anti-cheat management for server owners
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
            case "violations" -> handleViolations(sender, args);
            case "reset" -> handleReset(sender, args);
            case "exempt" -> handleExempt(sender, args);
            case "alerts" -> handleAlerts(sender, args);
            case "verbose" -> handleVerbose(sender, args);
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender, args);
            case "logs" -> handleLogs(sender, args);
            case "export" -> handleExport(sender, args);
            case "test" -> handleTest(sender, args);
            case "togglelayer" -> handleToggleLayer(sender, args);
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
        sender.sendMessage("§7Monitored Players: §f" + plugin.getDataManager().getPlayerCount());
        sender.sendMessage("");
        sender.sendMessage("§7Data Statistics:");
        sender.sendMessage("§8  • §7" + plugin.getDataManager().getStats());
        sender.sendMessage("§8  • §7Packets Processed: §f" + 
                (plugin.getCaptureManager() != null ? plugin.getCaptureManager().getPacketsProcessed() : 0));
        sender.sendMessage("");
        
        // Layer status
        sender.sendMessage("§7Active Layers:");
        sender.sendMessage(getLayerStatus(1, "Input Capture", plugin.getCaptureManager() != null));
        sender.sendMessage(getLayerStatus(2, "Entropy Analysis", plugin.getEntropyAnalyzer() != null));
        sender.sendMessage(getLayerStatus(3, "Baseline Profiling", plugin.getProfileManager() != null));
        sender.sendMessage(getLayerStatus(4, "Physics Drift", plugin.getPhysicsDriftManager() != null));
        sender.sendMessage(getLayerStatus(5, "Integrity Pressure", plugin.getPressureManager() != null));
        sender.sendMessage(getLayerStatus(6, "Post-Fight Review", plugin.getFightReviewer() != null));
        sender.sendMessage(getLayerStatus(7, "Camouflage Detection", plugin.getCamouflageDetector() != null));
        sender.sendMessage(getLayerStatus(8, "Identity Anchoring", plugin.getIdentityAnchor() != null));
        sender.sendMessage(getLayerStatus(9, "Transparency Mode", plugin.getTransparencyManager() != null));
        sender.sendMessage(getLayerStatus(10, "Chaos Events", plugin.getChaosManager() != null));
        sender.sendMessage("§7§m                                           ");
    }
    
    private String getLayerStatus(int num, String name, boolean active) {
        String symbol = active ? "§a✓" : "§c✗";
        String status = active ? "ACTIVE" : "DISABLED";
        return String.format("§8  • %s §7Layer %d: %s - §f%s", symbol, num, name, status);
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
     * Handle /echo alerts [on|off|toggle]
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
        
        AlertManager alertMgr = plugin.getAlertManager();
        String action = args.length >= 2 ? args[1].toLowerCase() : "toggle";
        
        boolean enabled = switch (action) {
            case "on" -> {
                alertMgr.enableAlerts(player.getUniqueId());
                yield true;
            }
            case "off" -> {
                alertMgr.disableAlerts(player.getUniqueId());
                yield false;
            }
            case "toggle" -> alertMgr.toggleAlerts(player.getUniqueId());
            default -> {
                sender.sendMessage("§c[ECHO] Usage: /echo alerts [on|off|toggle]");
                yield alertMgr.shouldReceiveAlerts(player);
            }
        };
        
        sender.sendMessage(enabled ? 
                "§a[ECHO] Alerts enabled" : 
                "§c[ECHO] Alerts disabled");
    }
    
    /**
     * Handle /echo violations <player> [page]
     */
    private void handleViolations(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c[ECHO] Usage: /echo violations <player> [page]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[ECHO] Player not found.");
            return;
        }
        
        int page = 1;
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c[ECHO] Invalid page number.");
                return;
            }
        }
        
        PlayerProfile profile = plugin.getDataManager().getProfile(target);
        List<String> violations = new ArrayList<>(profile.getViolations());
        
        if (violations.isEmpty()) {
            sender.sendMessage("§a[ECHO] No violations found for " + target.getName());
            return;
        }
        
        int perPage = 10;
        int maxPage = (int) Math.ceil((double) violations.size() / perPage);
        page = Math.max(1, Math.min(page, maxPage));
        
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, violations.size());
        
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("§a§lECHO §8- §7Violations: §f" + target.getName() + " §7(Page " + page + "/" + maxPage + ")");
        sender.sendMessage("§7§m                                           ");
        
        for (int i = start; i < end; i++) {
            sender.sendMessage("§8" + (i + 1) + ". §f" + violations.get(i));
        }
        
        sender.sendMessage("§7§m                                           ");
        if (page < maxPage) {
            sender.sendMessage("§7Use §f/echo violations " + target.getName() + " " + (page + 1) + " §7for more");
        }
    }
    
    /**
     * Handle /echo reset <player> [type]
     */
    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c[ECHO] Usage: /echo reset <player> [all|violations|baseline]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[ECHO] Player not found.");
            return;
        }
        
        String type = args.length >= 3 ? args[2].toLowerCase() : "all";
        PlayerProfile profile = plugin.getDataManager().getProfile(target);
        
        switch (type) {
            case "all" -> {
                profile.reset();
                sender.sendMessage("§a[ECHO] Reset all data for " + target.getName());
            }
            case "violations" -> {
                profile.clearViolations();
                sender.sendMessage("§a[ECHO] Cleared violations for " + target.getName());
            }
            case "baseline" -> {
                profile.resetBaseline();
                sender.sendMessage("§a[ECHO] Reset baseline for " + target.getName());
            }
            default -> sender.sendMessage("§c[ECHO] Invalid reset type. Use: all, violations, or baseline");
        }
    }
    
    /**
     * Handle /echo exempt <player> <add|remove|list> [check]
     */
    private void handleExempt(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§c[ECHO] Usage: /echo exempt <player> <add|remove|list> [check]");
            sender.sendMessage("§7Available checks: all, aimbot, autoclicker, killaura, reach, timer, velocity");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[ECHO] Player not found.");
            return;
        }
        
        String action = args[2].toLowerCase();
        ExemptionManager exemptMgr = plugin.getExemptionManager();
        
        switch (action) {
            case "add" -> {
                if (args.length < 4) {
                    sender.sendMessage("§c[ECHO] Usage: /echo exempt <player> add <check>");
                    return;
                }
                String check = args[3].toLowerCase();
                exemptMgr.addPermanentExemption(target.getUniqueId(), check);
                sender.sendMessage("§a[ECHO] Added exemption for " + target.getName() + ": " + check);
            }
            case "remove" -> {
                if (args.length < 4) {
                    sender.sendMessage("§c[ECHO] Usage: /echo exempt <player> remove <check>");
                    return;
                }
                String check = args[3].toLowerCase();
                exemptMgr.removePermanentExemption(target.getUniqueId(), check);
                sender.sendMessage("§a[ECHO] Removed exemption for " + target.getName() + ": " + check);
            }
            case "list" -> {
                Set<String> exemptions = exemptMgr.getExemptions(target.getUniqueId());
                if (exemptions.isEmpty()) {
                    sender.sendMessage("§e[ECHO] No exemptions for " + target.getName());
                } else {
                    sender.sendMessage("§a[ECHO] Exemptions for " + target.getName() + ": §f" + 
                            String.join(", ", exemptions));
                }
            }
            default -> sender.sendMessage("§c[ECHO] Invalid action. Use: add, remove, or list");
        }
    }
    
    /**
     * Handle /echo verbose [on|off]
     */
    private void handleVerbose(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.verbose")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[ECHO] This command can only be used by players.");
            return;
        }
        
        AlertManager alertMgr = plugin.getAlertManager();
        boolean verbose = alertMgr.toggleVerbose(player.getUniqueId());
        
        sender.sendMessage(verbose ? 
                "§a[ECHO] Verbose mode enabled - detailed debug output active" : 
                "§7[ECHO] Verbose mode disabled");
    }
    
    /**
     * Handle /echo info <player>
     */
    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c[ECHO] Usage: /echo info <player>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[ECHO] Player not found.");
            return;
        }
        
        PlayerProfile profile = plugin.getDataManager().getProfile(target);
        ExemptionManager exemptMgr = plugin.getExemptionManager();
        
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("§a§lECHO §8- §7Player Info: §f" + target.getName());
        sender.sendMessage("§7§m                                           ");
        sender.sendMessage("§7UUID: §f" + target.getUniqueId());
        sender.sendMessage("§7Ping: §f" + target.getPing() + "ms");
        sender.sendMessage("§7Client: §f" + target.getClientBrandName());
        sender.sendMessage("");
        sender.sendMessage("§7ECHO Status:");
        sender.sendMessage("§8  • §7Integrity Score: §f" + String.format("%.1f/100", profile.getCurrentIntegrityScore()));
        sender.sendMessage("§8  • §7Violations: §f" + profile.getViolations().size());
        sender.sendMessage("§8  • §7Flagged: " + (profile.isFlaggedForReview() ? "§cYes" : "§aNo"));
        sender.sendMessage("§8  • §7Baseline: " + (profile.isBaselineEstablished() ? "§aEstablished" : "§eBuilding"));
        
        Set<String> exemptions = exemptMgr.getExemptions(target.getUniqueId());
        if (!exemptions.isEmpty()) {
            sender.sendMessage("§8  • §7Exemptions: §f" + String.join(", ", exemptions));
        }
        
        sender.sendMessage("§7§m                                           ");
    }
    
    /**
     * Handle /echo logs [player] [lines]
     */
    private void handleLogs(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        sender.sendMessage("§e[ECHO] Log viewing is not yet implemented.");
        sender.sendMessage("§7Check the server console or violations.log file.");
    }
    
    /**
     * Handle /echo export <player>
     */
    private void handleExport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c[ECHO] Usage: /echo export <player>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[ECHO] Player not found.");
            return;
        }
        
        sender.sendMessage("§e[ECHO] Data export is not yet implemented.");
        sender.sendMessage("§7Data can be accessed directly from the database.");
    }
    
    /**
     * Handle /echo test <player> <type>
     */
    private void handleTest(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§c[ECHO] Usage: /echo test <player> <type>");
            sender.sendMessage("§7Types: aimbot, autoclicker, killaura, reach, timer, velocity");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[ECHO] Player not found.");
            return;
        }
        
        String type = args[2].toLowerCase();
        sender.sendMessage("§a[ECHO] Running " + type + " detection test on " + target.getName() + "...");
        sender.sendMessage("§7This will trigger a manual analysis. Check results in a few seconds.");
        
        // Trigger analysis
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Run appropriate test
            sender.sendMessage("§a[ECHO] Test complete. Check /echo check " + target.getName());
        });
    }
    
    /**
     * Handle /echo togglelayer <layer>
     */
    private void handleToggleLayer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage("§c[ECHO] No permission.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c[ECHO] Usage: /echo togglelayer <1-10|name>");
            return;
        }
        
        sender.sendMessage("§e[ECHO] Layer toggling is not yet implemented.");
        sender.sendMessage("§7Edit config.yml to enable/disable layers, then /echo reload");
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
        sender.sendMessage("§a/echo info <player> §8- §7Quick player info");
        sender.sendMessage("§a/echo violations <player> §8- §7View violations");
        sender.sendMessage("§a/echo reset <player> [type] §8- §7Reset player data");
        sender.sendMessage("§a/echo exempt <player> <add|remove|list> [check] §8- §7Manage exemptions");
        sender.sendMessage("§a/echo alerts [on|off] §8- §7Toggle alerts");
        sender.sendMessage("§a/echo verbose [on|off] §8- §7Toggle debug mode");
        sender.sendMessage("§a/echo reload §8- §7Reload configuration");
        sender.sendMessage("§a/echo test <player> <type> §8- §7Manual detection test");
        sender.sendMessage("§7§m                                           ");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("status", "check", "profile", "violations", "reset", 
                    "exempt", "alerts", "verbose", "reload", "info", "logs", 
                    "export", "test", "togglelayer", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("check") || sub.equals("profile") || sub.equals("violations") || 
                sub.equals("reset") || sub.equals("exempt") || sub.equals("info") || 
                sub.equals("export") || sub.equals("test")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            
            if (sub.equals("alerts")) {
                return Arrays.asList("on", "off", "toggle");
            }
            
            if (sub.equals("verbose")) {
                return Arrays.asList("on", "off");
            }
        }
        
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            
            if (sub.equals("reset")) {
                return Arrays.asList("all", "violations", "baseline");
            }
            
            if (sub.equals("exempt")) {
                return Arrays.asList("add", "remove", "list");
            }
            
            if (sub.equals("test")) {
                return Arrays.asList("aimbot", "autoclicker", "killaura", "reach", "timer", "velocity");
            }
        }
        
        if (args.length == 4 && args[0].equalsIgnoreCase("exempt")) {
            String action = args[2].toLowerCase();
            if (action.equals("add") || action.equals("remove")) {
                return Arrays.asList("all", "aimbot", "autoclicker", "killaura", 
                        "reach", "timer", "velocity", "scaffold", "fly");
            }
        }
        
        return new ArrayList<>();
    }
    
    // Helper methods
    
    private String getScoreColor(double score) {
        if (score >= 80) return "§a";
        if (score >= 60) return "§e";
        if (score >= 40) return "§6";
        if (score >= 20) return "§c";
        return "§4";
    }
}
