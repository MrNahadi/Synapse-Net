package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Throughput optimization algorithms for maximizing system performance
 * while respecting resource constraints.
 */
public class ThroughputOptimizer {
    
    private static final double THROUGHPUT_IMPROVEMENT_THRESHOLD = 0.05; // 5% minimum improvement
    
    /**
     * Maximizes system throughput within resource constraints.
     * @param nodeMetrics Current node metrics
     * @param constraintResults Resource constraint enforcement results
     * @return Throughput optimization result
     */
    public ThroughputOptimizationResult maximizeThroughput(
            Map<NodeId, NodeMetrics> nodeMetrics,
            Map<NodeId, ResourceConstraintResult> constraintResults) {
        
        // Calculate current total throughput
        double currentThroughput = calculateTotalThroughput(nodeMetrics);
        
        // Identify optimization opportunities
        List<OptimizationOpportunity> opportunities = 
            identifyOptimizationOpportunities(nodeMetrics, constraintResults);
        
        // Generate optimization actions
        List<OptimizationAction> optimizations = 
            generateOptimizationActions(opportunities, nodeMetrics);
        
        // Estimate throughput improvement
        double expectedImprovement = estimateThroughputImprovement(optimizations, nodeMetrics);
        
        return new ThroughputOptimizationResult(
            currentThroughput,
            expectedImprovement,
            optimizations,
            opportunities
        );
    }
    
    /**
     * Identifies nodes with throughput optimization potential.
     * @param nodeMetrics Current metrics
     * @param constraintResults Constraint enforcement results
     * @return List of optimization opportunities
     */
    private List<OptimizationOpportunity> identifyOptimizationOpportunities(
            Map<NodeId, NodeMetrics> nodeMetrics,
            Map<NodeId, ResourceConstraintResult> constraintResults) {
        
        List<OptimizationOpportunity> opportunities = new ArrayList<>();
        
        for (Map.Entry<NodeId, NodeMetrics> entry : nodeMetrics.entrySet()) {
            NodeId nodeId = entry.getKey();
            NodeMetrics metrics = entry.getValue();
            ResourceConstraintResult constraintResult = constraintResults.get(nodeId);
            
            // Check for CPU underutilization
            if (metrics.getCpuUtilization() < 60.0 && constraintResult.isCompliant()) {
                opportunities.add(new OptimizationOpportunity(
                    nodeId,
                    OpportunityType.CPU_UNDERUTILIZATION,
                    "CPU utilization is " + metrics.getCpuUtilization() + "%, can handle more load",
                    calculateCpuOptimizationPotential(metrics)
                ));
            }
            
            // Check for memory underutilization
            if (metrics.getMemoryUsage() < 12.0 && constraintResult.isCompliant()) {
                opportunities.add(new OptimizationOpportunity(
                    nodeId,
                    OpportunityType.MEMORY_UNDERUTILIZATION,
                    "Memory usage is " + metrics.getMemoryUsage() + "GB, can cache more data",
                    calculateMemoryOptimizationPotential(metrics)
                ));
            }
            
            // Check for throughput bottlenecks
            if (metrics.getThroughput() < 800.0) {
                opportunities.add(new OptimizationOpportunity(
                    nodeId,
                    OpportunityType.THROUGHPUT_BOTTLENECK,
                    "Throughput is " + metrics.getThroughput() + "Mbps, below optimal range",
                    calculateThroughputOptimizationPotential(metrics)
                ));
            }
            
            // Check for lock contention optimization
            if (metrics.getLockContention() > 10.0) {
                opportunities.add(new OptimizationOpportunity(
                    nodeId,
                    OpportunityType.LOCK_CONTENTION,
                    "Lock contention is " + metrics.getLockContention() + "%, reducing throughput",
                    calculateLockOptimizationPotential(metrics)
                ));
            }
        }
        
        // Sort opportunities by potential impact
        opportunities.sort((o1, o2) -> Double.compare(o2.getPotentialImprovement(), o1.getPotentialImprovement()));
        
        return opportunities;
    }
    
