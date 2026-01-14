package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Performance analyzer with mathematical ranking algorithms for bottleneck identification.
 * Implements comprehensive metric analysis and non-linear interaction analysis.
 */
public class PerformanceAnalyzer {
    
    private static final double LATENCY_WEIGHT = 0.25;
    private static final double THROUGHPUT_WEIGHT = 0.20;
    private static final double PACKET_LOSS_WEIGHT = 0.15;
    private static final double CPU_WEIGHT = 0.15;
    private static final double MEMORY_WEIGHT = 0.10;
    private static final double TRANSACTION_WEIGHT = 0.10;
    private static final double LOCK_CONTENTION_WEIGHT = 0.05;
    
    /**
     * Analyzes performance bottlenecks across all nodes using mathematical ranking.
     * @param nodeMetrics Map of node metrics for all nodes
     * @return Ranked list of bottleneck analyses
     */
    public List<BottleneckAnalysis> analyzeBottlenecks(Map<NodeId, NodeMetrics> nodeMetrics) {
        validateAllNodesPresent(nodeMetrics);
        
        List<BottleneckAnalysis> analyses = new ArrayList<>();
        
        for (Map.Entry<NodeId, NodeMetrics> entry : nodeMetrics.entrySet()) {
            NodeId nodeId = entry.getKey();
            NodeMetrics metrics = entry.getValue();
            
            double bottleneckScore = calculateBottleneckScore(metrics, nodeMetrics);
            BottleneckType type = identifyBottleneckType(metrics);
            String description = generateBottleneckDescription(nodeId, metrics, type);
            Set<OptimizationSuggestion> suggestions = generateOptimizationSuggestions(metrics, type);
            
            analyses.add(new BottleneckAnalysis(nodeId, type, bottleneckScore, description, suggestions));
        }
        
        // Sort by bottleneck score (highest first)
        analyses.sort((a, b) -> Double.compare(b.getSeverityScore(), a.getSeverityScore()));
        
        return analyses;
    }
    
    /**
     * Calculates comprehensive bottleneck score using all performance metrics.
     * Includes non-linear interaction analysis between performance factors.
     */
    private double calculateBottleneckScore(NodeMetrics metrics, Map<NodeId, NodeMetrics> allMetrics) {
        // Normalize metrics to 0-1 scale based on dataset ranges
        double normalizedLatency = normalizeLatency(metrics.getLatency());
        double normalizedThroughput = 1.0 - normalizeThroughput(metrics.getThroughput()); // Inverted (lower is worse)
        double normalizedPacketLoss = normalizePacketLoss(metrics.getPacketLoss());
        double normalizedCpu = normalizeCpuUtilization(metrics.getCpuUtilization());
        double normalizedMemory = normalizeMemoryUsage(metrics.getMemoryUsage());
        double normalizedTransactions = 1.0 - normalizeTransactionsPerSec(metrics.getTransactionsPerSec()); // Inverted
        double normalizedLockContention = normalizeLockContention(metrics.getLockContention());
        
        // Linear component
        double linearScore = LATENCY_WEIGHT * normalizedLatency +
                           THROUGHPUT_WEIGHT * normalizedThroughput +
                           PACKET_LOSS_WEIGHT * normalizedPacketLoss +
                           CPU_WEIGHT * normalizedCpu +
                           MEMORY_WEIGHT * normalizedMemory +
                           TRANSACTION_WEIGHT * normalizedTransactions +
                           LOCK_CONTENTION_WEIGHT * normalizedLockContention;
        
        // Non-linear interaction analysis
        double nonLinearComponent = calculateNonLinearInteractions(metrics);
        
        // Conditional failure probability adjustment
        double failureProbabilityAdjustment = calculateFailureProbabilityAdjustment(metrics, allMetrics);
        
        return Math.min(1.0, linearScore + nonLinearComponent + failureProbabilityAdjustment);
    }
    
    /**
     * Calculates non-linear interactions between performance factors.
     */
    private double calculateNonLinearInteractions(NodeMetrics metrics) {
        double cpuNorm = normalizeCpuUtilization(metrics.getCpuUtilization());
        double memoryNorm = normalizeMemoryUsage(metrics.getMemoryUsage());
        double lockNorm = normalizeLockContention(metrics.getLockContention());
        double latencyNorm = normalizeLatency(metrics.getLatency());
        
        // CPU-Memory interaction (high CPU + high memory = exponential degradation)
        double cpuMemoryInteraction = 0.1 * cpuNorm * memoryNorm * Math.exp(cpuNorm + memoryNorm - 1.0);
        
        // Lock contention amplifies latency issues
        double lockLatencyInteraction = 0.05 * lockNorm * latencyNorm * (1.0 + lockNorm);
        
        // Resource saturation threshold effect
        double saturationEffect = 0.0;
        if (cpuNorm > 0.8 && memoryNorm > 0.8) {
            saturationEffect = 0.15 * Math.pow(cpuNorm * memoryNorm, 2);
        }
        
        return cpuMemoryInteraction + lockLatencyInteraction + saturationEffect;
    }
    
