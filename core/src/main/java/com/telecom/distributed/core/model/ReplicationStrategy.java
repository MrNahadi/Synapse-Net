package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.Set;

/**
 * Replication strategy configuration for fault tolerance.
 * Defines how data and services should be replicated across nodes.
 */
public class ReplicationStrategy {
    private final ReplicationType type;
    private final int replicationFactor;
    private final Set<NodeId> preferredNodes;
    private final ConsistencyLevel consistencyLevel;
    private final boolean crossLayerReplication;

    public ReplicationStrategy(ReplicationType type, int replicationFactor, Set<NodeId> preferredNodes,
                             ConsistencyLevel consistencyLevel, boolean crossLayerReplication) {
        this.type = Objects.requireNonNull(type, "Replication type cannot be null");
        this.replicationFactor = validateReplicationFactor(replicationFactor);
        this.preferredNodes = Objects.requireNonNull(preferredNodes, "Preferred nodes cannot be null");
        this.consistencyLevel = Objects.requireNonNull(consistencyLevel, "Consistency level cannot be null");
        this.crossLayerReplication = crossLayerReplication;
    }

    private int validateReplicationFactor(int factor) {
        if (factor < 1) {
            throw new IllegalArgumentException("Replication factor must be at least 1, got: " + factor);
        }
        return factor;
    }

    public enum ReplicationType {
        ACTIVE("All replicas process requests simultaneously"),
        PASSIVE("Primary processes requests, backups receive state updates"),
        SEMI_ACTIVE("Primary processes requests, backups validate results"),
        BYZANTINE_TOLERANT("Replicas use BFT consensus for agreement");

        private final String description;

        ReplicationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum ConsistencyLevel {
        EVENTUAL("Updates will eventually propagate to all replicas"),
        STRONG("All replicas must agree before operation completes"),
        CAUSAL("Causally related operations are seen in same order"),
        SEQUENTIAL("All operations appear to execute in some sequential order");

        private final String description;

        ConsistencyLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getters
    public ReplicationType getType() { return type; }
    public int getReplicationFactor() { return replicationFactor; }
    public Set<NodeId> getPreferredNodes() { return preferredNodes; }
    public ConsistencyLevel getConsistencyLevel() { return consistencyLevel; }
    public boolean isCrossLayerReplication() { return crossLayerReplication; }

    public boolean isByzantineTolerant() {
        return type == ReplicationType.BYZANTINE_TOLERANT;
    }

    public int getMinimumNodes() {
        return isByzantineTolerant() ? 3 * replicationFactor + 1 : replicationFactor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplicationStrategy that = (ReplicationStrategy) o;
        return replicationFactor == that.replicationFactor &&
               crossLayerReplication == that.crossLayerReplication &&
               type == that.type &&
               Objects.equals(preferredNodes, that.preferredNodes) &&
               consistencyLevel == that.consistencyLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, replicationFactor, preferredNodes, consistencyLevel, crossLayerReplication);
    }

    @Override
    public String toString() {
        return "ReplicationStrategy{" +
               "type=" + type +
               ", replicationFactor=" + replicationFactor +
               ", preferredNodes=" + preferredNodes.size() +
               ", consistencyLevel=" + consistencyLevel +
               ", crossLayerReplication=" + crossLayerReplication +
               '}';
    }
}