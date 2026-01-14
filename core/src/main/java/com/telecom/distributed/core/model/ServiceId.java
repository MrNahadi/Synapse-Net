package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Unique identifier for services in the distributed telecom system.
 */
public class ServiceId {
    private final String id;

    public ServiceId(String id) {
        this.id = Objects.requireNonNull(id, "Service ID cannot be null");
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("Service ID cannot be empty");
        }
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceId serviceId = (ServiceId) o;
        return Objects.equals(id, serviceId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ServiceId{" + id + '}';
    }
}