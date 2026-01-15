package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Represents a point on the Pareto frontier for throughput-latency trade-off analysis.
 * Each point represents a non-dominated solution in the multi-objective optimization space.
 */
public class ParetoPoint {
    private final double throughput;  // Mbps
    private final double latency;     // milliseconds
    private final SystemConfiguration configuration;
    private final double dominanceScore;
    private final boolean isOptimal;

    public ParetoPoint(double throughput, double latency, SystemConfiguration configuration, 
                      double dominanceScore, boolean isOptimal) {
        if (throughput < 0) {
            throw new IllegalArgumentException("Throughput cannot be negative");
        }
        if (latency < 0) {
            throw new IllegalArgumentException("Latency cannot be negative");
        }
        
        this.throughput = throughput;
        this.latency = latency;
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        this.dominanceScore = dominanceScore;
        this.isOptimal = isOptimal;
    }

    public double getThroughput() {
        return throughput;
    }

    public double getLatency() {
        return latency;
    }

    public SystemConfiguration getConfiguration() {
        return configuration;
    }

    public double getDominanceScore() {
        return dominanceScore;
    }

    public boolean isOptimal() {
        return isOptimal;
    }

    /**
     * Checks if this point dominates another point in the Pareto sense.
     * A point dominates another if it's better in at least one objective and not worse in any.
     */
    public boolean dominates(ParetoPoint other) {
        Objects.requireNonNull(other, "Other point cannot be null");
        
        // For throughput-latency: higher throughput is better, lower latency is better
        boolean betterThroughput = this.throughput >= other.throughput;
        boolean betterLatency = this.latency <= other.latency;
        boolean strictlyBetter = this.throughput > other.throughput || this.latency < other.latency;
        
        return betterThroughput && betterLatency && strictlyBetter;
    }

    /**
     * Calculates the Euclidean distance to another point in the objective space.
     */
    public double distanceTo(ParetoPoint other) {
        Objects.requireNonNull(other, "Other point cannot be null");
        
        double throughputDiff = this.throughput - other.throughput;
        double latencyDiff = this.latency - other.latency;
        
        return Math.sqrt(throughputDiff * throughputDiff + latencyDiff * latencyDiff);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParetoPoint that = (ParetoPoint) o;
        return Double.compare(that.throughput, throughput) == 0 &&
               Double.compare(that.latency, latency) == 0 &&
               Double.compare(that.dominanceScore, dominanceScore) == 0 &&
               isOptimal == that.isOptimal &&
               Objects.equals(configuration, configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(throughput, latency, configuration, dominanceScore, isOptimal);
    }

    @Override
    public String toString() {
        return String.format("ParetoPoint{throughput=%.1f Mbps, latency=%.1f ms, optimal=%s, dominance=%.3f}",
                           throughput, latency, isOptimal, dominanceScore);
    }
}