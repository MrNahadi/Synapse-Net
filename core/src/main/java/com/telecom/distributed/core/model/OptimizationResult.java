package com.telecom.distributed.core.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of system optimization operations.
 */
public class OptimizationResult {
    private final double expectedThroughputImprovement;
    private final Map<NodeId, ResourceConstraintResult> constraintResults;
    private final List<OptimizationAction> appliedOptimizations;
    private final OptimizationStatus status;
    
    public OptimizationResult(double expectedThroughputImprovement,
                            Map<NodeId, ResourceConstraintResult> constraintResults,
                            List<OptimizationAction> appliedOptimizations) {
        this(expectedThroughputImprovement, constraintResults, appliedOptimizations, OptimizationStatus.SUCCESS);
    }
    
    public OptimizationResult(double expectedThroughputImprovement,
                            Map<NodeId, ResourceConstraintResult> constraintResults,
                            List<OptimizationAction> appliedOptimizations,
                            OptimizationStatus status) {
        this.expectedThroughputImprovement = expectedThroughputImprovement;
        this.constraintResults = Objects.requireNonNull(constraintResults);
        this.appliedOptimizations = Objects.requireNonNull(appliedOptimizations);
        this.status = Objects.requireNonNull(status);
    }
    
    public double getExpectedThroughputImprovement() { return expectedThroughputImprovement; }
    public Map<NodeId, ResourceConstraintResult> getConstraintResults() { return constraintResults; }
    public List<OptimizationAction> getAppliedOptimizations() { return appliedOptimizations; }
    public List<OptimizationAction> getActions() { return appliedOptimizations; }
    public OptimizationStatus getStatus() { return status; }
    
    @Override
    public String toString() {
        return "OptimizationResult{" +
               "expectedThroughputImprovement=" + (expectedThroughputImprovement * 100) + "%" +
               ", constraintResults=" + constraintResults.size() + " nodes" +
               ", appliedOptimizations=" + appliedOptimizations.size() + " actions" +
               '}';
    }
}