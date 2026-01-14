package com.telecom.distributed.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the current location and status of a service in the distributed system.
 */
public class ServiceLocation {
    private final ServiceId serviceId;
    private final NodeId currentNode;
    private final ServiceStatus status;
    private final Instant lastUpdated;
    private final String endpoint;
    private final int version;

    public ServiceLocation(ServiceId serviceId, NodeId currentNode, ServiceStatus status,
                          Instant lastUpdated, String endpoint, int version) {
        this.serviceId = Objects.requireNonNull(serviceId, "Service ID cannot be null");
        this.currentNode = Objects.requireNonNull(currentNode, "Current node cannot be null");
        this.status = Objects.requireNonNull(status, "Service status cannot be null");
        this.lastUpdated = Objects.requireNonNull(lastUpdated, "Last updated cannot be null");
        this.endpoint = Objects.requireNonNull(endpoint, "Endpoint cannot be null");
        this.version = validateVersion(version);
    }

    private int validateVersion(int version) {
        if (version < 0) {
            throw new IllegalArgumentException("Version must be non-negative, got: " + version);
        }
        return version;
    }

    public boolean isAvailable() {
        return status == ServiceStatus.ACTIVE;
    }

    public boolean isMigrating() {
        return status == ServiceStatus.MIGRATING;
    }

    public boolean isStale(long maxAgeMs) {
        return Instant.now().toEpochMilli() - lastUpdated.toEpochMilli() > maxAgeMs;
    }

    public ServiceLocation withNewLocation(NodeId newNode, String newEndpoint) {
        return new ServiceLocation(serviceId, newNode, status, Instant.now(), newEndpoint, version + 1);
    }

    public ServiceLocation withStatus(ServiceStatus newStatus) {
        return new ServiceLocation(serviceId, currentNode, newStatus, Instant.now(), endpoint, version);
    }

    // Getters
    public ServiceId getServiceId() { return serviceId; }
    public NodeId getCurrentNode() { return currentNode; }
    public ServiceStatus getStatus() { return status; }
    public Instant getLastUpdated() { return lastUpdated; }
    public String getEndpoint() { return endpoint; }
    public int getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceLocation that = (ServiceLocation) o;
        return Objects.equals(serviceId, that.serviceId) &&
               Objects.equals(currentNode, that.currentNode) &&
               version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, currentNode, version);
    }

    @Override
    public String toString() {
        return "ServiceLocation{" +
               "serviceId=" + serviceId +
               ", currentNode=" + currentNode +
               ", status=" + status +
               ", endpoint='" + endpoint + '\'' +
               ", version=" + version +
               '}';
    }
}