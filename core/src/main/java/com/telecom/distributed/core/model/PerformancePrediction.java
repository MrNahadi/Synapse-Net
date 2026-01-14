package com.telecom.distributed.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Performance prediction based on simulation results.
 */
public class PerformancePrediction {
    private final double predictedThroughput;
    private final double predictedLatency;
    private final double throughputImprovement;
    private final double latencyImprovement;
    private final Map<NodeId, ResourceUtilizationPrediction> resourcePredictions;
    private final double throughputConfidence;
    private final double latencyConfidence;
    
    public PerformancePrediction(double predictedThroughput, double predictedLatency,
                               double throughputImprovement, double latencyImprovement,
                               Map<NodeId, ResourceUtilizationPrediction> resourcePredictions,
                               double throughputConfidence, double latencyConfidence) {
        this.predictedThroughput = predictedThroughput;
        this.predictedLatency = predictedLatency;
        this.throughputImprovement = throughputImprovement;
        this.latencyImprovement = latencyImprovement;
        this.resourcePredictions = Objects.requireNonNull(resourcePredictions);
        this.throughputConfidence = throughputConfidence;
        this.latencyConfidence = latencyConfidence;
    }
    
    public double getPredictedThroughput() { return predictedThroughput; }
    public double getPredictedLatency() { return predictedLatency; }
    public double getThroughputImprovement() { return throughputImprovement; }
    public double getLatencyImprovement() { return latencyImprovement; }
    public Map<NodeId, ResourceUtilizationPrediction> getResourcePredictions() { return resourcePredictions; }
    public double getThroughputConfidence() { return throughputConfidence; }
    public double getLatencyConfidence() { return latencyConfidence; }
    
    @Override
    public String toString() {
        return "PerformancePrediction{" +
               "predictedThroughput=" + predictedThroughput +
               ", predictedLatency=" + predictedLatency +
               ", throughputImprovement=" + (throughputImprovement * 100) + "%" +
               ", latencyImprovement=" + (latencyImprovement * 100) + "%" +
               '}';
    }
}