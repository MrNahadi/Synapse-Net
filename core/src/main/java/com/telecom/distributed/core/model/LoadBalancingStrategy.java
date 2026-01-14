package com.telecom.distributed.core.model;

/**
 * Constants for load balancing strategies.
 */
public class LoadBalancingStrategy {
    public static final String WEIGHTED_ROUND_ROBIN = "weighted_round_robin";
    public static final String LEAST_CONNECTIONS = "least_connections";
    public static final String RESOURCE_AWARE = "resource_aware";
    
    private LoadBalancingStrategy() {
        // Utility class
    }
}