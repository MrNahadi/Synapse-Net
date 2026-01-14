package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;

/**
 * Manages adaptation policies and strategy generation.
 */
public class AdaptationPolicyManager {
    
    /**
     * Generates adaptation strategy based on pattern change.
     * @param change Pattern change detected
     * @param metrics Current node metrics
     * @param config Node configuration
     * @return Adaptation strategy or null if no action needed
     */
    public AdaptationStrategy generateStrategy(PatternChange change, NodeMetrics metrics, NodeConfiguration config) {
        switch (change.getType()) {
            case TRAFFIC_SPIKE:
                return new AdaptationStrategy(
                    "Scale up resources for traffic spike on " + change.getNodeId(),
                    0.8, // High impact
                    0.9  // High urgency
                );
                
            case TRAFFIC_DROP:
                return new AdaptationStrategy(
                    "Scale down resources for traffic drop on " + change.getNodeId(),
                    0.6, // Medium impact
                    0.4  // Low urgency
                );
                
            case TRANSACTION_BURST:
                return new AdaptationStrategy(
                    "Optimize transaction processing for burst on " + change.getNodeId(),
                    0.9, // Very high impact
                    0.8  // High urgency
                );
                
            case TRANSACTION_DECLINE:
                return new AdaptationStrategy(
                    "Rebalance transaction load from " + change.getNodeId(),
                    0.5, // Medium impact
                    0.3  // Low urgency
                );
                
            default:
                return null;
        }
    }
}