package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Core interface for managing fault tolerance across the distributed telecom system.
 * Handles detection, recovery, and prevention of various failure modes including
 * crash, omission, and Byzantine failures.
 */
public interface FaultToleranceManager {
    
    /**
     * Detects a failure on a specific node and initiates appropriate response.
     * @param node Node where failure was detected
     * @param type Type of failure detected
     */
    void detectFailure(NodeId node, FailureType type);
    
    /**
     * Initiates recovery procedures for a failed node.
     * @param failedNode Node that has failed and needs recovery
     * @return Future that completes when recovery is initiated
     */
    CompletableFuture<Void> initiateRecovery(NodeId failedNode);
    
    /**
     * Handles Byzantine failure detection and response.
     * @param suspectedNode Node suspected of Byzantine behavior
     * @param evidence Evidence supporting the Byzantine failure suspicion
     */
    void handleByzantineFailure(NodeId suspectedNode, ByzantineEvidence evidence);
    
    /**
     * Gets the appropriate replication strategy for a service type.
     * @param service Type of service requiring replication
     * @return Replication strategy optimized for the service
     */
    ReplicationStrategy getReplicationStrategy(ServiceType service);
    
    /**
     * Prevents cascading failures by isolating failed components.
     * @param failedNodes Set of nodes that have already failed
     * @param riskThreshold Threshold for determining cascade risk
     */
    void preventCascadingFailure(Set<NodeId> failedNodes, double riskThreshold);
    
    /**
     * Monitors system health and detects potential failure conditions.
     * @return Current system health assessment
     */
    SystemHealthAssessment assessSystemHealth();
    
    /**
     * Handles omission failures by implementing retry and alternative routing.
     * @param node Node experiencing omission failures
     * @param missedMessages Set of messages that were not delivered
     */
    void handleOmissionFailure(NodeId node, Set<MessageId> missedMessages);
    
    /**
     * Handles crash failures by implementing immediate failover.
     * @param node Node that has crashed
     * @return Future that completes when failover is complete
     */
    CompletableFuture<Void> handleCrashFailure(NodeId node);
    
    /**
     * Registers a failure detector for monitoring node health.
     * @param detector Failure detector to register
     */
    void registerFailureDetector(FailureDetector detector);
    
    /**
     * Gets the current failure tolerance configuration.
     * @return Current fault tolerance configuration
     */
    FaultToleranceConfiguration getConfiguration();
}