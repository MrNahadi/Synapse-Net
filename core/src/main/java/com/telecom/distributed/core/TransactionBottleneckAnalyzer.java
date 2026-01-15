package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes transaction bottlenecks using lock contention and resource usage patterns.
 * Identifies nodes causing transaction bottlenecks for distributed commit protocol optimization.
 */
public class TransactionBottleneckAnalyzer {
    
    private static final double LOCK_CONTENTION_THRESHOLD = 10.0; // 10% threshold
    private static final double RESOURCE_USAGE_THRESHOLD = 0.7;   // 70% threshold
    private static final double TRANSACTION_RATE_THRESHOLD = 200.0; // 200 tx/sec threshold
    
    /**
     * Identifies nodes causing transaction bottlenecks using lock contention and resource usage.
     * @param nodeMetrics Current metrics for all nodes
     * @param activeTransactions Currently active transactions
     * @param lockRegistry Current lock registry state
     * @return List of bottleneck nodes ranked by severity
     */
    public List<TransactionBottleneckResult> identifyTransactionBottlenecks(
            Map<NodeId, NodeMetrics> nodeMetrics,
            Map<TransactionId, DistributedTransaction> activeTransactions,
            Map<String, ResourceLock> lockRegistry) {
        
        validateInputs(nodeMetrics, activeTransactions, lockRegistry);
        
        List<TransactionBottleneckResult> results = new ArrayList<>();
        
        for (Map.Entry<NodeId, NodeMetrics> entry : nodeMetrics.entrySet()) {
            NodeId nodeId = entry.getKey();
            NodeMetrics metrics = entry.getValue();
            
            // Calculate bottleneck score based on multiple factors
            double bottleneckScore = calculateTransactionBottleneckScore(
                nodeId, metrics, activeTransactions, lockRegistry);
            
            // Analyze lock contention patterns
            LockContentionAnalysis lockAnalysis = analyzeLockContention(
                nodeId, metrics, activeTransactions, lockRegistry);
            
            // Analyze resource usage patterns
            ResourceUsageAnalysis resourceAnalysis = analyzeResourceUsage(nodeId, metrics);
            
            // Generate bottleneck explanation
            String explanation = generateBottleneckExplanation(
                nodeId, metrics, lockAnalysis, resourceAnalysis, bottleneckScore);
            
            results.add(new TransactionBottleneckResult(
                nodeId, bottleneckScore, lockAnalysis, resourceAnalysis, explanation));
        }
        
        // Sort by bottleneck score (highest first)
        results.sort((a, b) -> Double.compare(b.getBottleneckScore(), a.getBottleneckScore()));
        
        return results;
    }
    
    /**
     * Calculates comprehensive transaction bottleneck score.
     */
    private double calculateTransactionBottleneckScore(
            NodeId nodeId, 
            NodeMetrics metrics,
            Map<TransactionId, DistributedTransaction> activeTransactions,
            Map<String, ResourceLock> lockRegistry) {
        
        // Lock contention factor (0.0 to 1.0)
        double lockContentionFactor = Math.min(1.0, metrics.getLockContention() / 15.0);
        
        // Resource usage factor (CPU + Memory normalized)
        double cpuFactor = Math.min(1.0, (metrics.getCpuUtilization() - 45.0) / (72.0 - 45.0));
        double memoryFactor = Math.min(1.0, (metrics.getMemoryUsage() - 4.0) / (16.0 - 4.0));
        double resourceFactor = (cpuFactor + memoryFactor) / 2.0;
        
        // Transaction rate factor (inverted - high rate can indicate bottleneck)
        double transactionRateFactor = Math.min(1.0, metrics.getTransactionsPerSec() / 300.0);
        
        // Active transaction count factor
        long nodeTransactionCount = activeTransactions.values().stream()
            .filter(tx -> tx.getParticipants().contains(nodeId))
            .count();
        double activeTransactionFactor = Math.min(1.0, nodeTransactionCount / 50.0); // Assume 50 as high
        
        // Lock hold time factor
        double lockHoldTimeFactor = calculateLockHoldTimeFactor(nodeId, lockRegistry);
        
        // Weighted combination
        return 0.3 * lockContentionFactor +
               0.25 * resourceFactor +
               0.2 * transactionRateFactor +
               0.15 * activeTransactionFactor +
               0.1 * lockHoldTimeFactor;
    }
    
