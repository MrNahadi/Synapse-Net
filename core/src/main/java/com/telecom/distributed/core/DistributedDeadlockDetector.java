package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Distributed deadlock detection system using wait-for graph analysis.
 * Implements cycle detection across multiple nodes to identify deadlocks.
 */
public class DistributedDeadlockDetector {
    private static final Logger logger = LoggerFactory.getLogger(DistributedDeadlockDetector.class);
    
    private final Map<TransactionId, Set<TransactionId>> waitForGraph;
    private final Map<TransactionId, Set<String>> transactionResources;
    private final Map<String, TransactionId> resourceOwners;
    private final ReentrantReadWriteLock graphLock;
    private final long deadlockTimeoutMs;
    
    public DistributedDeadlockDetector(long deadlockTimeoutMs) {
        this.waitForGraph = new ConcurrentHashMap<>();
        this.transactionResources = new ConcurrentHashMap<>();
        this.resourceOwners = new ConcurrentHashMap<>();
        this.graphLock = new ReentrantReadWriteLock();
        this.deadlockTimeoutMs = deadlockTimeoutMs;
    }
    
    /**
     * Records that a transaction is waiting for a resource held by another transaction.
     */
    public void recordWaitFor(TransactionId waiter, TransactionId holder, String resourceId) {
        graphLock.writeLock().lock();
        try {
            waitForGraph.computeIfAbsent(waiter, k -> new HashSet<>()).add(holder);
            transactionResources.computeIfAbsent(waiter, k -> new HashSet<>()).add(resourceId);
            resourceOwners.put(resourceId, holder);
            
            logger.debug("Transaction {} waiting for {} (resource: {})", waiter, holder, resourceId);
        } finally {
            graphLock.writeLock().unlock();
        }
    }
    
    /**
     * Records that a transaction has acquired a resource.
     */
    public void recordResourceAcquisition(TransactionId transaction, String resourceId) {
        graphLock.writeLock().lock();
        try {
            resourceOwners.put(resourceId, transaction);
            transactionResources.computeIfAbsent(transaction, k -> new HashSet<>()).add(resourceId);
            
            logger.debug("Transaction {} acquired resource {}", transaction, resourceId);
        } finally {
            graphLock.writeLock().unlock();
        }
    }
    
    /**
     * Records that a transaction has released a resource.
     */
    public void recordResourceRelease(TransactionId transaction, String resourceId) {
        graphLock.writeLock().lock();
        try {
            resourceOwners.remove(resourceId);
            Set<String> txResources = transactionResources.get(transaction);
            if (txResources != null) {
                txResources.remove(resourceId);
                if (txResources.isEmpty()) {
                    transactionResources.remove(transaction);
                }
            }
            
            logger.debug("Transaction {} released resource {}", transaction, resourceId);
        } finally {
            graphLock.writeLock().unlock();
        }
    }
    
    /**
     * Removes all records for a completed transaction.
     */
    public void removeTransaction(TransactionId transaction) {
        graphLock.writeLock().lock();
        try {
            // Remove from wait-for graph
            waitForGraph.remove(transaction);
            waitForGraph.values().forEach(waitSet -> waitSet.remove(transaction));
            
            // Release all resources
            Set<String> resources = transactionResources.remove(transaction);
            if (resources != null) {
                resources.forEach(resourceOwners::remove);
            }
            
            logger.debug("Removed transaction {} from deadlock detector", transaction);
        } finally {
            graphLock.writeLock().unlock();
        }
    }
    
    /**
     * Detects deadlocks by finding cycles in the wait-for graph.
     * @return Set of transactions involved in deadlocks
     */
    public Set<TransactionId> detectDeadlocks() {
        graphLock.readLock().lock();
        try {
            Set<TransactionId> deadlockedTransactions = new HashSet<>();
            Set<TransactionId> visited = new HashSet<>();
            Set<TransactionId> recursionStack = new HashSet<>();
            
            for (TransactionId transaction : waitForGraph.keySet()) {
                if (!visited.contains(transaction)) {
                    if (hasCycle(transaction, visited, recursionStack, deadlockedTransactions)) {
                        logger.warn("Deadlock detected involving transaction {}", transaction);
                    }
                }
            }
            
            return deadlockedTransactions;
        } finally {
            graphLock.readLock().unlock();
        }
    }
    
