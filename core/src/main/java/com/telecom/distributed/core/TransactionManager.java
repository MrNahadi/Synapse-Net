package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.Set;

/**
 * Core interface for managing distributed transactions in the telecom system.
 * Supports 2PC/3PC protocols, deadlock handling, and transaction coordination.
 */
public interface TransactionManager {
    
    /**
     * Begins a new distributed transaction.
     * @return Unique transaction identifier
     */
    TransactionId beginTransaction();
    
    /**
     * Prepares a transaction across all participating nodes.
     * @param txId Transaction identifier
     * @param participants Set of nodes participating in the transaction
     */
    void prepare(TransactionId txId, Set<NodeId> participants);
    
    /**
     * Commits a prepared transaction.
     * @param txId Transaction identifier
     * @return Result of the commit operation
     */
    CommitResult commit(TransactionId txId);
    
    /**
     * Aborts a transaction and rolls back changes.
     * @param txId Transaction identifier
     */
    void abort(TransactionId txId);
    
    /**
     * Handles deadlock resolution for a set of deadlocked transactions.
     * @param deadlockedTxs Set of transaction identifiers involved in deadlock
     */
    void handleDeadlock(Set<TransactionId> deadlockedTxs);
}