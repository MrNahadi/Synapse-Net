package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Unique identifier for migration operations in the distributed system.
 */
public class MigrationId {
    private final String id;

    public MigrationId(String id) {
        this.id = Objects.requireNonNull(id, "Migration ID cannot be null");
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("Migration ID cannot be empty");
        }
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MigrationId that = (MigrationId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MigrationId{" + id + '}';
    }
}