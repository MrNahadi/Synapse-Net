package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Unique identifier for nodes in the distributed telecom system.
 * Represents the five nodes: Edge1, Edge2, Core1, Core2, Cloud1.
 */
public class NodeId {
    private final String id;

    public NodeId(String id) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        validateNodeId(id);
    }

    private void validateNodeId(String id) {
        if (!id.matches("^(Edge[12]|Core[12]|Cloud1)$")) {
            throw new IllegalArgumentException("Invalid node ID. Must be one of: Edge1, Edge2, Core1, Core2, Cloud1. Got: " + id);
        }
    }

    public String getId() {
        return id;
    }

    // Predefined node IDs based on dataset
    public static final NodeId EDGE1 = new NodeId("Edge1");
    public static final NodeId EDGE2 = new NodeId("Edge2");
    public static final NodeId CORE1 = new NodeId("Core1");
    public static final NodeId CORE2 = new NodeId("Core2");
    public static final NodeId CLOUD1 = new NodeId("Cloud1");

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeId nodeId = (NodeId) o;
        return Objects.equals(id, nodeId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NodeId{" + id + '}';
    }
}