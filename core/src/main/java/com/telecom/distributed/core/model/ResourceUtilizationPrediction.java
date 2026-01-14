package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Prediction of resource utilization for a node.
 */
public class ResourceUtilizationPrediction {
    private final NodeId nodeId;
    private final double predictedCpuUtilization;
    private final double predictedMemoryUsage;
    
    public ResourceUtilizationPrediction(NodeId nodeId, double predictedCpuUtilization, double predictedMemoryUsage) {
        this.nodeId = Objects.requireNonNull(nodeId);
        this.predictedCpuUtilization = predictedCpuUtilization;
        this.predictedMemoryUsage = predictedMemoryUsage;
    }
    
    public NodeId getNodeId() { return nodeId; }
    public double getPredictedCpuUtilization() { return predictedCpuUtilization; }
    public double getPredictedMemoryUsage() { return predictedMemoryUsage; }
    
    @Override
    public String toString() {
        return "ResourceUtilizationPrediction{" +
               "nodeId=" + nodeId +
               ", predictedCpuUtilization=" + predictedCpuUtilization + "%" +
               ", predictedMemoryUsage=" + predictedMemoryUsage + "GB" +
               '}';
    }
}