package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.Map;

/**
 * Load balancer interface for heterogeneity-aware process and service allocation
 * in the distributed telecom system.
 */
public interface LoadBalancer {
    
    /**
     * Selects the optimal node for a service request based on current load distribution
     * and node capabilities.
     * 
     * @param request The service request to allocate
     * @return The selected node ID for the request
     */
    NodeId selectNode(ServiceRequest request);
    
    /**
     * Updates the weights for nodes based on their current performance and capacity.
     * 
     * @param weights Map of node IDs to their weights (higher weight = more capacity)
     */
    void updateNodeWeights(Map<NodeId, Double> weights);
    
    /**
     * Migrates a service from one node to another.
     * 
     * @param service The service to migrate
     * @param from Source node
     * @param to Destination node
     */
    void migrateService(ServiceId service, NodeId from, NodeId to);
    
    /**
     * Gets current load balancing metrics and statistics.
     * 
     * @return Current load balancing metrics
     */
    LoadBalancingMetrics getMetrics();
    
    /**
     * Updates the current metrics for all nodes to enable informed load balancing decisions.
     * 
     * @param nodeMetrics Map of node IDs to their current metrics
     */
    void updateNodeMetrics(Map<NodeId, NodeMetrics> nodeMetrics);
    
    /**
     * Handles dynamic traffic fluctuations by adjusting load distribution.
     * 
     * @param trafficPattern Current traffic pattern
     */
    void handleTrafficFluctuation(TrafficPattern trafficPattern);
    
    /**
     * Gets the current load balancing strategy being used.
     * 
     * @return The current strategy name
     */
    String getCurrentStrategy();
    
    /**
     * Sets the load balancing strategy to use.
     * 
     * @param strategy The strategy to use (from LoadBalancingStrategy constants)
     */
    void setStrategy(String strategy);
}