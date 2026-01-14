package com.telecom.distributed.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Health status information for nodes in the distributed telecom system.
 */
public class HealthStatus {
    private final Status status;
    private final String message;
    private final Instant lastUpdated;
    private final double healthScore; // 0.0 to 1.0

    public HealthStatus(Status status, String message, Instant lastUpdated, double healthScore) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.message = Objects.requireNonNull(message, "Message cannot be null");
        this.lastUpdated = Objects.requireNonNull(lastUpdated, "LastUpdated cannot be null");
        this.healthScore = validateHealthScore(healthScore);
    }

    private double validateHealthScore(double healthScore) {
        if (healthScore < 0.0 || healthScore > 1.0) {
            throw new IllegalArgumentException("Health score must be between 0.0 and 1.0, got: " + healthScore);
        }
        return healthScore;
    }

    public enum Status {
        HEALTHY("Node is operating normally"),
        DEGRADED("Node is operational but with reduced performance"),
        UNHEALTHY("Node has significant issues but is still responsive"),
        FAILED("Node is not responding or has crashed");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getters
    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public Instant getLastUpdated() { return lastUpdated; }
    public double getHealthScore() { return healthScore; }

    public boolean isHealthy() {
        return status == Status.HEALTHY;
    }

    public boolean isOperational() {
        return status == Status.HEALTHY || status == Status.DEGRADED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthStatus that = (HealthStatus) o;
        return Double.compare(that.healthScore, healthScore) == 0 &&
               status == that.status &&
               Objects.equals(message, that.message) &&
               Objects.equals(lastUpdated, that.lastUpdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, message, lastUpdated, healthScore);
    }

    @Override
    public String toString() {
        return "HealthStatus{" +
               "status=" + status +
               ", message='" + message + '\'' +
               ", lastUpdated=" + lastUpdated +
               ", healthScore=" + healthScore +
               '}';
    }
}