    /**
     * Calculates failure probability adjustment based on conditional probabilities.
     */
    private double calculateFailureProbabilityAdjustment(NodeMetrics metrics, Map<NodeId, NodeMetrics> allMetrics) {
        // Higher resource utilization increases failure probability
        double resourceStress = (normalizeCpuUtilization(metrics.getCpuUtilization()) + 
                               normalizeMemoryUsage(metrics.getMemoryUsage())) / 2.0;
        
        // Network stress from packet loss and latency
        double networkStress = (normalizePacketLoss(metrics.getPacketLoss()) + 
                              normalizeLatency(metrics.getLatency())) / 2.0;
        
        // Transaction stress from lock contention and low throughput
        double transactionStress = (normalizeLockContention(metrics.getLockContention()) + 
                                  (1.0 - normalizeTransactionsPerSec(metrics.getTransactionsPerSec()))) / 2.0;
        
        // Conditional probability: P(failure | high_stress) = base_prob * stress_multiplier
        double stressMultiplier = 1.0 + resourceStress + networkStress + transactionStress;
        
        return 0.05 * stressMultiplier; // Base adjustment factor
    }
    
    /**
     * Identifies the highest latency contributor with conditional failure probability.
     */
    public LatencyContributorAnalysis identifyLatencyContributor(Map<NodeId, NodeMetrics> nodeMetrics) {
        validateAllNodesPresent(nodeMetrics);
        
        NodeId highestLatencyNode = null;
        double highestLatency = 0.0;
        
        for (Map.Entry<NodeId, NodeMetrics> entry : nodeMetrics.entrySet()) {
            double latency = entry.getValue().getLatency();
            if (latency > highestLatency) {
                highestLatency = latency;
                highestLatencyNode = entry.getKey();
            }
        }
        
        NodeMetrics contributorMetrics = nodeMetrics.get(highestLatencyNode);
        double conditionalFailureProbability = calculateConditionalFailureProbability(contributorMetrics, nodeMetrics);
        String justification = generateLatencyJustification(highestLatencyNode, contributorMetrics, conditionalFailureProbability);
        
        return new LatencyContributorAnalysis(highestLatencyNode, highestLatency, 
                                            conditionalFailureProbability, justification);
    }
    
    /**
     * Calculates conditional failure probability for latency contributor.
     */
    private double calculateConditionalFailureProbability(NodeMetrics contributorMetrics, Map<NodeId, NodeMetrics> allMetrics) {
        // Base failure probability
        double baseProbability = 0.01; // 1% base failure rate
        
        // Resource utilization factor
        double resourceFactor = (contributorMetrics.getCpuUtilization() / 72.0 + 
                               contributorMetrics.getMemoryUsage() / 16.0) / 2.0;
        
        // Transaction load factor
        double transactionFactor = contributorMetrics.getTransactionsPerSec() / 300.0;
        
        // Lock contention factor
        double lockFactor = contributorMetrics.getLockContention() / 15.0;
        
        // Network stress factor
        double networkFactor = (contributorMetrics.getPacketLoss() / 5.0 + 
                              contributorMetrics.getLatency() / 22.0) / 2.0;
        
        // Conditional probability: P(failure | high_latency_and_stress)
        return Math.min(0.95, baseProbability * (1.0 + resourceFactor + transactionFactor + lockFactor + networkFactor));
    }
    
    /**
     * Validates that all five nodes are present in the analysis.
     */
    private void validateAllNodesPresent(Map<NodeId, NodeMetrics> nodeMetrics) {
        Set<NodeId> requiredNodes = Set.of(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1);
        Set<NodeId> providedNodes = nodeMetrics.keySet();
        
        if (!providedNodes.containsAll(requiredNodes)) {
            Set<NodeId> missingNodes = new HashSet<>(requiredNodes);
            missingNodes.removeAll(providedNodes);
            throw new IllegalArgumentException("Missing nodes in analysis: " + missingNodes);
        }
    }
    
    // Normalization methods based on dataset ranges
    private double normalizeLatency(double latency) {
        return (latency - 8.0) / (22.0 - 8.0); // 8-22ms range
    }
    
    private double normalizeThroughput(double throughput) {
        return (throughput - 470.0) / (1250.0 - 470.0); // 470-1250 Mbps range
    }
    
