package com.void.echo.storage;

import com.void.echo.data.PlayerProfile;
import lombok.extern.java.Log;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQLite-based persistent storage for ECHO data
 * 
 * Stores:
 * - Player profiles and baselines
 * - Integrity scores over time
 * - Violation history
 * - Fight recordings (metadata)
 * - Statistical trends
 */
@Log
public class DatabaseManager {
    
    private final File databaseFile;
    private Connection connection;
    
    public DatabaseManager(File dataFolder) {
        this.databaseFile = new File(dataFolder, "echo.db");
        initialize();
    }
    
    /**
     * Initialize database connection and schema
     */
    public void initialize() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Create connection
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            // Create tables
            createTables();
            
            log.info("Database initialized: " + databaseFile.getAbsolutePath());
            
        } catch (Exception e) {
            log.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create database schema
     */
    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();
        
        // Player profiles table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS player_profiles (" +
            "uuid TEXT PRIMARY KEY," +
            "name TEXT NOT NULL," +
            "first_seen INTEGER NOT NULL," +
            "last_seen INTEGER NOT NULL," +
            "total_playtime INTEGER DEFAULT 0," +
            "session_count INTEGER DEFAULT 0," +
            "current_integrity_score REAL DEFAULT 100.0," +
            "lowest_integrity_score REAL DEFAULT 100.0," +
            "baseline_established INTEGER DEFAULT 0," +
            "flagged_for_review INTEGER DEFAULT 0," +
            "avg_yaw_delta REAL DEFAULT 0.0," +
            "std_dev_yaw_delta REAL DEFAULT 0.0," +
            "avg_pitch_delta REAL DEFAULT 0.0," +
            "std_dev_pitch_delta REAL DEFAULT 0.0," +
            "avg_cps REAL DEFAULT 0.0," +
            "std_dev_cps REAL DEFAULT 0.0," +
            "avg_reaction_time REAL DEFAULT 0.0," +
            "std_dev_reaction_time REAL DEFAULT 0.0," +
            "hit_accuracy REAL DEFAULT 0.0" +
            ")"
        );
        
        // Integrity history table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS integrity_history (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid TEXT NOT NULL," +
            "timestamp INTEGER NOT NULL," +
            "integrity_score REAL NOT NULL," +
            "yaw_entropy REAL," +
            "pitch_entropy REAL," +
            "click_distribution REAL," +
            "reaction_time REAL," +
            "crosshair_alignment REAL," +
            "aimbot_confidence REAL," +
            "autoclicker_confidence REAL," +
            "FOREIGN KEY(uuid) REFERENCES player_profiles(uuid)" +
            ")"
        );
        
        // Create index for faster queries
        stmt.execute(
            "CREATE INDEX IF NOT EXISTS idx_integrity_history_uuid " +
            "ON integrity_history(uuid)"
        );
        
        stmt.execute(
            "CREATE INDEX IF NOT EXISTS idx_integrity_history_timestamp " +
            "ON integrity_history(timestamp)"
        );
        
        // Violations table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS violations (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid TEXT NOT NULL," +
            "timestamp INTEGER NOT NULL," +
            "violation_type TEXT NOT NULL," +
            "severity REAL NOT NULL," +
            "description TEXT," +
            "score_at_time REAL," +
            "FOREIGN KEY(uuid) REFERENCES player_profiles(uuid)" +
            ")"
        );
        
        stmt.execute(
            "CREATE INDEX IF NOT EXISTS idx_violations_uuid " +
            "ON violations(uuid)"
        );
        
        // Fight recordings metadata table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS fight_recordings (" +
            "recording_id TEXT PRIMARY KEY," +
            "attacker_uuid TEXT NOT NULL," +
            "start_time INTEGER NOT NULL," +
            "end_time INTEGER," +
            "duration INTEGER," +
            "total_hits INTEGER DEFAULT 0," +
            "total_misses INTEGER DEFAULT 0," +
            "accuracy REAL," +
            "avg_reach REAL," +
            "max_reach REAL," +
            "combo_duration INTEGER," +
            "fight_won INTEGER," +
            "suspicion_level REAL," +
            "naturalness_score REAL," +
            "has_anomalies INTEGER," +
            "FOREIGN KEY(attacker_uuid) REFERENCES player_profiles(uuid)" +
            ")"
        );
        
        // Detection events table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS detection_events (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid TEXT NOT NULL," +
            "timestamp INTEGER NOT NULL," +
            "detection_type TEXT NOT NULL," +
            "confidence REAL NOT NULL," +
            "details TEXT," +
            "FOREIGN KEY(uuid) REFERENCES player_profiles(uuid)" +
            ")"
        );
        
        stmt.close();
    }
    
    /**
     * Save player profile
     */
    public void saveProfile(PlayerProfile profile) {
        try {
            String sql = "INSERT OR REPLACE INTO player_profiles " +
                        "(uuid, name, first_seen, last_seen, total_playtime, session_count, " +
                        "current_integrity_score, lowest_integrity_score, baseline_established, " +
                        "flagged_for_review, avg_yaw_delta, std_dev_yaw_delta, avg_pitch_delta, " +
                        "std_dev_pitch_delta, avg_cps, std_dev_cps, avg_reaction_time, " +
                        "std_dev_reaction_time, hit_accuracy) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, profile.getPlayerId().toString());
            stmt.setString(2, profile.getPlayerName());
            stmt.setLong(3, profile.getFirstSeen());
            stmt.setLong(4, profile.getLastSeen());
            stmt.setLong(5, profile.getTotalPlaytime());
            stmt.setInt(6, profile.getSessionCount());
            stmt.setDouble(7, profile.getCurrentIntegrityScore());
            stmt.setDouble(8, profile.getLowestIntegrityScore());
            stmt.setInt(9, profile.isBaselineEstablished() ? 1 : 0);
            stmt.setInt(10, profile.isFlaggedForReview() ? 1 : 0);
            stmt.setDouble(11, profile.getAvgYawDelta());
            stmt.setDouble(12, profile.getStdDevYawDelta());
            stmt.setDouble(13, profile.getAvgPitchDelta());
            stmt.setDouble(14, profile.getStdDevPitchDelta());
            stmt.setDouble(15, profile.getAvgCPS());
            stmt.setDouble(16, profile.getStdDevCPS());
            stmt.setDouble(17, profile.getAvgReactionTime());
            stmt.setDouble(18, profile.getStdDevReactionTime());
            stmt.setDouble(19, profile.getHitAccuracy());
            
            stmt.executeUpdate();
            stmt.close();
            
        } catch (SQLException e) {
            log.severe("Failed to save profile: " + e.getMessage());
        }
    }
    
    /**
     * Load player profile
     */
    public PlayerProfile loadProfile(UUID playerId) {
        try {
            String sql = "SELECT * FROM player_profiles WHERE uuid = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, playerId.toString());
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                PlayerProfile profile = new PlayerProfile(
                    playerId,
                    rs.getString("name")
                );
                
                profile.setFirstSeen(rs.getLong("first_seen"));
                profile.setLastSeen(rs.getLong("last_seen"));
                profile.setTotalPlaytime(rs.getLong("total_playtime"));
                profile.setSessionCount(rs.getInt("session_count"));
                profile.setCurrentIntegrityScore(rs.getDouble("current_integrity_score"));
                profile.setLowestIntegrityScore(rs.getDouble("lowest_integrity_score"));
                profile.setBaselineEstablished(rs.getInt("baseline_established") == 1);
                profile.setFlaggedForReview(rs.getInt("flagged_for_review") == 1);
                profile.setAvgYawDelta(rs.getDouble("avg_yaw_delta"));
                profile.setStdDevYawDelta(rs.getDouble("std_dev_yaw_delta"));
                profile.setAvgPitchDelta(rs.getDouble("avg_pitch_delta"));
                profile.setStdDevPitchDelta(rs.getDouble("std_dev_pitch_delta"));
                profile.setAvgCPS(rs.getDouble("avg_cps"));
                profile.setStdDevCPS(rs.getDouble("std_dev_cps"));
                profile.setAvgReactionTime(rs.getDouble("avg_reaction_time"));
                profile.setStdDevReactionTime(rs.getDouble("std_dev_reaction_time"));
                profile.setHitAccuracy(rs.getDouble("hit_accuracy"));
                
                rs.close();
                stmt.close();
                
                return profile;
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            log.severe("Failed to load profile: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Record integrity score
     */
    public void recordIntegrityScore(UUID playerId, double score, 
                                     double yawEntropy, double pitchEntropy,
                                     double clickDist, double reactionTime,
                                     double crosshairAlign, double aimbotConf,
                                     double autoclickerConf) {
        try {
            String sql = "INSERT INTO integrity_history " +
                        "(uuid, timestamp, integrity_score, yaw_entropy, pitch_entropy, " +
                        "click_distribution, reaction_time, crosshair_alignment, " +
                        "aimbot_confidence, autoclicker_confidence) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, playerId.toString());
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setDouble(3, score);
            stmt.setDouble(4, yawEntropy);
            stmt.setDouble(5, pitchEntropy);
            stmt.setDouble(6, clickDist);
            stmt.setDouble(7, reactionTime);
            stmt.setDouble(8, crosshairAlign);
            stmt.setDouble(9, aimbotConf);
            stmt.setDouble(10, autoclickerConf);
            
            stmt.executeUpdate();
            stmt.close();
            
        } catch (SQLException e) {
            log.severe("Failed to record integrity score: " + e.getMessage());
        }
    }
    
    /**
     * Record violation
     */
    public void recordViolation(UUID playerId, String type, double severity, 
                               String description, double scoreAtTime) {
        try {
            String sql = "INSERT INTO violations " +
                        "(uuid, timestamp, violation_type, severity, description, score_at_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, playerId.toString());
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, type);
            stmt.setDouble(4, severity);
            stmt.setString(5, description);
            stmt.setDouble(6, scoreAtTime);
            
            stmt.executeUpdate();
            stmt.close();
            
        } catch (SQLException e) {
            log.severe("Failed to record violation: " + e.getMessage());
        }
    }
    
    /**
     * Get recent violations
     */
    public List<ViolationRecord> getRecentViolations(UUID playerId, int limit) {
        List<ViolationRecord> violations = new ArrayList<>();
        
        try {
            String sql = "SELECT * FROM violations WHERE uuid = ? " +
                        "ORDER BY timestamp DESC LIMIT ?";
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, playerId.toString());
            stmt.setInt(2, limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ViolationRecord record = new ViolationRecord();
                record.timestamp = rs.getLong("timestamp");
                record.type = rs.getString("violation_type");
                record.severity = rs.getDouble("severity");
                record.description = rs.getString("description");
                record.scoreAtTime = rs.getDouble("score_at_time");
                
                violations.add(record);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            log.severe("Failed to get violations: " + e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Close database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            log.severe("Failed to close database: " + e.getMessage());
        }
    }
    
    /**
     * Violation record data class
     */
    public static class ViolationRecord {
        public long timestamp;
        public String type;
        public double severity;
        public String description;
        public double scoreAtTime;
    }
}
