package com.telecom.distributed.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Metrics for load balancing operations and performance.
 */
public class LoadBalancingMetrics {
    private final Map<NodeId, Double> cpuDistribution;      // CPU load distribution across nodes
    private final Map<NodeId, Double> memoryDistribution;   // Memory load distribution across nodes
    private final Map<NodeId, Integer> transactionDistribution; // Transaction load distribution
    private final double loadBalanceIndex;                  // How well balanced the load is (0-1, 1 is perfect)
    private final int totalServicesAllocated;
    private final int migrationCount;                       // Number of services migrated
    private final long lastUpdateTimestamp;

    public LoadBalancingMetrics(Map<NodeId, Double> cpuDistribution,
                               Map<NodeId, Double> memoryDistribution,
                               Map<NodeId, Integer> transactionDistribution,
                               double loadBalanceIndex,
                               int totalServicesAllocated,
                               int migrationCount,
                               long lastUpdateTimestamp) {
        this.cpuDistribution = Objects.requireNonNull(cpuDistribution, "CPU distribution cannot be null");
        this.memoryDistribution = Objects.requireNonNull(memoryDistribution, "Memory distribution cannot be null");
        this.transactionDistribution = Objects.requireNonNull(transactionDistribution, "Transaction distribution cannot be null");
        this.loadBalanceIndex = validateLoadBalanceIndex(loadBalanceIndex);
        this.totalServicesAllocated = validateNonNegative(totalServicesAllocated, "Total services allocated");
        this.migrationCount = validateNonNegative(migrationCount, "Migration count");
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    private double validateLoadBalanceIndex(double index) {
        if (index < 0.0 || index > 1.0) {
            throw new IllegalArgumentException("Load balance index must be between 0-1, got: " + index);
        }
        return index;
    }

    private int validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative, got: " + value);
        }
        return value;
    }

    // Getters
    public Map<NodeId, Double> getCpuDistribution() { return cpuDistribution; }
    public Map<NodeId, Double> getMemoryDistribution() { return memoryDistribution; }
    public Map<NodeId, Integer> getTransactionDistribution() { return transactionDistribution; }
    public double getLoadBalanceIndex() { return loadBalanceIndex; }
    public int getTotalServicesAllocated() { return totalServicesAllocated; }
    public int getMigrationCount() { return migrationCount; }
    public long getLastUpdateTimestamp() { return lastUpdateTimestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoadBalancingMetrics that = (LoadBalancingMetrics) o;
        return Double.compare(that.loadBalanceIndex, loadBalanceIndex) == 0 &&
               totalServicesAllocated == that.totalServicesAllocated &&
               migrationCount == that.migrationCount &&
               lastUpdateTimestamp == that.lastUpdateTimestamp &&
               Objects.equals(cpuDistribution, that.cpuDistribution) &&
               Objects.equals(memoryDistribution, that.memoryDistribution) &&
               Objects.equals(transactionDistribution, that.transactionDistribution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpuDistribution, memoryDistribution, transactionDistribution, 
                          loadBalanceIndex, totalServicesAllocated, migrationCount, lastUpdateTimestamp);
    }

    @Override
    public String toString() {
        return "LoadBalancingMetrics{" +
               "cpuDistribution=" + cpuDistribution +
               ", memoryDistribution=" + memoryDistribution +
               ", transactionDistribution=" + transactionDistribution +
               ", loadBalanceIndex=" + loadBalanceIndex +
               ", totalServicesAllocated=" + totalServicesAllocated +
               ", migrationCount=" + migrationCount +
               ", lastUpdateTimestamp=" + lastUpdateTimestamp +
               '}';
    }
}