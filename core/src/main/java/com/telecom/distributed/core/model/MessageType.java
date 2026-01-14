package com.telecom.distributed.core.model;

/**
 * Types of messages in the distributed telecom system.
 */
public enum MessageType {
    RPC_REQUEST("Remote procedure call request"),
    RPC_RESPONSE("Remote procedure call response"),
    TRANSACTION_PREPARE("Transaction prepare phase message"),
    TRANSACTION_COMMIT("Transaction commit message"),
    TRANSACTION_ABORT("Transaction abort message"),
    HEARTBEAT("Node health heartbeat message"),
    FAILURE_NOTIFICATION("Node failure notification"),
    LOAD_BALANCING("Load balancing coordination message"),
    REPLICATION("Data replication message"),
    CONSENSUS("Consensus protocol message"),
    EVENT_ORDERING("Event ordering message");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}