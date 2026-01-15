package com.telecom.distributed.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents an optimization action to be applied to the system.
 */
public class OptimizationAction {
    private final ActionType actionType;
    private final NodeId nodeId;
    private final String description;
    private final double expectedImprovement;
    private boolean applied = false;
    
    // Additional fields for specific action types
    private ServiceId serviceId;
    private NodeId targetNode;
    private int replicationFactor;
    private Map<NodeId, Double> nodeWeights;
    
    public OptimizationAction(ActionType actionType, NodeId nodeId, 
                            String description, double expectedImprovement) {
        this.actionType = Objects.requireNonNull(actionType);
        this.nodeId = Objects.requireNonNull(nodeId);
        this.description = Objects.requireNonNull(description);
        this.expectedImprovement = expectedImprovement;
    }
    
    public OptimizationAction withServiceMigration(ServiceId serviceId, NodeId targetNode) {
        this.serviceId = serviceId;
        this.targetNode = targetNode;
        return this;
    }
    
    public OptimizationAction withReplicationAdjustment(ServiceId serviceId, int replicationFactor) {
        this.serviceId = serviceId;
        this.replicationFactor = replicationFactor;
        return this;
    }
    
    public OptimizationAction withLoadBalancingUpdate(Map<NodeId, Double> nodeWeights) {
        this.nodeWeights = nodeWeights;
        return this;
    }
    
    /**
     * Applies this optimization action to the system.
     */
    public void apply() {
        // Implementation would integrate with actual system components
        // For now, just mark as applied
        this.applied = true;
    }
    
    public ActionType getActionType() { return actionType; }
    public ActionType getType() { return actionType; }
    public NodeId getNodeId() { return nodeId; }
    public String getDescription() { return description; }
    public double getExpectedImprovement() { return expectedImprovement; }
    public boolean isApplied() { return applied; }
    
    public ServiceId getServiceId() { return serviceId; }
    public NodeId getTargetNode() { return targetNode; }
    public int getReplicationFactor() { return replicationFactor; }
    public Map<NodeId, Double> getNodeWeights() { return nodeWeights; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptimizationAction that = (OptimizationAction) o;
        return Double.compare(that.expectedImprovement, expectedImprovement) == 0 &&
               actionType == that.actionType &&
               Objects.equals(nodeId, that.nodeId) &&
               Objects.equals(description, that.description);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(actionType, nodeId, description, expectedImprovement);
    }
    
    @Override
    public String toString() {
        return "OptimizationAction{" +
               "actionType=" + actionType +
               ", nodeId=" + nodeId +
               ", description='" + description + '\'' +
               ", expectedImprovement=" + (expectedImprovement * 100) + "%" +
               ", applied=" + applied +
               '}';
    }
}