package com.telecom.distributed.core.model;

/**
 * Strategies for migrating services between nodes while preserving availability.
 */
public enum MigrationStrategy {
    LIVE_MIGRATION("Migrate service without downtime using state transfer"),
    COLD_MIGRATION("Stop service, migrate, then restart on target node"),
    WARM_MIGRATION("Prepare target node, brief pause for final state transfer"),
    GRADUAL_MIGRATION("Gradually redirect traffic to target node"),
    REPLICATED_MIGRATION("Create replica on target, then switch primary");

    private final String description;

    MigrationStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean preservesAvailability() {
        return this == LIVE_MIGRATION || this == GRADUAL_MIGRATION || this == REPLICATED_MIGRATION;
    }

    public boolean requiresDowntime() {
        return this == COLD_MIGRATION;
    }

    public boolean hasMinimalDowntime() {
        return this == WARM_MIGRATION;
    }
}