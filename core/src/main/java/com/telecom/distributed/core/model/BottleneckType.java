package com.telecom.distributed.core.model;

/**
 * Types of performance bottlenecks in the distributed telecom system.
 */
public enum BottleneckType {
    CPU("CPU utilization bottleneck"),
    MEMORY("Memory usage bottleneck"),
    NETWORK_LATENCY("Network latency bottleneck"),
    NETWORK_THROUGHPUT("Network throughput bottleneck"),
    LOCK_CONTENTION("Lock contention bottleneck"),
    MIXED("Mixed performance bottleneck");

    private final String description;

    BottleneckType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}