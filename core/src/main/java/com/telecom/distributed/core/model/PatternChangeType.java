package com.telecom.distributed.core.model;

/**
 * Types of pattern changes that can be detected in the system.
 */
public enum PatternChangeType {
    TRAFFIC_SPIKE,
    TRAFFIC_DROP,
    TRANSACTION_BURST,
    TRANSACTION_DECLINE,
    BURST,
    STEADY,
    DECLINING
}