    /**
     * Analyzes lock contention patterns for a specific node.
     */
    private LockContentionAnalysis analyzeLockContention(
            NodeId nodeId,
            NodeMetrics metrics,
            Map<TransactionId, DistributedTransaction> activeTransactions,
            Map<String, ResourceLock> lockRegistry) {
        
        // Count locks held by this node
        long locksHeld = lockRegistry.values().stream()
            .filter(lock -> lock.getHolderNodeId().equals(nodeId))
            .count();
        
        // Calculate average lock hold time for this node
        double avgLockHoldTime = lockRegistry.values().stream()
            .filter(lock -> lock.getHolderNodeId().equals(nodeId))
            .mapToLong(ResourceLock::getHoldTime)
            .average()
            .orElse(0.0);
        
        // Count transactions waiting for locks from this node
        long waitingTransactions = activeTransactions.values().stream()
            .filter(tx -> tx.getParticipants().contains(nodeId))
            .filter(tx -> tx.getState() == TransactionState.PREPARING)
            .count();
        
        // Determine contention severity
        ContentionSeverity severity = determineContentionSeverity(
            metrics.getLockContention(), locksHeld, avgLockHoldTime);
        
        return new LockContentionAnalysis(
            metrics.getLockContention(), locksHeld, avgLockHoldTime, waitingTransactions, severity);
    }
    
    /**
     * Analyzes resource usage patterns for transaction processing.
     */
    private ResourceUsageAnalysis analyzeResourceUsage(NodeId nodeId, NodeMetrics metrics) {
        // Normalize resource usage to 0-1 scale
        double normalizedCpu = Math.max(0.0, (metrics.getCpuUtilization() - 45.0) / (72.0 - 45.0));
        double normalizedMemory = Math.max(0.0, (metrics.getMemoryUsage() - 4.0) / (16.0 - 4.0));
        
        // Calculate resource pressure
        double resourcePressure = (normalizedCpu + normalizedMemory) / 2.0;
        
        // Determine if resources are constraining transaction processing
        boolean cpuConstrained = metrics.getCpuUtilization() > 65.0; // 65% threshold
        boolean memoryConstrained = metrics.getMemoryUsage() > 12.0; // 12GB threshold
        
        // Calculate transaction processing efficiency
        double transactionEfficiency = calculateTransactionEfficiency(metrics);
        
        return new ResourceUsageAnalysis(
            normalizedCpu, normalizedMemory, resourcePressure, 
            cpuConstrained, memoryConstrained, transactionEfficiency);
    }
    
    /**
     * Calculates lock hold time factor for bottleneck scoring.
     */
    private double calculateLockHoldTimeFactor(NodeId nodeId, Map<String, ResourceLock> lockRegistry) {
        OptionalDouble avgHoldTime = lockRegistry.values().stream()
            .filter(lock -> lock.getHolderNodeId().equals(nodeId))
            .mapToLong(ResourceLock::getHoldTime)
            .average();
        
        if (avgHoldTime.isEmpty()) {
            return 0.0;
        }
        
        // Normalize against expected hold time (assume 1000ms as baseline)
        return Math.min(1.0, avgHoldTime.getAsDouble() / 5000.0); // 5 seconds as high threshold
    }
    
    /**
     * Determines contention severity based on metrics.
     */
    private ContentionSeverity determineContentionSeverity(
            double lockContention, long locksHeld, double avgLockHoldTime) {
        
        if (lockContention > 12.0 || avgLockHoldTime > 3000.0) {
            return ContentionSeverity.HIGH;
        } else if (lockContention > 8.0 || avgLockHoldTime > 1500.0) {
            return ContentionSeverity.MEDIUM;
        } else {
            return ContentionSeverity.LOW;
        }
    }
    
    /**
     * Calculates transaction processing efficiency.
     */
    private double calculateTransactionEfficiency(NodeMetrics metrics) {
        // Higher transaction rate with lower resource usage = higher efficiency
        double resourceUsage = (metrics.getCpuUtilization() / 72.0 + metrics.getMemoryUsage() / 16.0) / 2.0;
        double transactionRate = metrics.getTransactionsPerSec() / 300.0;
        
        if (resourceUsage == 0.0) {
            return transactionRate;
        }
        
        return transactionRate / resourceUsage;
    }
    
    /**
     * Generates human-readable explanation of bottleneck analysis.
     */
    private String generateBottleneckExplanation(
            NodeId nodeId,
            NodeMetrics metrics,
            LockContentionAnalysis lockAnalysis,
            ResourceUsageAnalysis resourceAnalysis,
            double bottleneckScore) {
        
        StringBuilder explanation = new StringBuilder();
        explanation.append(String.format("Node %s transaction bottleneck analysis (score: %.3f):\n", 
            nodeId.getId(), bottleneckScore));
        
        // Lock contention analysis
        explanation.append(String.format("- Lock contention: %.1f%% (%s severity, %d locks held, %.1fms avg hold time)\n",
            lockAnalysis.getLockContentionPercentage(),
            lockAnalysis.getSeverity().name().toLowerCase(),
            lockAnalysis.getLocksHeld(),
            lockAnalysis.getAvgLockHoldTime()));
        
        // Resource usage analysis
        explanation.append(String.format("- Resource usage: CPU %.1f%% (%s), Memory %.1fGB (%s)\n",
            metrics.getCpuUtilization(),
            resourceAnalysis.isCpuConstrained() ? "constrained" : "normal",
            metrics.getMemoryUsage(),
            resourceAnalysis.isMemoryConstrained() ? "constrained" : "normal"));
        
        // Transaction processing
        explanation.append(String.format("- Transaction processing: %d tx/sec (efficiency: %.2f)\n",
            metrics.getTransactionsPerSec(),
            resourceAnalysis.getTransactionEfficiency()));
        
        return explanation.toString();
    }
    
