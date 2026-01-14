package com.telecom.distributed.core.model;

/**
 * Result of Monte Carlo simulation.
 */
public class MonteCarloResult {
    private final double averageThroughput;
    private final double averageLatency;
    private final ConfidenceInterval throughputConfidenceInterval;
    private final ConfidenceInterval latencyConfidenceInterval;
    
    public MonteCarloResult(double averageThroughput, double averageLatency,
                          ConfidenceInterval throughputCI, ConfidenceInterval latencyCI) {
        this.averageThroughput = averageThroughput;
        this.averageLatency = averageLatency;
        this.throughputConfidenceInterval = throughputCI;
        this.latencyConfidenceInterval = latencyCI;
    }
    
    public double getAverageThroughput() { return averageThroughput; }
    public double getAverageLatency() { return averageLatency; }
    public ConfidenceInterval getThroughputConfidenceInterval() { return throughputConfidenceInterval; }
    public ConfidenceInterval getLatencyConfidenceInterval() { return latencyConfidenceInterval; }
    public ConfidenceInterval getConfidenceInterval() { return throughputConfidenceInterval; }
    
    @Override
    public String toString() {
        return "MonteCarloResult{" +
               "averageThroughput=" + averageThroughput +
               ", averageLatency=" + averageLatency +
               '}';
    }
}