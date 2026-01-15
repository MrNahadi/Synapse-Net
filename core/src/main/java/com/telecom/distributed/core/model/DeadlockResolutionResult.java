package com.telecom.distributed.core.model;

import java.util.Set;

/**
 * Result of a deadlock resolution operation.
 */
public class DeadlockResolutionResult {
    private final Set<TransactionId> deadlockedTransactions;
    private final TransactionId victimTransaction;
    private final boolean resolved;
    private final long resolutionTimeMs;
    private final String resolutionStrategy;

    public DeadlockResolutionResult(Set<TransactionId> deadlockedTransactions,
                                   TransactionId victimTransaction,
                                   boolean resolved,
                                   long resolutionTimeMs,
                                   String resolutionStrategy) {
        this.deadlockedTransactions = deadlockedTransactions;
        this.victimTransaction = victimTransaction;
        this.resolved = resolved;
        this.resolutionTimeMs = resolutionTimeMs;
        this.resolutionStrategy = resolutionStrategy;
    }

    public Set<TransactionId> getDeadlockedTransactions() {
        return deadlockedTransactions;
    }

    public TransactionId getVictimTransaction() {
        return victimTransaction;
    }

    public boolean isResolved() {
        return resolved;
    }

    public long getResolutionTimeMs() {
        return resolutionTimeMs;
    }

    public String getResolutionStrategy() {
        return resolutionStrategy;
    }

    @Override
    public String toString() {
        return "DeadlockResolutionResult{" +
                "deadlockedTransactions=" + deadlockedTransactions +
                ", victimTransaction=" + victimTransaction +
                ", resolved=" + resolved +
                ", resolutionTimeMs=" + resolutionTimeMs +
                ", resolutionStrategy='" + resolutionStrategy + '\'' +
                '}';
    }
}
