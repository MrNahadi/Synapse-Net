package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enforces CPU and memory constraints (45-72%, 4.0-16.0GB) across the distributed system.
 * Implements constraint validation and enforcement mechanisms.
 */
public class ResourceConstraintEnforcer {
    
    private static final double MIN_CPU_UTILIZATION = 45.0;
    private static final double MAX_CPU_UTILIZATION = 72.0;
    private static final double MIN_MEMORY_USAGE = 4.0;
    private static final double MAX_MEMORY_USAGE = 16.0;
    
    /**
     * Enforces resource constraints across all specified nodes.
     * @param nodeMetrics Current metrics for each node
     * @param nodeConfigurations Node configurations with limits
     * @return Constraint enforcement results
     */
    public Map<NodeId, ResourceConstraintResult> enforceConstraints(
            Map<NodeId, NodeMetrics> nodeMetrics,
            Map<NodeId, NodeConfiguration> nodeConfigurations) {
        
        Map<NodeId, ResourceConstraintResult> results = new HashMap<>();
        
        for (Map.Entry<NodeId, NodeMetrics> entry : nodeMetrics.entrySet()) {
            NodeId nodeId = entry.getKey();
            NodeMetrics metrics = entry.getValue();
            NodeConfiguration config = nodeConfigurations.get(nodeId);
            
            if (config != null) {
                ResourceConstraintResult result = enforceNodeConstraints(nodeId, metrics, config);
                results.put(nodeId, result);
            }
        }
        
        return results;
    }
    
    /**
     * Enforces constraints for a single node.
     * @param nodeId Node identifier
     * @param metrics Current node metrics
     * @param config Node configuration
     * @return Constraint enforcement result
     */
    public ResourceConstraintResult enforceNodeConstraints(NodeId nodeId, 
                                                         NodeMetrics metrics, 
                                                         NodeConfiguration config) {
        List<ConstraintViolation> violations = new ArrayList<>();
        List<ConstraintAction> actions = new ArrayList<>();
        
        // Check CPU utilization constraints
        double cpuUtilization = metrics.getCpuUtilization();
        if (cpuUtilization < MIN_CPU_UTILIZATION) {
            violations.add(new ConstraintViolation(
                ConstraintType.CPU_UNDERUTILIZATION,
                "CPU utilization " + cpuUtilization + "% is below minimum " + MIN_CPU_UTILIZATION + "%"
            ));
            actions.add(new ConstraintAction(
                ActionType.INCREASE_WORKLOAD,
                "Increase workload to utilize CPU capacity"
            ));
        } else if (cpuUtilization > MAX_CPU_UTILIZATION) {
            violations.add(new ConstraintViolation(
                ConstraintType.CPU_OVERUTILIZATION,
                "CPU utilization " + cpuUtilization + "% exceeds maximum " + MAX_CPU_UTILIZATION + "%"
            ));
            actions.add(new ConstraintAction(
                ActionType.REDUCE_WORKLOAD,
                "Reduce workload or migrate tasks to other nodes"
            ));
        }
        
        // Check memory usage constraints
        double memoryUsage = metrics.getMemoryUsage();
        if (memoryUsage < MIN_MEMORY_USAGE) {
            violations.add(new ConstraintViolation(
                ConstraintType.MEMORY_UNDERUTILIZATION,
                "Memory usage " + memoryUsage + "GB is below minimum " + MIN_MEMORY_USAGE + "GB"
            ));
            actions.add(new ConstraintAction(
                ActionType.OPTIMIZE_MEMORY_ALLOCATION,
                "Optimize memory allocation to use available capacity"
            ));
        } else if (memoryUsage > MAX_MEMORY_USAGE) {
            violations.add(new ConstraintViolation(
                ConstraintType.MEMORY_OVERUTILIZATION,
                "Memory usage " + memoryUsage + "GB exceeds maximum " + MAX_MEMORY_USAGE + "GB"
            ));
            actions.add(new ConstraintAction(
                ActionType.REDUCE_MEMORY_USAGE,
                "Reduce memory usage or migrate memory-intensive tasks"
            ));
        }
        
        // Check transaction rate constraints
        int transactionRate = metrics.getTransactionsPerSec();
        int maxTransactions = config.getResourceLimits().getMaxConcurrentTransactions();
        if (transactionRate > maxTransactions) {
            violations.add(new ConstraintViolation(
                ConstraintType.TRANSACTION_OVERLOAD,
                "Transaction rate " + transactionRate + " exceeds limit " + maxTransactions
            ));
            actions.add(new ConstraintAction(
                ActionType.THROTTLE_TRANSACTIONS,
                "Throttle transaction rate or distribute load"
            ));
        }
        
        boolean compliant = violations.isEmpty();
        return new ResourceConstraintResult(nodeId, compliant, violations, actions);
    }
    
    /**
     * Validates if metrics are within acceptable constraint ranges.
     * @param metrics Node metrics to validate
     * @return True if all constraints are satisfied
     */
    public boolean validateConstraints(NodeMetrics metrics) {
        double cpuUtilization = metrics.getCpuUtilization();
        double memoryUsage = metrics.getMemoryUsage();
        
        return cpuUtilization >= MIN_CPU_UTILIZATION && 
               cpuUtilization <= MAX_CPU_UTILIZATION &&
               memoryUsage >= MIN_MEMORY_USAGE && 
               memoryUsage <= MAX_MEMORY_USAGE;
    }
    
    /**
     * Calculates constraint compliance score (0.0 to 1.0).
     * @param metrics Node metrics
     * @return Compliance score
     */
    public double calculateComplianceScore(NodeMetrics metrics) {
        double cpuScore = calculateCpuComplianceScore(metrics.getCpuUtilization());
        double memoryScore = calculateMemoryComplianceScore(metrics.getMemoryUsage());
        
        return (cpuScore + memoryScore) / 2.0;
    }
    
    private double calculateCpuComplianceScore(double cpuUtilization) {
        if (cpuUtilization < MIN_CPU_UTILIZATION) {
            return Math.max(0.0, cpuUtilization / MIN_CPU_UTILIZATION);
        } else if (cpuUtilization > MAX_CPU_UTILIZATION) {
            return Math.max(0.0, (100.0 - cpuUtilization) / (100.0 - MAX_CPU_UTILIZATION));
        } else {
            return 1.0; // Within optimal range
        }
    }
    
    private double calculateMemoryComplianceScore(double memoryUsage) {
        if (memoryUsage < MIN_MEMORY_USAGE) {
            return Math.max(0.0, memoryUsage / MIN_MEMORY_USAGE);
        } else if (memoryUsage > MAX_MEMORY_USAGE) {
            return Math.max(0.0, (20.0 - memoryUsage) / (20.0 - MAX_MEMORY_USAGE));
        } else {
            return 1.0; // Within optimal range
        }
    }
}