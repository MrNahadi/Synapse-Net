package com.telecom.distributed.core.model;

/**
 * Status of optimization for a node.
 */
public enum OptimizationStatus {
    SUCCESS,
    OPTIMAL,
    GOOD,
    NEEDS_OPTIMIZATION,
    FAILED,
    UNKNOWN
}