    /**
     * DFS-based cycle detection in wait-for graph.
     */
    private boolean hasCycle(TransactionId current, Set<TransactionId> visited, 
                            Set<TransactionId> recursionStack, Set<TransactionId> deadlocked) {
        visited.add(current);
        recursionStack.add(current);
        
        Set<TransactionId> neighbors = waitForGraph.get(current);
        if (neighbors != null) {
            for (TransactionId neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    if (hasCycle(neighbor, visited, recursionStack, deadlocked)) {
                        deadlocked.add(current);
                        return true;
                    }
                } else if (recursionStack.contains(neighbor)) {
                    // Cycle detected
                    deadlocked.add(current);
                    deadlocked.add(neighbor);
                    return true;
                }
            }
        }
        
        recursionStack.remove(current);
        return false;
    }
    
    /**
     * Resolves deadlock by selecting victim transaction(s) to abort.
     * Uses youngest transaction first strategy to minimize work lost.
     * @param deadlockedTransactions Set of transactions in deadlock
     * @return Transaction selected as victim to abort
     */
    public TransactionId selectVictim(Set<TransactionId> deadlockedTransactions, 
                                     Map<TransactionId, Long> transactionStartTimes) {
        if (deadlockedTransactions.isEmpty()) {
            return null;
        }
        
        // Select youngest transaction (started most recently) as victim
        TransactionId victim = deadlockedTransactions.stream()
            .max(Comparator.comparing(tx -> transactionStartTimes.getOrDefault(tx, 0L)))
            .orElse(deadlockedTransactions.iterator().next());
        
        logger.info("Selected transaction {} as deadlock victim", victim);
        return victim;
    }
    
    /**
     * Checks if a transaction has exceeded its timeout threshold.
     * @param transaction Transaction to check
     * @param startTime Transaction start time in milliseconds
     * @return true if transaction has timed out
     */
    public boolean isTimedOut(TransactionId transaction, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        boolean timedOut = elapsed > deadlockTimeoutMs;
        
        if (timedOut) {
            logger.warn("Transaction {} timed out after {}ms (threshold: {}ms)", 
                       transaction, elapsed, deadlockTimeoutMs);
        }
        
        return timedOut;
    }
    
    /**
     * Performs recovery after deadlock resolution by cleaning up victim transaction.
     * @param victim Transaction that was aborted to resolve deadlock
     */
    public void performRecovery(TransactionId victim) {
        graphLock.writeLock().lock();
        try {
            // Remove victim from wait-for graph
            removeTransaction(victim);
            
            logger.info("Recovery completed for aborted transaction {}", victim);
        } finally {
            graphLock.writeLock().unlock();
        }
    }
    
    /**
     * Gets all transactions currently in the wait-for graph.
     * @return Set of active transactions
     */
    public Set<TransactionId> getActiveTransactions() {
        graphLock.readLock().lock();
        try {
            return new HashSet<>(waitForGraph.keySet());
        } finally {
            graphLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the wait-for dependencies for a specific transaction.
     * @param transaction Transaction to query
     * @return Set of transactions this transaction is waiting for
     */
    public Set<TransactionId> getWaitingFor(TransactionId transaction) {
        graphLock.readLock().lock();
        try {
            Set<TransactionId> waiting = waitForGraph.get(transaction);
            return waiting != null ? new HashSet<>(waiting) : new HashSet<>();
        } finally {
            graphLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the resources held by a transaction.
     * @param transaction Transaction to query
     * @return Set of resource IDs held by the transaction
     */
    public Set<String> getHeldResources(TransactionId transaction) {
        graphLock.readLock().lock();
        try {
            Set<String> resources = transactionResources.get(transaction);
            return resources != null ? new HashSet<>(resources) : new HashSet<>();
        } finally {
            graphLock.readLock().unlock();
        }
    }
}
