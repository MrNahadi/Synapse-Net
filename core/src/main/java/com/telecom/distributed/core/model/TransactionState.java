package com.telecom.distributed.core.model;

/**
 * Represents the state of a distributed transaction in the 2PC/3PC protocol.
 */
public enum TransactionState {
    ACTIVE("Transaction is active and accepting operations"),
    PREPARING("Transaction is in prepare phase (2PC/3PC)"),
    PREPARED("All participants have prepared successfully"),
    COMMITTING("Transaction is in commit phase"),
    COMMITTED("Transaction has been successfully committed"),
    ABORTING("Transaction is being aborted"),
    ABORTED("Transaction has been aborted");

    private final String description;

    TransactionState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMMITTED || this == ABORTED;
    }

    public boolean canTransitionTo(TransactionState newState) {
        switch (this) {
            case ACTIVE:
                return newState == PREPARING || newState == ABORTING;
            case PREPARING:
                return newState == PREPARED || newState == ABORTING;
            case PREPARED:
                return newState == COMMITTING || newState == ABORTING;
            case COMMITTING:
                return newState == COMMITTED || newState == ABORTING;
            case ABORTING:
                return newState == ABORTED;
            case COMMITTED:
            case ABORTED:
                return false; // Terminal states
            default:
                return false;
        }
    }
}