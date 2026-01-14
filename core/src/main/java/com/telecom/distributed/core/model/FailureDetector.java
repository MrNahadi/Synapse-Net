package com.telecom.distributed.core.model;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for failure detection mechanisms.
 * Monitors node health and detects various types of failures.
 */
public interface FailureDetector {
    
    /**
     * Starts monitoring the specified node for failures.
     * @param nodeId Node to monitor
     * @return Future that completes when monitoring starts
     */
    CompletableFuture<Void> startMonitoring(NodeId nodeId);
    
    /**
     * Stops monitoring the specified node.
     * @param nodeId Node to stop monitoring
     * @return Future that completes when monitoring stops
     */
    CompletableFuture<Void> stopMonitoring(NodeId nodeId);
    
    /**
     * Checks if a node is currently suspected of failure.
     * @param nodeId Node to check
     * @return True if node is suspected of failure
     */
    boolean isSuspected(NodeId nodeId);
    
    /**
     * Gets the failure detection timeout for this detector.
     * @return Timeout in milliseconds
     */
    long getDetectionTimeoutMs();
    
    /**
     * Gets the type of failures this detector can identify.
     * @return Failure type this detector monitors
     */
    FailureType getDetectedFailureType();
    
    /**
     * Registers a callback for when failures are detected.
     * @param callback Callback to invoke when failure is detected
     */
    void onFailureDetected(FailureCallback callback);
    
    /**
     * Gets the current health status of a monitored node.
     * @param nodeId Node to check
     * @return Current health status
     */
    HealthStatus getNodeHealth(NodeId nodeId);
    
    /**
     * Callback interface for failure detection events.
     */
    @FunctionalInterface
    interface FailureCallback {
        void onFailure(NodeId failedNode, FailureType failureType, String details);
    }
}