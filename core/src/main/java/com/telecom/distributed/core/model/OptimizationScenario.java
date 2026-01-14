package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.Set;

/**
 * Represents an optimization scenario for simulation and analysis.
 */
public class OptimizationScenario {
    private final String name;
    private final String description;
    private final Set<NodeId> targetNodes;
    private final double expectedThroughputImprovement;
    private final double expectedLatencyImprovement;
    private final Set<OptimizationAction> proposedActions;
    
    public OptimizationScenario(String name, String description, Set<NodeId> targetNodes,
                              double expectedThroughputImprovement, double expectedLatencyImprovement,
                              Set<OptimizationAction> proposedActions) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.targetNodes = Objects.requireNonNull(targetNodes);
        this.expectedThroughputImprovement = expectedThroughputImprovement;
        this.expectedLatencyImprovement = expectedLatencyImprovement;
        this.proposedActions = Objects.requireNonNull(proposedActions);
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Set<NodeId> getTargetNodes() { return targetNodes; }
    public double getExpectedThroughputImprovement() { return expectedThroughputImprovement; }
    public double getExpectedLatencyImprovement() { return expectedLatencyImprovement; }
    public Set<OptimizationAction> getProposedActions() { return proposedActions; }
    
    @Override
    public String toString() {
        return "OptimizationScenario{" +
               "name='" + name + '\'' +
               ", targetNodes=" + targetNodes.size() +
               ", expectedThroughputImprovement=" + (expectedThroughputImprovement * 100) + "%" +
               ", expectedLatencyImprovement=" + (expectedLatencyImprovement * 100) + "%" +
               '}';
    }
}