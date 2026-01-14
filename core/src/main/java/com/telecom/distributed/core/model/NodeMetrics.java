package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Performance and resource metrics for nodes in the distributed telecom system.
 * Based on dataset characteristics with specific ranges for each metric.
 */
public class NodeMetrics {
    private final double latency;              // milliseconds (8-22ms range)
    private final double throughput;           // Mbps (470-1250 Mbps range)
    private final double packetLoss;           // percentage (0-5% typical)
    private final double cpuUtilization;       // percentage (45-72% range)
    private final double memoryUsage;          // GB (4.0-16.0 GB range)
    private final int transactionsPerSec;      // tx/sec (100-300 range)
    private final double lockContention;       // percentage (5-15% range)

    public NodeMetrics(double latency, double throughput, double packetLoss,
                      double cpuUtilization, double memoryUsage, 
                      int transactionsPerSec, double lockContention) {
        this.latency = validateLatency(latency);
        this.throughput = validateThroughput(throughput);
        this.packetLoss = validatePacketLoss(packetLoss);
        this.cpuUtilization = validateCpuUtilization(cpuUtilization);
        this.memoryUsage = validateMemoryUsage(memoryUsage);
        this.transactionsPerSec = validateTransactionsPerSec(transactionsPerSec);
        this.lockContention = validateLockContention(lockContention);
    }

    // Validation methods based on dataset characteristics
    private double validateLatency(double latency) {
        if (latency < 8.0 || latency > 22.0) {
            throw new IllegalArgumentException("Latency must be between 8-22ms, got: " + latency);
        }
        return latency;
    }

    private double validateThroughput(double throughput) {
        if (throughput < 470.0 || throughput > 1250.0) {
            throw new IllegalArgumentException("Throughput must be between 470-1250 Mbps, got: " + throughput);
        }
        return throughput;
    }

    private double validatePacketLoss(double packetLoss) {
        if (packetLoss < 0.0 || packetLoss > 100.0) {
            throw new IllegalArgumentException("Packet loss must be between 0-100%, got: " + packetLoss);
        }
        return packetLoss;
    }

    private double validateCpuUtilization(double cpuUtilization) {
        if (cpuUtilization < 45.0 || cpuUtilization > 72.0) {
            throw new IllegalArgumentException("CPU utilization must be between 45-72%, got: " + cpuUtilization);
        }
        return cpuUtilization;
    }

    private double validateMemoryUsage(double memoryUsage) {
        if (memoryUsage < 4.0 || memoryUsage > 16.0) {
            throw new IllegalArgumentException("Memory usage must be between 4.0-16.0 GB, got: " + memoryUsage);
        }
        return memoryUsage;
    }

    private int validateTransactionsPerSec(int transactionsPerSec) {
        if (transactionsPerSec < 100 || transactionsPerSec > 300) {
            throw new IllegalArgumentException("Transactions per second must be between 100-300, got: " + transactionsPerSec);
        }
        return transactionsPerSec;
    }

    private double validateLockContention(double lockContention) {
        if (lockContention < 5.0 || lockContention > 15.0) {
            throw new IllegalArgumentException("Lock contention must be between 5-15%, got: " + lockContention);
        }
        return lockContention;
    }

    // Getters
    public double getLatency() { return latency; }
    public double getThroughput() { return throughput; }
    public double getPacketLoss() { return packetLoss; }
    public double getCpuUtilization() { return cpuUtilization; }
    public double getMemoryUsage() { return memoryUsage; }
    public int getTransactionsPerSec() { return transactionsPerSec; }
    public double getLockContention() { return lockContention; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeMetrics that = (NodeMetrics) o;
        return Double.compare(that.latency, latency) == 0 &&
               Double.compare(that.throughput, throughput) == 0 &&
               Double.compare(that.packetLoss, packetLoss) == 0 &&
               Double.compare(that.cpuUtilization, cpuUtilization) == 0 &&
               Double.compare(that.memoryUsage, memoryUsage) == 0 &&
               transactionsPerSec == that.transactionsPerSec &&
               Double.compare(that.lockContention, lockContention) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latency, throughput, packetLoss, cpuUtilization, 
                          memoryUsage, transactionsPerSec, lockContention);
    }

    @Override
    public String toString() {
        return "NodeMetrics{" +
               "latency=" + latency + "ms" +
               ", throughput=" + throughput + "Mbps" +
               ", packetLoss=" + packetLoss + "%" +
               ", cpuUtilization=" + cpuUtilization + "%" +
               ", memoryUsage=" + memoryUsage + "GB" +
               ", transactionsPerSec=" + transactionsPerSec +
               ", lockContention=" + lockContention + "%" +
               '}';
    }
}