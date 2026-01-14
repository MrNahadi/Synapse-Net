package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Resource limits and constraints for nodes in the distributed telecom system.
 * Based on dataset characteristics with specific ranges.
 */
public class ResourceLimits {
    private final double maxCpuUtilization;        // 0.0 to 1.0 (45-72% range)
    private final double maxMemoryUsage;           // GB (4.0-16.0 GB range)
    private final int maxConcurrentTransactions;   // (100-300 range)
    private final double maxNetworkBandwidth;      // Mbps (470-1250 Mbps range)

    public ResourceLimits(double maxCpuUtilization, double maxMemoryUsage,
                         int maxConcurrentTransactions, double maxNetworkBandwidth) {
        this.maxCpuUtilization = validateCpuUtilization(maxCpuUtilization);
        this.maxMemoryUsage = validateMemoryUsage(maxMemoryUsage);
        this.maxConcurrentTransactions = validateConcurrentTransactions(maxConcurrentTransactions);
        this.maxNetworkBandwidth = validateNetworkBandwidth(maxNetworkBandwidth);
    }

    private double validateCpuUtilization(double cpuUtilization) {
        if (cpuUtilization < 0.0 || cpuUtilization > 1.0) {
            throw new IllegalArgumentException("CPU utilization must be between 0.0 and 1.0, got: " + cpuUtilization);
        }
        return cpuUtilization;
    }

    private double validateMemoryUsage(double memoryUsage) {
        if (memoryUsage < 4.0 || memoryUsage > 16.0) {
            throw new IllegalArgumentException("Memory usage must be between 4.0-16.0 GB, got: " + memoryUsage);
        }
        return memoryUsage;
    }

    private int validateConcurrentTransactions(int transactions) {
        if (transactions < 100 || transactions > 300) {
            throw new IllegalArgumentException("Concurrent transactions must be between 100-300, got: " + transactions);
        }
        return transactions;
    }

    private double validateNetworkBandwidth(double bandwidth) {
        if (bandwidth < 470.0 || bandwidth > 1250.0) {
            throw new IllegalArgumentException("Network bandwidth must be between 470-1250 Mbps, got: " + bandwidth);
        }
        return bandwidth;
    }

    // Getters
    public double getMaxCpuUtilization() { return maxCpuUtilization; }
    public double getMaxMemoryUsage() { return maxMemoryUsage; }
    public int getMaxConcurrentTransactions() { return maxConcurrentTransactions; }
    public double getMaxNetworkBandwidth() { return maxNetworkBandwidth; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceLimits that = (ResourceLimits) o;
        return Double.compare(that.maxCpuUtilization, maxCpuUtilization) == 0 &&
               Double.compare(that.maxMemoryUsage, maxMemoryUsage) == 0 &&
               maxConcurrentTransactions == that.maxConcurrentTransactions &&
               Double.compare(that.maxNetworkBandwidth, maxNetworkBandwidth) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxCpuUtilization, maxMemoryUsage, maxConcurrentTransactions, maxNetworkBandwidth);
    }

    @Override
    public String toString() {
        return "ResourceLimits{" +
               "maxCpuUtilization=" + (maxCpuUtilization * 100) + "%" +
               ", maxMemoryUsage=" + maxMemoryUsage + "GB" +
               ", maxConcurrentTransactions=" + maxConcurrentTransactions +
               ", maxNetworkBandwidth=" + maxNetworkBandwidth + "Mbps" +
               '}';
    }
}