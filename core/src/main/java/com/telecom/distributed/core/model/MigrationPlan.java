package com.telecom.distributed.core.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a plan for migrating services between nodes while preserving availability.
 */
public class MigrationPlan {
    private final MigrationId migrationId;
    private final ServiceId serviceId;
    private final NodeId sourceNode;
    private final NodeId targetNode;
    private final MigrationStrategy strategy;
    private final Instant scheduledTime;
    private final long estimatedDurationMs;
    private final Set<ServiceId> dependencies;
    private MigrationStatus status;

    public MigrationPlan(MigrationId migrationId, ServiceId serviceId, NodeId sourceNode, NodeId targetNode,
                        MigrationStrategy strategy, Instant scheduledTime, long estimatedDurationMs,
                        Set<ServiceId> dependencies) {
        this.migrationId = Objects.requireNonNull(migrationId, "Migration ID cannot be null");
        this.serviceId = Objects.requireNonNull(serviceId, "Service ID cannot be null");
        this.sourceNode = Objects.requireNonNull(sourceNode, "Source node cannot be null");
        this.targetNode = Objects.requireNonNull(targetNode, "Target node cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "Migration strategy cannot be null");
        this.scheduledTime = Objects.requireNonNull(scheduledTime, "Scheduled time cannot be null");
        this.estimatedDurationMs = validateDuration(estimatedDurationMs);
        this.dependencies = Objects.requireNonNull(dependencies, "Dependencies cannot be null");
        this.status = MigrationStatus.PLANNED;
    }

    private long validateDuration(long duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Estimated duration must be positive, got: " + duration);
        }
        return duration;
    }

    public void updateStatus(MigrationStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "Migration status cannot be null");
    }

    public boolean isReadyToExecute() {
        return status == MigrationStatus.PLANNED && Instant.now().isAfter(scheduledTime);
    }

    public boolean isInProgress() {
        return status == MigrationStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == MigrationStatus.COMPLETED;
    }

    public boolean hasFailed() {
        return status == MigrationStatus.FAILED;
    }

    // Getters
    public MigrationId getMigrationId() { return migrationId; }
    public ServiceId getServiceId() { return serviceId; }
    public NodeId getSourceNode() { return sourceNode; }
    public NodeId getTargetNode() { return targetNode; }
    public MigrationStrategy getStrategy() { return strategy; }
    public Instant getScheduledTime() { return scheduledTime; }
    public long getEstimatedDurationMs() { return estimatedDurationMs; }
    public Set<ServiceId> getDependencies() { return dependencies; }
    public MigrationStatus getStatus() { return status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MigrationPlan that = (MigrationPlan) o;
        return Objects.equals(migrationId, that.migrationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(migrationId);
    }

    @Override
    public String toString() {
        return "MigrationPlan{" +
               "migrationId=" + migrationId +
               ", serviceId=" + serviceId +
               ", sourceNode=" + sourceNode +
               ", targetNode=" + targetNode +
               ", strategy=" + strategy +
               ", status=" + status +
               '}';
    }
}