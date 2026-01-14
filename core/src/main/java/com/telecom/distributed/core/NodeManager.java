package com.telecom.distributed.core;

import com.telecom.distributed.core.model.NodeConfiguration;
import com.telecom.distributed.core.model.NodeMetrics;
import com.telecom.distributed.core.model.HealthStatus;
import com.telecom.distributed.core.model.FailureType;

/**
 * Core interface for managing individual nodes in the distributed telecom system.
 * Handles node configuration, metrics collection, health monitoring, and failure handling.
 */
public interface NodeManager {
    
    /**
     * Retrieves current performance and resource metrics for this node.
     * @return Current node metrics including latency, throughput, CPU, memory usage
     */
    NodeMetrics getMetrics();
    
    /**
     * Updates the configuration of this node.
     * @param config New configuration to apply
     */
    void updateConfiguration(NodeConfiguration config);
    
    /**
     * Gets the current health status of this node.
     * @return Health status indicating if node is operational
     */
    HealthStatus getHealthStatus();
    
    /**
     * Handles a detected failure on this node.
     * @param type Type of failure detected (crash, omission, byzantine)
     */
    void handleFailure(FailureType type);
}