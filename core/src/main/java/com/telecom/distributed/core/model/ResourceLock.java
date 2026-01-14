package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Represents a distributed lock on a resource across nodes.
 */
public class ResourceLock {
    private final String resourceId;
    private final NodeId holderNodeId;
    private final TransactionId transactionId;
    private final LockType lockType;
    private final long acquiredTimestamp;

    public ResourceLock(String resourceId, NodeId holderNodeId, TransactionId transactionId, LockType lockType) {
        this.resourceId = Objects.requireNonNull(resourceId, "Resource ID cannot be null");
        this.holderNodeId = Objects.requireNonNull(holderNodeId, "Holder node ID cannot be null");
        this.transactionId = Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        this.lockType = Objects.requireNonNull(lockType, "Lock type cannot be null");
        this.acquiredTimestamp = System.currentTimeMillis();
    }

    public String getResourceId() {
        return resourceId;
    }

    public NodeId getHolderNodeId() {
        return holderNodeId;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public LockType getLockType() {
        return lockType;
    }

    public long getAcquiredTimestamp() {
        return acquiredTimestamp;
    }

    public long getHoldTime() {
        return System.currentTimeMillis() - acquiredTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceLock that = (ResourceLock) o;
        return Objects.equals(resourceId, that.resourceId) &&
               Objects.equals(holderNodeId, that.holderNodeId) &&
               Objects.equals(transactionId, that.transactionId) &&
               lockType == that.lockType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, holderNodeId, transactionId, lockType);
    }

    @Override
    public String toString() {
        return "ResourceLock{" +
                "resourceId='" + resourceId + '\'' +
                ", holderNodeId=" + holderNodeId +
                ", transactionId=" + transactionId +
                ", lockType=" + lockType +
                ", holdTime=" + getHoldTime() + "ms" +
                '}';
    }
}