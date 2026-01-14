package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a group of nodes that replicate a service or data.
 * Manages primary-replica relationships and consistency requirements.
 */
public class ReplicationGroup {
    private final GroupId groupId;
    private final NodeId primary;
    private final Set<NodeId> replicas;
    private final ReplicationStrategy.ConsistencyLevel consistencyLevel;
    private final ReplicationStrategy strategy;
    private final int replicationFactor;
    private final Set<ServiceId> services;

    public ReplicationGroup(GroupId groupId, NodeId primary, Set<NodeId> replicas,
                           ReplicationStrategy.ConsistencyLevel consistencyLevel,
                           ReplicationStrategy strategy, int replicationFactor) {
        this.groupId = Objects.requireNonNull(groupId, "Group ID cannot be null");
        this.primary = Objects.requireNonNull(primary, "Primary node cannot be null");
        this.replicas = Objects.requireNonNull(replicas, "Replicas cannot be null");
        this.consistencyLevel = Objects.requireNonNull(consistencyLevel, "Consistency level cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "Replication strategy cannot be null");
        this.replicationFactor = validateReplicationFactor(replicationFactor);
        this.services = ConcurrentHashMap.newKeySet();
    }

    private int validateReplicationFactor(int factor) {
        if (factor < 1) {
            throw new IllegalArgumentException("Replication factor must be at least 1, got: " + factor);
        }
        return factor;
    }

    public void addService(ServiceId serviceId) {
        services.add(Objects.requireNonNull(serviceId, "Service ID cannot be null"));
    }

    public void removeService(ServiceId serviceId) {
        services.remove(serviceId);
    }

    public boolean containsNode(NodeId nodeId) {
        return primary.equals(nodeId) || replicas.contains(nodeId);
    }

    public boolean isPrimary(NodeId nodeId) {
        return primary.equals(nodeId);
    }

    public boolean isReplica(NodeId nodeId) {
        return replicas.contains(nodeId);
    }

    public int getTotalNodes() {
        return 1 + replicas.size(); // primary + replicas
    }

    public boolean meetsReplicationFactor() {
        return getTotalNodes() >= replicationFactor;
    }

    // Getters
    public GroupId getGroupId() { return groupId; }
    public NodeId getPrimary() { return primary; }
    public Set<NodeId> getReplicas() { return replicas; }
    public ReplicationStrategy.ConsistencyLevel getConsistencyLevel() { return consistencyLevel; }
    public ReplicationStrategy getStrategy() { return strategy; }
    public int getReplicationFactor() { return replicationFactor; }
    public Set<ServiceId> getServices() { return services; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplicationGroup that = (ReplicationGroup) o;
        return Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId);
    }

    @Override
    public String toString() {
        return "ReplicationGroup{" +
               "groupId=" + groupId +
               ", primary=" + primary +
               ", replicas=" + replicas.size() +
               ", consistencyLevel=" + consistencyLevel +
               ", replicationFactor=" + replicationFactor +
               ", services=" + services.size() +
               '}';
    }
}