package com.telecom.distributed.core.model;

/**
 * Types of services supported by nodes in the distributed telecom system.
 */
public enum ServiceType {
    RPC_HANDLING("Remote procedure call processing"),
    RPC_CALL("Remote procedure call processing"),  // Alias
    DATA_REPLICATION("Data replication and consistency management"),
    REPLICATION("Data replication and consistency management"),  // Alias
    EVENT_ORDERING("Event ordering and sequencing"),
    TRANSACTION_COMMIT("Transaction commit coordination"),
    RECOVERY_OPERATIONS("Node failure recovery operations"),
    RECOVERY("Node failure recovery operations"),  // Alias
    LOAD_BALANCING("Dynamic load balancing and process allocation"),
    DEADLOCK_DETECTION("Distributed deadlock detection and resolution"),
    MIGRATION_SERVICES("Service migration and relocation"),
    MIGRATION("Service migration and relocation"),  // Alias
    ANALYTICS("Performance analytics and bottleneck analysis"),
    DISTRIBUTED_SHARED_MEMORY("Distributed shared memory management"),
    CRITICAL("Critical system services requiring high availability"),
    TRANSACTION_PROCESSING("Transaction processing and coordination");

    private final String description;

    ServiceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}