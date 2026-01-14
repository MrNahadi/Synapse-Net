package com.telecom.distributed.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Result of throughput optimization operations.
 */
public class ThroughputOptimizationResult {
    private final double currentThroughput;
    private final double expectedImprovement;
    private final List<OptimizationAction> optimizations;
    private final List<OptimizationOpportunity> opportunities;
    
    public ThroughputOptimizationResult(double currentThroughput,
                                      double expectedImprovement,
                                      List<OptimizationAction> optimizations,
                                      List<OptimizationOpportunity> opportunities) {
        this.currentThroughput = currentThroughput;
        this.expectedImprovement = expectedImprovement;
        this.optimizations = Objects.requireNonNull(optimizations);
        this.opportunities = Objects.requireNonNull(opportunities);
    }
    
    public double getCurrentThroughput() { return currentThroughput; }
    public double getExpectedImprovement() { return expectedImprovement; }
    public double getExpectedThroughputImprovement() { return expectedImprovement; }
    public List<OptimizationAction> getOptimizations() { return optimizations; }
    public List<OptimizationOpportunity> getOpportunities() { return opportunities; }
    
    @Override
    public String toString() {
        return "ThroughputOptimizationResult{" +
               "currentThroughput=" + currentThroughput + "Mbps" +
               ", expectedImprovement=" + (expectedImprovement * 100) + "%" +
               ", optimizations=" + optimizations.size() +
               ", opportunities=" + opportunities.size() +
               '}';
    }
}