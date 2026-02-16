package com.void.echo;

import com.void.echo.capture.PacketCaptureManager;
import com.void.echo.analysis.EntropyAnalyzer;
import com.void.echo.profile.ProfileManager;
import com.void.echo.physics.PhysicsDriftManager;
import com.void.echo.pressure.IntegrityPressureManager;
import com.void.echo.review.PostFightReviewer;
import com.void.echo.camouflage.CamouflageDetector;
import com.void.echo.identity.IdentityAnchor;
import com.void.echo.transparency.TransparencyManager;
import com.void.echo.chaos.ChaosEventManager;
import com.void.echo.command.EchoCommandExecutor;
import com.void.echo.data.PlayerDataManager;
import com.void.echo.manager.AlertManager;
import com.void.echo.manager.ExemptionManager;
import com.void.echo.integration.LuckPermsIntegration;
import com.void.echo.util.ConfigManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * ECHO - The Behavioral Integrity Engine
 * 
 * A next-generation anti-cheat that models human behavior
 * rather than detecting specific cheat patterns.
 * 
 * Philosophy: Stop detecting cheats. Start modeling reality.
 * 
 * @author VoidDev
 * @version 1.0.0-ALPHA
 */
public class EchoPlugin extends JavaPlugin {
    
    @Getter
    private static EchoPlugin instance;
    
    // Configuration
    @Getter
    private ConfigManager configManager;
    
    // Core Managers
    @Getter
    private PlayerDataManager dataManager;
    
    @Getter
    private AlertManager alertManager;
    
    @Getter
    private ExemptionManager exemptionManager;
    
    // Integrations
    @Getter
    private LuckPermsIntegration luckPermsIntegration;
    
    // Layer 1: Input Capture
    @Getter
    private PacketCaptureManager captureManager;
    
    // Layer 2: Entropy Analysis
    @Getter
    private EntropyAnalyzer entropyAnalyzer;
    
    // Layer 3: Profiling
    @Getter
    private ProfileManager profileManager;
    
    // Layer 4: Physics Drift
    @Getter
    private PhysicsDriftManager physicsDriftManager;
    
    // Layer 5: Integrity Pressure
    @Getter
    private IntegrityPressureManager pressureManager;
    
    // Layer 6: Post-Fight Review
    @Getter
    private PostFightReviewer fightReviewer;
    
    // Layer 7: Camouflage Detection
    @Getter
    private CamouflageDetector camouflageDetector;
    
    // Layer 8: Identity Anchoring
    @Getter
    private IdentityAnchor identityAnchor;
    
    // Layer 9: Transparency
    @Getter
    private TransparencyManager transparencyManager;
    
    // Layer 10: Chaos Events
    @Getter
    private ChaosEventManager chaosManager;
    
    // Thread pool for async processing
    @Getter
    private ExecutorService executorService;
    
    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("  ECHO - Behavioral Integrity Engine  ");
        getLogger().info("  Stop detecting cheats.              ");
        getLogger().info("  Start modeling reality.             ");
        getLogger().info("═══════════════════════════════════════");
        
        // Check for ProtocolLib
        if (!getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().severe("ProtocolLib not found! ECHO requires ProtocolLib to function.");
            getLogger().severe("Download: https://www.spigotmc.org/resources/protocollib.1997/");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize thread pool
        int threadPoolSize = configManager.getInt("performance.thread-pool-size", 4);
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        getLogger().info("Initialized thread pool with " + threadPoolSize + " threads");
        
        // Initialize core systems
        initializeCore();
        
        // Initialize layers
        initializeLayers();
        
        // Register commands
        getCommand("echo").setExecutor(new EchoCommandExecutor(this));
        
        getLogger().info("ECHO has been enabled successfully!");
        getLogger().info("All 10 behavioral layers are active.");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Shutting down ECHO...");
        
        // Shutdown thread pool
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
        
        // Save all player data
        if (dataManager != null) {
            dataManager.saveAll();
        }
        
        // Cleanup managers
        if (captureManager != null) captureManager.shutdown();
        if (transparencyManager != null) transparencyManager.shutdown();
        if (chaosManager != null) chaosManager.shutdown();
        
        getLogger().info("ECHO has been disabled. All data saved.");
    }
    
    /**
     * Initialize core systems
     */
    private void initializeCore() {
        try {
            // Data management
            dataManager = new PlayerDataManager(this);
            getLogger().info("✓ Core data management initialized");
            
            // Alert management
            alertManager = new AlertManager(this);
            getLogger().info("✓ Alert management initialized");
            
            // Exemption management
            exemptionManager = new ExemptionManager(this);
            getLogger().info("✓ Exemption management initialized");
            
            // LuckPerms integration (optional)
            luckPermsIntegration = new LuckPermsIntegration(this);
            if (luckPermsIntegration.isEnabled()) {
                getLogger().info("✓ LuckPerms integration active");
            }
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize core systems!", e);
        }
    }
    
