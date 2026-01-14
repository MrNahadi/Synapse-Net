package com.telecom.distributed.core.model;

/**
 * Status of a migration operation.
 */
public enum MigrationStatus {
    PLANNED("Migration is planned but not yet started"),
    IN_PROGRESS("Migration is currently executing"),
    COMPLETED("Migration completed successfully"),
    FAILED("Migration failed and needs intervention"),
    CANCELLED("Migration was cancelled before completion"),
    ROLLBACK("Migration is being rolled back due to issues");

    private final String description;

    MigrationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == IN_PROGRESS || this == ROLLBACK;
    }
}