    /**
     * Generates optimization actions based on identified opportunities.
     * @param opportunities Optimization opportunities
     * @param nodeMetrics Current node metrics
     * @return List of optimization actions
     */
    private List<OptimizationAction> generateOptimizationActions(
            List<OptimizationOpportunity> opportunities,
            Map<NodeId, NodeMetrics> nodeMetrics) {
        
        List<OptimizationAction> actions = new ArrayList<>();
        
        for (OptimizationOpportunity opportunity : opportunities) {
            switch (opportunity.getType()) {
                case CPU_UNDERUTILIZATION:
                    actions.add(new OptimizationAction(
                        ActionType.INCREASE_WORKLOAD,
                        opportunity.getNodeId(),
                        "Increase workload allocation to utilize available CPU capacity",
                        opportunity.getPotentialImprovement()
                    ));
                    break;
                    
                case MEMORY_UNDERUTILIZATION:
                    actions.add(new OptimizationAction(
                        ActionType.OPTIMIZE_MEMORY_ALLOCATION,
                        opportunity.getNodeId(),
                        "Implement aggressive caching to utilize available memory",
                        opportunity.getPotentialImprovement()
                    ));
                    break;
                    
                case THROUGHPUT_BOTTLENECK:
                    actions.add(new OptimizationAction(
                        ActionType.OPTIMIZE_NETWORK,
                        opportunity.getNodeId(),
                        "Optimize network configuration and connection pooling",
                        opportunity.getPotentialImprovement()
                    ));
                    break;
                    
                case LOCK_CONTENTION:
                    actions.add(new OptimizationAction(
                        ActionType.REDUCE_LOCK_CONTENTION,
                        opportunity.getNodeId(),
                        "Implement lock-free data structures and reduce lock granularity",
                        opportunity.getPotentialImprovement()
                    ));
                    break;
            }
        }
        
        return actions;
    }
    
    /**
     * Estimates total throughput improvement from optimization actions.
     * @param optimizations List of optimization actions
     * @param nodeMetrics Current node metrics
     * @return Expected throughput improvement percentage
     */
    private double estimateThroughputImprovement(
            List<OptimizationAction> optimizations,
            Map<NodeId, NodeMetrics> nodeMetrics) {
        
        double totalImprovement = 0.0;
        Map<NodeId, Double> nodeImprovements = new HashMap<>();
        
        for (OptimizationAction action : optimizations) {
            NodeId nodeId = action.getNodeId();
            double improvement = action.getExpectedImprovement();
            
            // Accumulate improvements per node (with diminishing returns)
            double currentImprovement = nodeImprovements.getOrDefault(nodeId, 0.0);
            double combinedImprovement = currentImprovement + improvement * (1.0 - currentImprovement);
            nodeImprovements.put(nodeId, combinedImprovement);
        }
        
        // Calculate weighted average improvement based on node throughput
        double totalThroughput = calculateTotalThroughput(nodeMetrics);
        
        for (Map.Entry<NodeId, Double> entry : nodeImprovements.entrySet()) {
            NodeId nodeId = entry.getKey();
            double nodeImprovement = entry.getValue();
            NodeMetrics metrics = nodeMetrics.get(nodeId);
            
            if (metrics != null) {
                double nodeWeight = metrics.getThroughput() / totalThroughput;
                totalImprovement += nodeImprovement * nodeWeight;
            }
        }
        
        return totalImprovement;
    }
    
    private double calculateTotalThroughput(Map<NodeId, NodeMetrics> nodeMetrics) {
        return nodeMetrics.values().stream()
            .mapToDouble(NodeMetrics::getThroughput)
            .sum();
    }
    
    private double calculateCpuOptimizationPotential(NodeMetrics metrics) {
        double currentUtilization = metrics.getCpuUtilization();
        double maxUtilization = 72.0;
        return (maxUtilization - currentUtilization) / maxUtilization * 0.3; // 30% max improvement
    }
    
    private double calculateMemoryOptimizationPotential(NodeMetrics metrics) {
        double currentUsage = metrics.getMemoryUsage();
        double maxUsage = 16.0;
        return (maxUsage - currentUsage) / maxUsage * 0.2; // 20% max improvement
    }
    
    private double calculateThroughputOptimizationPotential(NodeMetrics metrics) {
        double currentThroughput = metrics.getThroughput();
        double maxThroughput = 1250.0;
        return (maxThroughput - currentThroughput) / maxThroughput * 0.4; // 40% max improvement
    }
    
    private double calculateLockOptimizationPotential(NodeMetrics metrics) {
        double lockContention = metrics.getLockContention();
        return Math.min(lockContention / 15.0 * 0.25, 0.25); // Up to 25% improvement
    }
}