package com.telecom.distributed.core.model;

/**
 * Types of resource constraint violations.
 */
public enum ConstraintType {
    CPU_UNDERUTILIZATION,
    CPU_OVERUTILIZATION,
    MEMORY_UNDERUTILIZATION,
    MEMORY_OVERUTILIZATION,
    TRANSACTION_OVERLOAD,
    NETWORK_CONGESTION
}