package com.void.echo.capture;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.void.echo.EchoPlugin;
import com.void.echo.data.BehavioralBuffer;
import com.void.echo.data.PlayerDataManager;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Layer 1: High-Resolution Input Capture
 * 
 * Captures every rotation, movement, attack, and interaction packet
 * with nanosecond-level timing precision.
 * 
 * This is the foundation - without accurate capture, all analysis fails.
 */
public class PacketCaptureManager {
    
    private final EchoPlugin plugin;
    private final PlayerDataManager dataManager;
    private final ProtocolManager protocolManager;
    
    @Getter
    private boolean initialized = false;
    
    // Packet listeners
    private PacketAdapter rotationListener;
    private PacketAdapter movementListener;
    private PacketAdapter attackListener;
    
    // Statistics
    private long packetsProcessed = 0;
    
    public PacketCaptureManager(EchoPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }
    
    /**
     * Initialize packet listeners
     */
    public void initialize() {
        plugin.getLogger().info("Initializing packet capture system...");
        
        // Register rotation packet listener
        if (plugin.getConfigManager().getBoolean("capture.packets.rotation", true)) {
            registerRotationListener();
        }
        
        // Register movement packet listener
        if (plugin.getConfigManager().getBoolean("capture.packets.movement", true)) {
            registerMovementListener();
        }
        
        // Register attack packet listener
        if (plugin.getConfigManager().getBoolean("capture.packets.attack", true)) {
            registerAttackListener();
        }
        
        initialized = true;
        plugin.getLogger().info("Packet capture system initialized!");
    }
    
    /**
     * Register rotation packet listener
     * Captures: PacketPlayInLook and PacketPlayInPositionLook
     */
    private void registerRotationListener() {
        rotationListener = new PacketAdapter(
                plugin,
                ListenerPriority.MONITOR,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.POSITION_LOOK
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isCancelled()) return;
                
                Player player = event.getPlayer();
                if (player == null) return;
                
                // Skip if player has bypass permission
                if (player.hasPermission("echo.bypass")) return;
                
                PacketContainer packet = event.getPacket();
                
                // Extract rotation data
                float yaw = packet.getFloat().read(0);     // Yaw
                float pitch = packet.getFloat().read(1);   // Pitch
                
                // Get player's behavioral buffer
                BehavioralBuffer buffer = dataManager.getBuffer(player.getUniqueId());
                
                // Add rotation to buffer
                buffer.addRotation(yaw, pitch);
                
                packetsProcessed++;
            }
        };
        
        protocolManager.addPacketListener(rotationListener);
        plugin.getLogger().info("  ✓ Rotation capture enabled");
    }
    
    /**
     * Register movement packet listener
     * Captures: PacketPlayInPosition and PacketPlayInPositionLook
     */
    private void registerMovementListener() {
        movementListener = new PacketAdapter(
                plugin,
                ListenerPriority.MONITOR,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isCancelled()) return;
                
                Player player = event.getPlayer();
                if (player == null) return;
                
                // Skip if player has bypass permission
                if (player.hasPermission("echo.bypass")) return;
                
                PacketContainer packet = event.getPacket();
                
                // Extract position data
                double x = packet.getDoubles().read(0);
                double y = packet.getDoubles().read(1);
                double z = packet.getDoubles().read(2);
                boolean onGround = packet.getBooleans().read(0);
                
                Vector position = new Vector(x, y, z);
                Vector velocity = player.getVelocity();
                
                // Get player's behavioral buffer
                BehavioralBuffer buffer = dataManager.getBuffer(player.getUniqueId());
                
                // Add movement to buffer
                buffer.addMovement(
                        position,
                        velocity,
                        onGround,
                        player.isSprinting(),
                        player.isSneaking()
                );
                
                packetsProcessed++;
            }
        };
        
        protocolManager.addPacketListener(movementListener);
        plugin.getLogger().info("  ✓ Movement capture enabled");
    }
    
    /**
     * Register attack packet listener
     * Captures: PacketPlayInUseEntity (attack action)
     */
    private void registerAttackListener() {
        attackListener = new PacketAdapter(
                plugin,
                ListenerPriority.MONITOR,
                PacketType.Play.Client.USE_ENTITY
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isCancelled()) return;
                
                Player player = event.getPlayer();
                if (player == null) return;
                
                // Skip if player has bypass permission
                if (player.hasPermission("echo.bypass")) return;
                
                PacketContainer packet = event.getPacket();
                
                // Check if this is an attack action (not interact)
                try {
                    var actionField = packet.getEntityUseActions().read(0);
                    
                    // Only process attack actions
                    if (actionField.getAction() != com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction.ATTACK) {
                        return;
                    }
                    
                    // Get target entity ID
                    int entityId = packet.getIntegers().read(0);
                    
                    // Calculate target information
                    org.bukkit.entity.Entity target = null;
                    double distance = 0.0;
                    float yawAlignment = 0.0f;
                    
                    // Find the target entity
                    for (org.bukkit.entity.Entity entity : player.getWorld().getEntities()) {
                        if (entity.getEntityId() == entityId) {
                            target = entity;
                            break;
                        }
                    }
                    
                    if (target != null) {
                        // Calculate distance
                        distance = player.getLocation().distance(target.getLocation());
                        
                        // Calculate yaw alignment (how well aimed the player is)
                        Vector toTarget = target.getLocation().toVector()
                                .subtract(player.getLocation().toVector())
                                .normalize();
                        
                        Vector playerDirection = player.getLocation().getDirection();
                        
                        // Dot product gives alignment (-1 to 1, where 1 is perfect)
                        yawAlignment = (float) playerDirection.dot(toTarget);
                    }
                    
                    // Get player's behavioral buffer
                    BehavioralBuffer buffer = dataManager.getBuffer(player.getUniqueId());
                    
                    // Add click to buffer
                    buffer.addClick(
                            target != null ? entityId : -1,
                            distance,
                            yawAlignment
                    );
                    
                    packetsProcessed++;
                    
                } catch (Exception e) {
                    // Silently skip malformed packets
                }
            }
        };
        
        protocolManager.addPacketListener(attackListener);
        plugin.getLogger().info("  ✓ Attack capture enabled");
    }
    
    /**
     * Shutdown packet capture
     */
    public void shutdown() {
        if (rotationListener != null) {
            protocolManager.removePacketListener(rotationListener);
        }
        if (movementListener != null) {
            protocolManager.removePacketListener(movementListener);
        }
        if (attackListener != null) {
            protocolManager.removePacketListener(attackListener);
        }
        
        plugin.getLogger().info("Packet capture system shut down. Total packets processed: " + packetsProcessed);
    }
    
    /**
     * Get statistics
     */
    public long getPacketsProcessed() {
        return packetsProcessed;
    }
}
