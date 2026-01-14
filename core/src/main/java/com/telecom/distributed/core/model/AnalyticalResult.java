package com.telecom.distributed.core.model;

/**
 * Result of analytical model analysis.
 */
public class AnalyticalResult {
    private final double predictedThroughput;
    private final double predictedLatency;
    
    public AnalyticalResult(double predictedThroughput, double predictedLatency) {
        this.predictedThroughput = predictedThroughput;
        this.predictedLatency = predictedLatency;
    }
    
    public double getPredictedThroughput() { return predictedThroughput; }
    public double getPredictedLatency() { return predictedLatency; }
    
    @Override
    public String toString() {
        return "AnalyticalResult{" +
               "predictedThroughput=" + predictedThroughput +
               ", predictedLatency=" + predictedLatency +
               '}';
    }
}