    /**
     * Initialize all 10 layers of ECHO
     */
    private void initializeLayers() {
        try {
            // Layer 1: High-Resolution Input Capture
            if (configManager.getBoolean("capture.enabled", true)) {
                captureManager = new PacketCaptureManager(this);
                captureManager.initialize();
                getLogger().info("✓ Layer 1: Input Capture - ACTIVE");
            } else {
                getLogger().warning("✗ Layer 1: Input Capture - DISABLED");
            }
            
            // Layer 2: Human Entropy Modeling
            if (configManager.getBoolean("entropy.enabled", true)) {
                entropyAnalyzer = new EntropyAnalyzer(this);
                getLogger().info("✓ Layer 2: Entropy Modeling - ACTIVE");
            } else {
                getLogger().warning("✗ Layer 2: Entropy Modeling - DISABLED");
            }
            
            // Layer 3: Personal Baseline Profiling
            if (configManager.getBoolean("profiling.enabled", true)) {
                profileManager = new ProfileManager(this);
                getLogger().info("✓ Layer 3: Baseline Profiling - ACTIVE");
            } else {
                getLogger().warning("✗ Layer 3: Baseline Profiling - DISABLED");
            }
            
            // Layer 4: Dynamic Physics Drift
            if (configManager.getBoolean("physics-drift.enabled", true)) {
                physicsDriftManager = new PhysicsDriftManager(this);
                physicsDriftManager.start();
                getLogger().info("✓ Layer 4: Physics Drift - ACTIVE");
            } else {
                getLogger().warning("✗ Layer 4: Physics Drift - DISABLED");
            }
            
            // Layer 5: Silent Integrity Pressure
            if (configManager.getBoolean("integrity-pressure.enabled", true)) {
                pressureManager = new IntegrityPressureManager(this);
                getLogger().info("✓ Layer 5: Integrity Pressure - ACTIVE");
            } else {
                getLogger().warning("✗ Layer 5: Integrity Pressure - DISABLED");
            }
            
            // Layer 6: Post-Fight Neural Review
            if (configManager.getBoolean("neural-review.enabled", true)) {
                fightReviewer = new PostFightReviewer(this);
                fightReviewer.start();
                getLogger().info("✓ Layer 6: Neural Review - ACTIVE");
            } else {
                getLogger().warning("✗ Layer 6: Neural Review - DISABLED");
            }
            
            // Layer 7: Social Camouflage Detection
            if (configManager.getBoolean("camouflage-detection.enabled", true)) {
                camouflageDetector = new CamouflageDetector(this);
                getLogger().info("✓ Layer 7: Camouflage Detection - ACTIVE");
            } else {
                getLogger().warning("✗ Layer 7: Camouflage Detection - DISABLED");
            }
            
            // Layer 8: Anti-Spoof Identity Anchoring
            if (configManager.getBoolean("identity.enabled", true)) {
                identityAnchor = new IdentityAnchor(this);
                getLogger().info("✓ Layer 8: Identity Anchoring - ACTIVE");
            } else {
                getLogger().warning("✗ Layer 8: Identity Anchoring - DISABLED");
            }
            
            // Layer 9: Public Transparency Mode
            if (configManager.getBoolean("transparency.show-scores", true)) {
                transparencyManager = new TransparencyManager(this);
                transparencyManager.start();
                getLogger().info("✓ Layer 9: Transparency Mode - ACTIVE");
            } else {
                getLogger().warning("✗ Layer 9: Transparency Mode - DISABLED");
            }
            
            // Layer 10: Controlled Chaos Events
            if (configManager.getBoolean("chaos-events.enabled", true)) {
                chaosManager = new ChaosEventManager(this);
                chaosManager.start();
                getLogger().info("✓ Layer 10: Chaos Events - ACTIVE");
            } else {
                getLogger().warning("✗ Layer 10: Chaos Events - DISABLED");
            }
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize ECHO layers!", e);
        }
    }
    
    /**
     * Reload ECHO configuration and systems
     */
    public void reload() {
        getLogger().info("Reloading ECHO configuration...");
        
        // Reload config
        configManager.loadConfig();
        
        // Reinitialize layers
        initializeLayers();
        
        getLogger().info("ECHO configuration reloaded successfully!");
    }
}
