package com.telecom.distributed.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Status of replication for a file or state.
 */
public class ReplicationStatus {
    private final String resourceId;
    private final NodeId nodeId;
    private final ReplicationState state;
    private final Instant lastSyncTime;
    private final long bytesReplicated;
    private final double syncProgress; // 0.0 to 1.0

    public ReplicationStatus(String resourceId, NodeId nodeId, ReplicationState state,
                           Instant lastSyncTime, long bytesReplicated, double syncProgress) {
        this.resourceId = Objects.requireNonNull(resourceId, "Resource ID cannot be null");
        this.nodeId = Objects.requireNonNull(nodeId, "Node ID cannot be null");
        this.state = Objects.requireNonNull(state, "Replication state cannot be null");
        this.lastSyncTime = Objects.requireNonNull(lastSyncTime, "Last sync time cannot be null");
        this.bytesReplicated = validateBytesReplicated(bytesReplicated);
        this.syncProgress = validateSyncProgress(syncProgress);
    }

    private long validateBytesReplicated(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Bytes replicated cannot be negative: " + bytes);
        }
        return bytes;
    }

    private double validateSyncProgress(double progress) {
        if (progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException("Sync progress must be between 0.0 and 1.0: " + progress);
        }
        return progress;
    }

    public boolean isComplete() {
        return state == ReplicationState.SYNCED && syncProgress >= 1.0;
    }

    public boolean isFailed() {
        return state == ReplicationState.FAILED;
    }

    public boolean isInProgress() {
        return state == ReplicationState.SYNCING;
    }

    public enum ReplicationState {
        PENDING("Replication pending"),
        SYNCING("Replication in progress"),
        SYNCED("Replication complete"),
        FAILED("Replication failed"),
        STALE("Replica is stale");

        private final String description;

        ReplicationState(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getters
    public String getResourceId() { return resourceId; }
    public NodeId getNodeId() { return nodeId; }
    public ReplicationState getState() { return state; }
    public Instant getLastSyncTime() { return lastSyncTime; }
    public long getBytesReplicated() { return bytesReplicated; }
    public double getSyncProgress() { return syncProgress; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplicationStatus that = (ReplicationStatus) o;
        return Objects.equals(resourceId, that.resourceId) &&
               Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, nodeId);
    }

    @Override
    public String toString() {
        return "ReplicationStatus{" +
               "resourceId='" + resourceId + '\'' +
               ", nodeId=" + nodeId +
               ", state=" + state +
               ", progress=" + String.format("%.1f%%", syncProgress * 100) +
               ", bytes=" + bytesReplicated +
               '}';
    }
}
