package com.telecom.distributed.core.model;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Represents a distributed transaction across multiple nodes in the telecom system.
 */
public class DistributedTransaction {
    private final TransactionId transactionId;
    private final Set<NodeId> participants;
    private volatile TransactionState state;
    private final long startTimestamp;
    private final long timeoutMs;
    private final Map<NodeId, PrepareResponse> prepareResponses;
    private final Set<ResourceLock> acquiredLocks;

    public DistributedTransaction(TransactionId transactionId, Set<NodeId> participants, long timeoutMs) {
        this.transactionId = transactionId;
        this.participants = new CopyOnWriteArraySet<>(participants);
        this.state = TransactionState.ACTIVE;
        this.startTimestamp = System.currentTimeMillis();
        this.timeoutMs = timeoutMs;
        this.prepareResponses = new ConcurrentHashMap<>();
        this.acquiredLocks = new CopyOnWriteArraySet<>();
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public Set<NodeId> getParticipants() {
        return participants;
    }

    public TransactionState getState() {
        return state;
    }

    public void setState(TransactionState state) {
        this.state = state;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isTimedOut() {
        return System.currentTimeMillis() - startTimestamp > timeoutMs;
    }

    public Map<NodeId, PrepareResponse> getPrepareResponses() {
        return prepareResponses;
    }

    public void addPrepareResponse(NodeId nodeId, PrepareResponse response) {
        prepareResponses.put(nodeId, response);
    }

    public boolean allParticipantsPrepared() {
        return prepareResponses.size() == participants.size() &&
               prepareResponses.values().stream().allMatch(PrepareResponse::isSuccess);
    }

    public Set<ResourceLock> getAcquiredLocks() {
        return acquiredLocks;
    }

    public void addLock(ResourceLock lock) {
        acquiredLocks.add(lock);
    }

    public void removeLock(ResourceLock lock) {
        acquiredLocks.remove(lock);
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTimestamp;
    }

    @Override
    public String toString() {
        return "DistributedTransaction{" +
                "transactionId=" + transactionId +
                ", participants=" + participants +
                ", state=" + state +
                ", startTimestamp=" + startTimestamp +
                ", timeoutMs=" + timeoutMs +
                ", elapsedTime=" + getElapsedTime() +
                '}';
    }
}