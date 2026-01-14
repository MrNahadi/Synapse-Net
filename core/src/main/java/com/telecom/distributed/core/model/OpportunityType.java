package com.telecom.distributed.core.model;

/**
 * Types of optimization opportunities.
 */
public enum OpportunityType {
    CPU_UNDERUTILIZATION,
    MEMORY_UNDERUTILIZATION,
    THROUGHPUT_BOTTLENECK,
    LOCK_CONTENTION,
    NETWORK_INEFFICIENCY
}