package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.Set;

/**
 * Information about network partitions in the distributed telecom system.
 */
public class NetworkPartition {
    private final Set<Set<NodeId>> partitions;
    private final long detectedAt;
    private final PartitionType type;

    public NetworkPartition(Set<Set<NodeId>> partitions, long detectedAt, PartitionType type) {
        this.partitions = Objects.requireNonNull(partitions, "Partitions cannot be null");
        this.detectedAt = detectedAt;
        this.type = Objects.requireNonNull(type, "Partition type cannot be null");
        validatePartitions();
    }

    private void validatePartitions() {
        if (partitions.isEmpty()) {
            throw new IllegalArgumentException("Partitions cannot be empty");
        }
        if (partitions.size() < 2) {
            throw new IllegalArgumentException("Must have at least 2 partitions");
        }
    }

    public enum PartitionType {
        CLEAN_SPLIT("Network cleanly split into separate partitions"),
        ASYMMETRIC("Asymmetric partition with different connectivity"),
        CASCADING("Cascading partition due to multiple node failures");

        private final String description;

        PartitionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getters
    public Set<Set<NodeId>> getPartitions() { return partitions; }
    public long getDetectedAt() { return detectedAt; }
    public PartitionType getType() { return type; }

    public int getPartitionCount() {
        return partitions.size();
    }

    public boolean isNodeInSamePartition(NodeId node1, NodeId node2) {
        for (Set<NodeId> partition : partitions) {
            if (partition.contains(node1) && partition.contains(node2)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkPartition that = (NetworkPartition) o;
        return detectedAt == that.detectedAt &&
               Objects.equals(partitions, that.partitions) &&
               type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(partitions, detectedAt, type);
    }

    @Override
    public String toString() {
        return "NetworkPartition{" +
               "partitionCount=" + partitions.size() +
               ", detectedAt=" + detectedAt +
               ", type=" + type +
               '}';
    }
}