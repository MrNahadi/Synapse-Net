package com.telecom.distributed.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Aggregated performance metrics for the entire distributed system.
 */
public class SystemPerformanceMetrics {
    private final Map<NodeId, NodeMetrics> nodeMetrics;
    private final double averageLatency;
    private final double averageThroughput;
    private final double averageCpuUtilization;
    private final double averageMemoryUsage;
    private final int totalTransactionsPerSec;
    
    public SystemPerformanceMetrics(Map<NodeId, NodeMetrics> nodeMetrics) {
        this.nodeMetrics = Objects.requireNonNull(nodeMetrics, "Node metrics cannot be null");
        
        // Calculate aggregated metrics
        this.averageLatency = nodeMetrics.values().stream()
            .mapToDouble(NodeMetrics::getLatency)
            .average()
            .orElse(0.0);
        
        this.averageThroughput = nodeMetrics.values().stream()
            .mapToDouble(NodeMetrics::getThroughput)
            .average()
            .orElse(0.0);
        
        this.averageCpuUtilization = nodeMetrics.values().stream()
            .mapToDouble(NodeMetrics::getCpuUtilization)
            .average()
            .orElse(0.0);
        
        this.averageMemoryUsage = nodeMetrics.values().stream()
            .mapToDouble(NodeMetrics::getMemoryUsage)
            .average()
            .orElse(0.0);
        
        this.totalTransactionsPerSec = nodeMetrics.values().stream()
            .mapToInt(NodeMetrics::getTransactionsPerSec)
            .sum();
    }
    
    public Map<NodeId, NodeMetrics> getNodeMetrics() {
        return nodeMetrics;
    }
    
    public double getAverageLatency() {
        return averageLatency;
    }
    
    public double getAverageThroughput() {
        return averageThroughput;
    }
    
    public double getAverageCpuUtilization() {
        return averageCpuUtilization;
    }
    
    public double getAverageMemoryUsage() {
        return averageMemoryUsage;
    }
    
    public int getTotalTransactionsPerSec() {
        return totalTransactionsPerSec;
    }
    
    public double getTransactionAbortRate() {
        // Estimate based on lock contention
        return nodeMetrics.values().stream()
            .mapToDouble(NodeMetrics::getLockContention)
            .average()
            .orElse(0.0) / 100.0;
    }
    
    public double getDeadlockRate() {
        // Estimate based on lock contention
        return nodeMetrics.values().stream()
            .mapToDouble(NodeMetrics::getLockContention)
            .max()
            .orElse(0.0) / 100.0;
    }
    
    public double getThroughputVariance() {
        double avg = averageThroughput;
        double variance = nodeMetrics.values().stream()
            .mapToDouble(m -> Math.pow(m.getThroughput() - avg, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }
    
    @Override
    public String toString() {
        return String.format("SystemPerformanceMetrics{nodes=%d, avgLatency=%.2fms, " +
                           "avgThroughput=%.2fMbps, avgCPU=%.2f%%, avgMemory=%.2fGB, totalTx=%d/sec}",
                           nodeMetrics.size(), averageLatency, averageThroughput,
                           averageCpuUtilization, averageMemoryUsage, totalTransactionsPerSec);
    }
}
