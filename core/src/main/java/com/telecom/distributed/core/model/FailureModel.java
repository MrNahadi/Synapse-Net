package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Failure model configuration for nodes in the distributed telecom system.
 */
public class FailureModel {
    private final FailureType primaryFailureType;
    private final double failureProbability;
    private final long detectionTimeoutMs;
    private final long recoveryTimeoutMs;

    public FailureModel(FailureType primaryFailureType, double failureProbability,
                       long detectionTimeoutMs, long recoveryTimeoutMs) {
        this.primaryFailureType = Objects.requireNonNull(primaryFailureType, "Primary failure type cannot be null");
        this.failureProbability = validateFailureProbability(failureProbability);
        this.detectionTimeoutMs = validateTimeout(detectionTimeoutMs, "Detection timeout");
        this.recoveryTimeoutMs = validateTimeout(recoveryTimeoutMs, "Recovery timeout");
    }

    private double validateFailureProbability(double probability) {
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("Failure probability must be between 0.0 and 1.0, got: " + probability);
        }
        return probability;
    }

    private long validateTimeout(long timeout, String timeoutType) {
        if (timeout <= 0) {
            throw new IllegalArgumentException(timeoutType + " must be positive, got: " + timeout);
        }
        return timeout;
    }

    // Getters
    public FailureType getPrimaryFailureType() { return primaryFailureType; }
    public double getFailureProbability() { return failureProbability; }
    public long getDetectionTimeoutMs() { return detectionTimeoutMs; }
    public long getRecoveryTimeoutMs() { return recoveryTimeoutMs; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FailureModel that = (FailureModel) o;
        return Double.compare(that.failureProbability, failureProbability) == 0 &&
               detectionTimeoutMs == that.detectionTimeoutMs &&
               recoveryTimeoutMs == that.recoveryTimeoutMs &&
               primaryFailureType == that.primaryFailureType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryFailureType, failureProbability, detectionTimeoutMs, recoveryTimeoutMs);
    }

    @Override
    public String toString() {
        return "FailureModel{" +
               "primaryFailureType=" + primaryFailureType +
               ", failureProbability=" + failureProbability +
               ", detectionTimeoutMs=" + detectionTimeoutMs +
               ", recoveryTimeoutMs=" + recoveryTimeoutMs +
               '}';
    }
}