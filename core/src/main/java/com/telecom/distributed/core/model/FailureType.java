package com.telecom.distributed.core.model;

/**
 * Types of failures that can occur in the distributed telecom system.
 * Based on the dataset failure characteristics for each node.
 */
public enum FailureType {
    CRASH("Node stops responding completely - affects Edge1, Core2"),
    OMISSION("Node fails to send/receive some messages - affects Edge2, Cloud1"),
    BYZANTINE("Node exhibits arbitrary malicious behavior - affects Core1"),
    NETWORK_PARTITION("Network connectivity issues between nodes");

    private final String description;

    FailureType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the expected failure type for a given node based on dataset characteristics.
     * @param nodeId Node identifier
     * @return Expected failure type for the node
     */
    public static FailureType getExpectedFailureType(NodeId nodeId) {
        switch (nodeId.getId()) {
            case "Edge1":
            case "Core2":
                return CRASH;
            case "Edge2":
            case "Cloud1":
                return OMISSION;
            case "Core1":
                return BYZANTINE;
            default:
                throw new IllegalArgumentException("Unknown node ID: " + nodeId.getId());
        }
    }
}