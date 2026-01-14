package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Unique identifier for replication groups in the distributed system.
 */
public class GroupId {
    private final String id;

    public GroupId(String id) {
        this.id = Objects.requireNonNull(id, "Group ID cannot be null");
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("Group ID cannot be empty");
        }
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupId groupId = (GroupId) o;
        return Objects.equals(id, groupId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "GroupId{" + id + '}';
    }
}