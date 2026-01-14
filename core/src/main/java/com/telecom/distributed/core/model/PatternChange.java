package com.telecom.distributed.core.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a detected change in system patterns.
 */
public class PatternChange {
    private final NodeId nodeId;
    private final PatternChangeType type;
    private final String description;
    private final double magnitude;
    private final LocalDateTime timestamp;
    
    public PatternChange(NodeId nodeId, PatternChangeType type, String description, 
                        double magnitude, LocalDateTime timestamp) {
        this.nodeId = Objects.requireNonNull(nodeId);
        this.type = Objects.requireNonNull(type);
        this.description = Objects.requireNonNull(description);
        this.magnitude = magnitude;
        this.timestamp = Objects.requireNonNull(timestamp);
    }
    
    public NodeId getNodeId() { return nodeId; }
    public PatternChangeType getType() { return type; }
    public String getDescription() { return description; }
    public double getMagnitude() { return magnitude; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return "PatternChange{" +
               "nodeId=" + nodeId +
               ", type=" + type +
               ", description='" + description + '\'' +
               ", magnitude=" + magnitude +
               ", timestamp=" + timestamp +
               '}';
    }
}