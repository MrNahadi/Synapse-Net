package com.telecom.distributed.core.model;

/**
 * Status of a service in the distributed system.
 */
public enum ServiceStatus {
    ACTIVE("Service is running and available"),
    INACTIVE("Service is stopped or unavailable"),
    MIGRATING("Service is being migrated to another node"),
    STARTING("Service is starting up"),
    STOPPING("Service is shutting down"),
    FAILED("Service has failed and needs attention"),
    MAINTENANCE("Service is under maintenance");

    private final String description;

    ServiceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOperational() {
        return this == ACTIVE;
    }

    public boolean isTransitioning() {
        return this == MIGRATING || this == STARTING || this == STOPPING;
    }

    public boolean requiresIntervention() {
        return this == FAILED;
    }
}