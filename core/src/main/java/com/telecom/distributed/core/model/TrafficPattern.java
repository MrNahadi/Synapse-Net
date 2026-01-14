package com.telecom.distributed.core.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Represents current traffic patterns across nodes.
 */
public class TrafficPattern {
    private final Map<NodeId, Double> nodeTrafficRates; // Mbps
    private final LocalDateTime timestamp;
    private final PatternChangeType patternType;
    private final double intensity; // 0-100 scale
    
    public TrafficPattern(Map<NodeId, Double> nodeTrafficRates, PatternChangeType patternType, double intensity) {
        this.nodeTrafficRates = Objects.requireNonNull(nodeTrafficRates);
        this.patternType = Objects.requireNonNull(patternType);
        this.intensity = validateIntensity(intensity);
        this.timestamp = LocalDateTime.now();
    }
    
    private double validateIntensity(double intensity) {
        if (intensity < 0.0 || intensity > 100.0) {
            throw new IllegalArgumentException("Intensity must be between 0-100, got: " + intensity);
        }
        return intensity;
    }
    
    public Map<NodeId, Double> getNodeTrafficRates() { return nodeTrafficRates; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public PatternChangeType getPatternType() { return patternType; }
    public double getIntensity() { return intensity; }
    
    public double getTotalTrafficRate() {
        return nodeTrafficRates.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    
    @Override
    public String toString() {
        return "TrafficPattern{" +
               "nodeTrafficRates=" + nodeTrafficRates.size() + " nodes" +
               ", timestamp=" + timestamp +
               ", patternType=" + patternType +
               ", intensity=" + intensity +
               ", totalRate=" + getTotalTrafficRate() + "Mbps" +
               '}';
    }
}