    private double normalizePacketLoss(double packetLoss) {
        return Math.min(1.0, packetLoss / 5.0); // 0-5% typical range
    }
    
    private double normalizeCpuUtilization(double cpu) {
        return (cpu - 45.0) / (72.0 - 45.0); // 45-72% range
    }
    
    private double normalizeMemoryUsage(double memory) {
        return (memory - 4.0) / (16.0 - 4.0); // 4.0-16.0 GB range
    }
    
    private double normalizeTransactionsPerSec(double transactions) {
        return (transactions - 100.0) / (300.0 - 100.0); // 100-300 tx/sec range
    }
    
    private double normalizeLockContention(double lockContention) {
        return (lockContention - 5.0) / (15.0 - 5.0); // 5-15% range
    }
    
    // Helper methods for bottleneck analysis
    private BottleneckType identifyBottleneckType(NodeMetrics metrics) {
        double cpuNorm = normalizeCpuUtilization(metrics.getCpuUtilization());
        double memoryNorm = normalizeMemoryUsage(metrics.getMemoryUsage());
        double latencyNorm = normalizeLatency(metrics.getLatency());
        double throughputNorm = 1.0 - normalizeThroughput(metrics.getThroughput());
        double lockNorm = normalizeLockContention(metrics.getLockContention());
        
        if (cpuNorm > 0.8) return BottleneckType.CPU;
        if (memoryNorm > 0.8) return BottleneckType.MEMORY;
        if (latencyNorm > 0.7) return BottleneckType.NETWORK_LATENCY;
        if (throughputNorm > 0.7) return BottleneckType.NETWORK_THROUGHPUT;
        if (lockNorm > 0.7) return BottleneckType.LOCK_CONTENTION;
        
        return BottleneckType.MIXED;
    }
    
    private String generateBottleneckDescription(NodeId nodeId, NodeMetrics metrics, BottleneckType type) {
        return String.format("Node %s shows %s bottleneck with metrics: latency=%.1fms, throughput=%.1fMbps, " +
                           "CPU=%.1f%%, memory=%.1fGB, transactions=%d/sec, locks=%.1f%%",
                           nodeId.getId(), type.name().toLowerCase(),
                           metrics.getLatency(), metrics.getThroughput(), metrics.getCpuUtilization(),
                           metrics.getMemoryUsage(), metrics.getTransactionsPerSec(), metrics.getLockContention());
    }
    
    private Set<OptimizationSuggestion> generateOptimizationSuggestions(NodeMetrics metrics, BottleneckType type) {
        Set<OptimizationSuggestion> suggestions = new HashSet<>();
        
        switch (type) {
            case CPU:
                suggestions.add(new OptimizationSuggestion("Load balancing", "Redistribute CPU-intensive tasks"));
                suggestions.add(new OptimizationSuggestion("Process optimization", "Optimize algorithms and reduce CPU usage"));
                break;
            case MEMORY:
                suggestions.add(new OptimizationSuggestion("Memory management", "Implement memory pooling and garbage collection tuning"));
                suggestions.add(new OptimizationSuggestion("Data structure optimization", "Use more memory-efficient data structures"));
                break;
            case NETWORK_LATENCY:
                suggestions.add(new OptimizationSuggestion("Network optimization", "Optimize routing and reduce network hops"));
                suggestions.add(new OptimizationSuggestion("Caching", "Implement local caching to reduce network calls"));
                break;
            case NETWORK_THROUGHPUT:
                suggestions.add(new OptimizationSuggestion("Bandwidth optimization", "Implement data compression and batching"));
                suggestions.add(new OptimizationSuggestion("Connection pooling", "Use connection pooling to improve throughput"));
                break;
            case LOCK_CONTENTION:
                suggestions.add(new OptimizationSuggestion("Lock optimization", "Reduce lock granularity and duration"));
                suggestions.add(new OptimizationSuggestion("Concurrency design", "Implement lock-free data structures"));
                break;
            case MIXED:
                suggestions.add(new OptimizationSuggestion("Comprehensive optimization", "Multi-dimensional performance tuning required"));
                break;
        }
        
        return suggestions;
    }
    
    private String generateLatencyJustification(NodeId nodeId, NodeMetrics metrics, double failureProbability) {
        return String.format("Node %s contributes highest latency (%.1fms) due to resource utilization " +
                           "(CPU: %.1f%%, Memory: %.1fGB), transaction load (%d tx/sec), and lock contention (%.1f%%). " +
                           "Conditional failure probability: %.3f",
                           nodeId.getId(), metrics.getLatency(), metrics.getCpuUtilization(),
                           metrics.getMemoryUsage(), metrics.getTransactionsPerSec(),
                           metrics.getLockContention(), failureProbability);
    }
}