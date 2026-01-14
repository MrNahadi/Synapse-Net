package com.telecom.distributed.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Configuration for fault tolerance mechanisms across the system.
 */
public class FaultToleranceConfiguration {
    private final Map<FailureType, Long> detectionTimeouts;
    private final Map<FailureType, Long> recoveryTimeouts;
    private final int maxConcurrentFailures;
    private final double cascadeRiskThreshold;
    private final boolean enableByzantineDetection;
    private final int byzantineToleranceLevel; // f in 3f+1 nodes
    private final long heartbeatIntervalMs;
    private final int maxRetryAttempts;
    private final boolean enableCrossLayerReplication;

    public FaultToleranceConfiguration(Map<FailureType, Long> detectionTimeouts,
                                     Map<FailureType, Long> recoveryTimeouts,
                                     int maxConcurrentFailures,
                                     double cascadeRiskThreshold,
                                     boolean enableByzantineDetection,
                                     int byzantineToleranceLevel,
                                     long heartbeatIntervalMs,
                                     int maxRetryAttempts,
                                     boolean enableCrossLayerReplication) {
        this.detectionTimeouts = Objects.requireNonNull(detectionTimeouts, "Detection timeouts cannot be null");
        this.recoveryTimeouts = Objects.requireNonNull(recoveryTimeouts, "Recovery timeouts cannot be null");
        this.maxConcurrentFailures = validateMaxConcurrentFailures(maxConcurrentFailures);
        this.cascadeRiskThreshold = validateCascadeRiskThreshold(cascadeRiskThreshold);
        this.enableByzantineDetection = enableByzantineDetection;
        this.byzantineToleranceLevel = validateByzantineToleranceLevel(byzantineToleranceLevel);
        this.heartbeatIntervalMs = validateHeartbeatInterval(heartbeatIntervalMs);
        this.maxRetryAttempts = validateMaxRetryAttempts(maxRetryAttempts);
        this.enableCrossLayerReplication = enableCrossLayerReplication;
    }

    private int validateMaxConcurrentFailures(int maxFailures) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("Max concurrent failures must be at least 1, got: " + maxFailures);
        }
        return maxFailures;
    }

    private double validateCascadeRiskThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Cascade risk threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        return threshold;
    }

    private int validateByzantineToleranceLevel(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Byzantine tolerance level must be non-negative, got: " + level);
        }
        return level;
    }

    private long validateHeartbeatInterval(long interval) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Heartbeat interval must be positive, got: " + interval);
        }
        return interval;
    }

    private int validateMaxRetryAttempts(int attempts) {
        if (attempts < 0) {
            throw new IllegalArgumentException("Max retry attempts must be non-negative, got: " + attempts);
        }
        return attempts;
    }

    // Getters
    public Map<FailureType, Long> getDetectionTimeouts() { return detectionTimeouts; }
    public Map<FailureType, Long> getRecoveryTimeouts() { return recoveryTimeouts; }
    public int getMaxConcurrentFailures() { return maxConcurrentFailures; }
    public double getCascadeRiskThreshold() { return cascadeRiskThreshold; }
    public boolean isEnableByzantineDetection() { return enableByzantineDetection; }
    public int getByzantineToleranceLevel() { return byzantineToleranceLevel; }
    public long getHeartbeatIntervalMs() { return heartbeatIntervalMs; }
    public int getMaxRetryAttempts() { return maxRetryAttempts; }
    public boolean isEnableCrossLayerReplication() { return enableCrossLayerReplication; }

    public long getDetectionTimeout(FailureType failureType) {
        return detectionTimeouts.getOrDefault(failureType, 5000L); // Default 5 seconds
    }

    public long getRecoveryTimeout(FailureType failureType) {
        return recoveryTimeouts.getOrDefault(failureType, 10000L); // Default 10 seconds
    }

    public int getMinimumNodesForByzantineTolerance() {
        return 3 * byzantineToleranceLevel + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FaultToleranceConfiguration that = (FaultToleranceConfiguration) o;
        return maxConcurrentFailures == that.maxConcurrentFailures &&
               Double.compare(that.cascadeRiskThreshold, cascadeRiskThreshold) == 0 &&
               enableByzantineDetection == that.enableByzantineDetection &&
               byzantineToleranceLevel == that.byzantineToleranceLevel &&
               heartbeatIntervalMs == that.heartbeatIntervalMs &&
               maxRetryAttempts == that.maxRetryAttempts &&
               enableCrossLayerReplication == that.enableCrossLayerReplication &&
               Objects.equals(detectionTimeouts, that.detectionTimeouts) &&
               Objects.equals(recoveryTimeouts, that.recoveryTimeouts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detectionTimeouts, recoveryTimeouts, maxConcurrentFailures, cascadeRiskThreshold,
                          enableByzantineDetection, byzantineToleranceLevel, heartbeatIntervalMs, 
                          maxRetryAttempts, enableCrossLayerReplication);
    }

    @Override
    public String toString() {
        return "FaultToleranceConfiguration{" +
               "maxConcurrentFailures=" + maxConcurrentFailures +
               ", cascadeRiskThreshold=" + cascadeRiskThreshold +
               ", enableByzantineDetection=" + enableByzantineDetection +
               ", byzantineToleranceLevel=" + byzantineToleranceLevel +
               ", heartbeatIntervalMs=" + heartbeatIntervalMs +
               ", maxRetryAttempts=" + maxRetryAttempts +
               ", enableCrossLayerReplication=" + enableCrossLayerReplication +
               '}';
    }
}