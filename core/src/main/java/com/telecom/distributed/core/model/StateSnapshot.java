package com.telecom.distributed.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Snapshot of distributed system state at a point in time.
 * Used for replication and recovery.
 */
public class StateSnapshot {
    private final String snapshotId;
    private final Instant timestamp;
    private final NodeId sourceNode;
    private final Map<String, Object> stateData;
    private final String checksum;
    private final long sequenceNumber;

    public StateSnapshot(String snapshotId, Instant timestamp, NodeId sourceNode,
                        Map<String, Object> stateData, String checksum, long sequenceNumber) {
        this.snapshotId = Objects.requireNonNull(snapshotId, "Snapshot ID cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.sourceNode = Objects.requireNonNull(sourceNode, "Source node cannot be null");
        this.stateData = Objects.requireNonNull(stateData, "State data cannot be null");
        this.checksum = Objects.requireNonNull(checksum, "Checksum cannot be null");
        this.sequenceNumber = validateSequenceNumber(sequenceNumber);
    }

    private long validateSequenceNumber(long seqNum) {
        if (seqNum < 0) {
            throw new IllegalArgumentException("Sequence number cannot be negative: " + seqNum);
        }
        return seqNum;
    }

    public boolean isNewerThan(StateSnapshot other) {
        return this.sequenceNumber > other.sequenceNumber;
    }

    public boolean isFromSameSource(StateSnapshot other) {
        return this.sourceNode.equals(other.sourceNode);
    }

    // Getters
    public String getSnapshotId() { return snapshotId; }
    public Instant getTimestamp() { return timestamp; }
    public NodeId getSourceNode() { return sourceNode; }
    public Map<String, Object> getStateData() { return stateData; }
    public String getChecksum() { return checksum; }
    public long getSequenceNumber() { return sequenceNumber; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateSnapshot that = (StateSnapshot) o;
        return Objects.equals(snapshotId, that.snapshotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshotId);
    }

    @Override
    public String toString() {
        return "StateSnapshot{" +
               "snapshotId='" + snapshotId + '\'' +
               ", timestamp=" + timestamp +
               ", sourceNode=" + sourceNode +
               ", sequenceNumber=" + sequenceNumber +
               ", dataSize=" + stateData.size() +
               '}';
    }
}
