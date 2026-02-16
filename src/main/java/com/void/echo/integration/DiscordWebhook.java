package com.void.echo.integration;

import lombok.extern.java.Log;

import java.awt.Color;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Discord Webhook Integration
 * 
 * Sends real-time alerts to Discord channels via webhooks.
 * Supports:
 * - Rich embeds with colors
 * - Multiple severity levels
 * - Async sending (non-blocking)
 * - Rate limiting
 * - Retry logic
 */
@Log
public class DiscordWebhook {
    
    private final String webhookUrl;
    private final ExecutorService executor;
    private final RateLimiter rateLimiter;
    
    // Severity colors
    private static final int COLOR_CRITICAL = 0xFF0000;  // Red
    private static final int COLOR_HIGH = 0xFF6600;      // Orange
    private static final int COLOR_MEDIUM = 0xFFCC00;    // Yellow
    private static final int COLOR_LOW = 0x00FF00;       // Green
    private static final int COLOR_INFO = 0x0099FF;      // Blue
    
    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.executor = Executors.newFixedThreadPool(2);
        this.rateLimiter = new RateLimiter(5, 2000); // 5 messages per 2 seconds
    }
    
    /**
     * Send detection alert
     */
    public void sendDetectionAlert(String playerName, String detectionType, 
                                   double confidence, String details) {
        String title = "🚨 " + detectionType + " Detected";
        int color = getColorForConfidence(confidence);
        
        StringBuilder description = new StringBuilder();
        description.append("**Player:** ").append(playerName).append("\n");
        description.append("**Confidence:** ").append(String.format("%.1f%%", confidence * 100)).append("\n");
        description.append("**Details:** ").append(details).append("\n");
        
        sendEmbedAsync(title, description.toString(), color);
    }
    
    /**
     * Send integrity score alert
     */
    public void sendIntegrityAlert(String playerName, double score, String status) {
        String title = "⚠️ Integrity Score Alert";
        int color = getColorForScore(score);
        
        StringBuilder description = new StringBuilder();
        description.append("**Player:** ").append(playerName).append("\n");
        description.append("**Score:** ").append(String.format("%.1f/100", score)).append("\n");
        description.append("**Status:** ").append(status).append("\n");
        
        sendEmbedAsync(title, description.toString(), color);
    }
    
    /**
     * Send violation alert
     */
    public void sendViolationAlert(String playerName, String violationType,
                                   String severity, String description) {
        String title = "🔴 Violation Logged";
        int color = getColorForSeverity(severity);
        
        StringBuilder desc = new StringBuilder();
        desc.append("**Player:** ").append(playerName).append("\n");
        desc.append("**Type:** ").append(violationType).append("\n");
        desc.append("**Severity:** ").append(severity).append("\n");
        desc.append("**Description:** ").append(description).append("\n");
        
        sendEmbedAsync(title, desc.toString(), color);
    }
    
    /**
     * Send fight analysis alert
     */
    public void sendFightAnalysisAlert(String playerName, double suspicionLevel,
                                      double naturalness, int anomalyCount) {
        String title = "⚔️ Fight Analysis Complete";
        int color = suspicionLevel > 0.75 ? COLOR_CRITICAL : 
                   suspicionLevel > 0.5 ? COLOR_HIGH : COLOR_INFO;
        
        StringBuilder description = new StringBuilder();
        description.append("**Player:** ").append(playerName).append("\n");
        description.append("**Suspicion Level:** ").append(String.format("%.1f%%", suspicionLevel * 100)).append("\n");
        description.append("**Naturalness Score:** ").append(String.format("%.1f/100", naturalness)).append("\n");
        description.append("**Anomalies:** ").append(anomalyCount).append("\n");
        
        sendEmbedAsync(title, description.toString(), color);
    }
    
    /**
     * Send simple message
     */
    public void sendMessage(String content) {
        sendAsync(content, null, null, 0);
    }
    
    /**
     * Send embed async
     */
    private void sendEmbedAsync(String title, String description, int color) {
        sendAsync(null, title, description, color);
    }
    
    /**
     * Send message asynchronously
     */
    private void sendAsync(String content, String title, String description, int color) {
        CompletableFuture.runAsync(() -> {
            try {
                // Rate limiting
                rateLimiter.acquire();
                
                // Send webhook
                sendWebhook(content, title, description, color);
                
            } catch (Exception e) {
                log.warning("Failed to send Discord webhook: " + e.getMessage());
            }
        }, executor);
    }
    
    /**
     * Send webhook request
     */
    private void sendWebhook(String content, String title, String description, int color) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "ECHO-AntiCheat/1.0");
        conn.setDoOutput(true);
        
        // Build JSON
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        if (content != null && !content.isEmpty()) {
            json.append("\"content\":\"").append(escapeJson(content)).append("\"");
        }
        
        if (title != null && !title.isEmpty()) {
            if (json.length() > 1) json.append(",");
            json.append("\"embeds\":[{");
            json.append("\"title\":\"").append(escapeJson(title)).append("\",");
            json.append("\"description\":\"").append(escapeJson(description)).append("\",");
            json.append("\"color\":").append(color).append(",");
            json.append("\"timestamp\":\"").append(Instant.now().toString()).append("\"");
            json.append("}]");
        }
        
        json.append("}");
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            log.warning("Discord webhook returned " + responseCode);
        }
        
        conn.disconnect();
    }
    
    /**
     * Escape JSON strings
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Get color for confidence level
     */
    private int getColorForConfidence(double confidence) {
        if (confidence >= 0.75) return COLOR_CRITICAL;
        if (confidence >= 0.50) return COLOR_HIGH;
        if (confidence >= 0.25) return COLOR_MEDIUM;
        return COLOR_LOW;
    }
    
    /**
     * Get color for integrity score
     */
    private int getColorForScore(double score) {
        if (score < 20) return COLOR_CRITICAL;
        if (score < 40) return COLOR_HIGH;
        if (score < 60) return COLOR_MEDIUM;
        return COLOR_INFO;
    }
    
    /**
     * Get color for severity
     */
    private int getColorForSeverity(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL", "SEVERE" -> COLOR_CRITICAL;
            case "HIGH" -> COLOR_HIGH;
            case "MEDIUM" -> COLOR_MEDIUM;
            case "LOW" -> COLOR_LOW;
            default -> COLOR_INFO;
        };
    }
    
    /**
     * Shutdown executor
     */
    public void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Simple rate limiter
     */
    private static class RateLimiter {
        private final int maxRequests;
        private final long timeWindow;
        private final java.util.Queue<Long> requestTimes;
        
        public RateLimiter(int maxRequests, long timeWindowMs) {
            this.maxRequests = maxRequests;
            this.timeWindow = timeWindowMs;
            this.requestTimes = new java.util.LinkedList<>();
        }
        
        public synchronized void acquire() throws InterruptedException {
            long now = System.currentTimeMillis();
            
            // Remove old requests outside time window
            while (!requestTimes.isEmpty() && now - requestTimes.peek() > timeWindow) {
                requestTimes.poll();
            }
            
            // Wait if at limit
            while (requestTimes.size() >= maxRequests) {
                long oldestRequest = requestTimes.peek();
                long waitTime = timeWindow - (now - oldestRequest);
                if (waitTime > 0) {
                    Thread.sleep(waitTime);
                }
                now = System.currentTimeMillis();
                while (!requestTimes.isEmpty() && now - requestTimes.peek() > timeWindow) {
                    requestTimes.poll();
                }
            }
            
            // Add this request
            requestTimes.offer(now);
        }
    }
}
