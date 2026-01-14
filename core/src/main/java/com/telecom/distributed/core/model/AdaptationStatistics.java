package com.telecom.distributed.core.model;

import java.time.LocalDateTime;

/**
 * Statistics about system adaptation behavior.
 */
public class AdaptationStatistics {
    private final int totalNodes;
    private final int activeAdaptations;
    private final double averageAdaptationRate;
    private final LocalDateTime timestamp;
    
    public AdaptationStatistics(int totalNodes, int activeAdaptations, 
                              double averageAdaptationRate, LocalDateTime timestamp) {
        this.totalNodes = totalNodes;
        this.activeAdaptations = activeAdaptations;
        this.averageAdaptationRate = averageAdaptationRate;
        this.timestamp = timestamp;
    }
    
    public int getTotalNodes() { return totalNodes; }
    public int getActiveAdaptations() { return activeAdaptations; }
    public double getAverageAdaptationRate() { return averageAdaptationRate; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return "AdaptationStatistics{" +
               "totalNodes=" + totalNodes +
               ", activeAdaptations=" + activeAdaptations +
               ", averageAdaptationRate=" + averageAdaptationRate +
               ", timestamp=" + timestamp +
               '}';
    }
}