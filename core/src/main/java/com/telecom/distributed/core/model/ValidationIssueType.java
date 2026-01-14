package com.telecom.distributed.core.model;

/**
 * Types of validation issues in simulation results.
 */
public enum ValidationIssueType {
    THROUGHPUT_MISMATCH,
    LATENCY_MISMATCH,
    WIDE_CONFIDENCE_INTERVAL,
    INCONSISTENT_RESULTS
}