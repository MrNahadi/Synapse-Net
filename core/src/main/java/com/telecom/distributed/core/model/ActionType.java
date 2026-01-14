package com.telecom.distributed.core.model;

/**
 * Types of optimization and constraint actions.
 */
public enum ActionType {
    INCREASE_WORKLOAD,
    REDUCE_WORKLOAD,
    OPTIMIZE_MEMORY_ALLOCATION,
    REDUCE_MEMORY_USAGE,
    THROTTLE_TRANSACTIONS,
    OPTIMIZE_NETWORK,
    REDUCE_LOCK_CONTENTION,
    MIGRATE_SERVICES,
    SCALE_RESOURCES,
    REBALANCE_LOAD
}