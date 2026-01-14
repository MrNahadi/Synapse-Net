package com.telecom.distributed.core.model;

/**
 * Enumeration of the three architectural layers in the distributed telecom system.
 */
public enum NodeLayer {
    EDGE("Edge layer - handles user-facing services with low latency requirements"),
    CORE("Core layer - manages transaction processing and coordination"),
    CLOUD("Cloud layer - provides analytics and distributed shared memory services");

    private final String description;

    NodeLayer(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines the layer for a given node ID.
     * @param nodeId Node identifier
     * @return Corresponding layer
     */
    public static NodeLayer fromNodeId(NodeId nodeId) {
        String id = nodeId.getId();
        if (id.startsWith("Edge")) {
            return EDGE;
        } else if (id.startsWith("Core")) {
            return CORE;
        } else if (id.startsWith("Cloud")) {
            return CLOUD;
        }
        throw new IllegalArgumentException("Unknown node layer for ID: " + id);
    }
}