    /**
     * Validates input parameters.
     */
    private void validateInputs(
            Map<NodeId, NodeMetrics> nodeMetrics,
            Map<TransactionId, DistributedTransaction> activeTransactions,
            Map<String, ResourceLock> lockRegistry) {
        
        Objects.requireNonNull(nodeMetrics, "Node metrics cannot be null");
        Objects.requireNonNull(activeTransactions, "Active transactions cannot be null");
        Objects.requireNonNull(lockRegistry, "Lock registry cannot be null");
        
        if (nodeMetrics.isEmpty()) {
            throw new IllegalArgumentException("Node metrics cannot be empty");
        }
    }
    
    // Inner classes for analysis results
    public static class TransactionBottleneckResult {
        private final NodeId nodeId;
        private final double bottleneckScore;
        private final LockContentionAnalysis lockAnalysis;
        private final ResourceUsageAnalysis resourceAnalysis;
        private final String explanation;
        
        public TransactionBottleneckResult(NodeId nodeId, double bottleneckScore,
                                         LockContentionAnalysis lockAnalysis,
                                         ResourceUsageAnalysis resourceAnalysis,
                                         String explanation) {
            this.nodeId = nodeId;
            this.bottleneckScore = bottleneckScore;
            this.lockAnalysis = lockAnalysis;
            this.resourceAnalysis = resourceAnalysis;
            this.explanation = explanation;
        }
        
        // Getters
        public NodeId getNodeId() { return nodeId; }
        public double getBottleneckScore() { return bottleneckScore; }
        public LockContentionAnalysis getLockAnalysis() { return lockAnalysis; }
        public ResourceUsageAnalysis getResourceAnalysis() { return resourceAnalysis; }
        public String getExplanation() { return explanation; }
    }
    
    public static class LockContentionAnalysis {
        private final double lockContentionPercentage;
        private final long locksHeld;
        private final double avgLockHoldTime;
        private final long waitingTransactions;
        private final ContentionSeverity severity;
        
        public LockContentionAnalysis(double lockContentionPercentage, long locksHeld,
                                    double avgLockHoldTime, long waitingTransactions,
                                    ContentionSeverity severity) {
            this.lockContentionPercentage = lockContentionPercentage;
            this.locksHeld = locksHeld;
            this.avgLockHoldTime = avgLockHoldTime;
            this.waitingTransactions = waitingTransactions;
            this.severity = severity;
        }
        
        // Getters
        public double getLockContentionPercentage() { return lockContentionPercentage; }
        public long getLocksHeld() { return locksHeld; }
        public double getAvgLockHoldTime() { return avgLockHoldTime; }
        public long getWaitingTransactions() { return waitingTransactions; }
        public ContentionSeverity getSeverity() { return severity; }
    }
    
    public static class ResourceUsageAnalysis {
        private final double normalizedCpu;
        private final double normalizedMemory;
        private final double resourcePressure;
        private final boolean cpuConstrained;
        private final boolean memoryConstrained;
        private final double transactionEfficiency;
        
        public ResourceUsageAnalysis(double normalizedCpu, double normalizedMemory,
                                   double resourcePressure, boolean cpuConstrained,
                                   boolean memoryConstrained, double transactionEfficiency) {
            this.normalizedCpu = normalizedCpu;
            this.normalizedMemory = normalizedMemory;
            this.resourcePressure = resourcePressure;
            this.cpuConstrained = cpuConstrained;
            this.memoryConstrained = memoryConstrained;
            this.transactionEfficiency = transactionEfficiency;
        }
        
        // Getters
        public double getNormalizedCpu() { return normalizedCpu; }
        public double getNormalizedMemory() { return normalizedMemory; }
        public double getResourcePressure() { return resourcePressure; }
        public boolean isCpuConstrained() { return cpuConstrained; }
        public boolean isMemoryConstrained() { return memoryConstrained; }
        public double getTransactionEfficiency() { return transactionEfficiency; }
    }
    
    public enum ContentionSeverity {
        LOW, MEDIUM, HIGH
    }
}