package com.telecom.distributed.core.model;

/**
 * Result of a distributed transaction commit operation.
 */
public enum CommitResult {
    COMMITTED("Transaction successfully committed across all participants"),
    ABORTED("Transaction was aborted due to participant failure or conflict"),
    TIMEOUT("Transaction timed out during commit process"),
    BYZANTINE_FAILURE("Transaction failed due to Byzantine behavior from a participant");

    private final String description;

    CommitResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSuccessful() {
        return this == COMMITTED;
    }
}