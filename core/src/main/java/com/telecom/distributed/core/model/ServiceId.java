package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Unique identifier for services in the distributed telecom system.
 */
public class ServiceId {
    private final String id;
    
    // Standard service IDs
    public static final ServiceId RPC_HANDLER = new ServiceId("rpc-handler");
    public static final ServiceId REPLICATION_SERVICE = new ServiceId("replication-service");
    public static final ServiceId MIGRATION_SERVICE = new ServiceId("migration-service");
    public static final ServiceId RECOVERY_SERVICE = new ServiceId("recovery-service");
    public static final ServiceId TRANSACTION_COORDINATOR = new ServiceId("transaction-coordinator");
    public static final ServiceId LOAD_BALANCER_SERVICE = new ServiceId("load-balancer-service");
    public static final ServiceId DEADLOCK_DETECTOR = new ServiceId("deadlock-detector");
    public static final ServiceId ANALYTICS_SERVICE = new ServiceId("analytics-service");
    public static final ServiceId DISTRIBUTED_MEMORY = new ServiceId("distributed-memory");

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