package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Represents an optimization opportunity identified in the system.
 */
public class OptimizationOpportunity {
    private final NodeId nodeId;
    private final OpportunityType type;
    private final String description;
    private final double potentialImprovement;
    
    public OptimizationOpportunity(NodeId nodeId, OpportunityType type,
                                 String description, double potentialImprovement) {
        this.nodeId = Objects.requireNonNull(nodeId);
        this.type = Objects.requireNonNull(type);
        this.description = Objects.requireNonNull(description);
        this.potentialImprovement = potentialImprovement;
    }
    
    public NodeId getNodeId() { return nodeId; }
    public OpportunityType getType() { return type; }
    public String getDescription() { return description; }
    public double getPotentialImprovement() { return potentialImprovement; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptimizationOpportunity that = (OptimizationOpportunity) o;
        return Double.compare(that.potentialImprovement, potentialImprovement) == 0 &&
               Objects.equals(nodeId, that.nodeId) &&
               type == that.type &&
               Objects.equals(description, that.description);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(nodeId, type, description, potentialImprovement);
    }
    
    @Override
    public String toString() {
        return "OptimizationOpportunity{" +
               "nodeId=" + nodeId +
               ", type=" + type +
               ", description='" + description + '\'' +
               ", potentialImprovement=" + (potentialImprovement * 100) + "%" +
               '}';
    }
}