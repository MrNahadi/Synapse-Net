package com.telecom.distributed.core.impl;

import com.telecom.distributed.core.CommunicationManager;
import com.telecom.distributed.core.TransactionManager;
import com.telecom.distributed.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of TransactionManager supporting 2PC/3PC protocols,
 * distributed locking, and deadlock detection.
 */
public class TransactionManagerImpl implements TransactionManager {
    private static final Logger logger = LoggerFactory.getLogger(TransactionManagerImpl.class);
    
    private final CommunicationManager communicationManager;
    private final Map<TransactionId, DistributedTransaction> activeTransactions;
    private final Map<String, ResourceLock> resourceLocks;
    private final ScheduledExecutorService timeoutExecutor;
    private final ExecutorService transactionExecutor;
    private final ReentrantReadWriteLock lockManagerLock;
    
    // Configuration
    private final long defaultTimeoutMs;
    private final int maxRetries;
    private final long deadlockDetectionIntervalMs;

    public TransactionManagerImpl(CommunicationManager communicationManager) {
        this(communicationManager, 30000, 3, 5000); // 30s timeout, 3 retries, 5s deadlock detection
    }

    public TransactionManagerImpl(CommunicationManager communicationManager, 
                                long defaultTimeoutMs, 
                                int maxRetries, 
                                long deadlockDetectionIntervalMs) {
        this.communicationManager = communicationManager;
        this.activeTransactions = new ConcurrentHashMap<>();
        this.resourceLocks = new ConcurrentHashMap<>();
        this.timeoutExecutor = Executors.newScheduledThreadPool(2);
        this.transactionExecutor = Executors.newCachedThreadPool();
        this.lockManagerLock = new ReentrantReadWriteLock();
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.maxRetries = maxRetries;
        this.deadlockDetectionIntervalMs = deadlockDetectionIntervalMs;
        
        // Start periodic deadlock detection
        startDeadlockDetection();
    }

    @Override
    public TransactionId beginTransaction() {
        TransactionId txId = TransactionId.generate();
        DistributedTransaction transaction = new DistributedTransaction(
            txId, new HashSet<>(), defaultTimeoutMs);
        
        activeTransactions.put(txId, transaction);
        
        // Schedule timeout
        timeoutExecutor.schedule(() -> {
            if (activeTransactions.containsKey(txId)) {
                logger.warn("Transaction {} timed out, aborting", txId);
                abort(txId);
            }
        }, defaultTimeoutMs, TimeUnit.MILLISECONDS);
        
        logger.info("Started transaction {}", txId);
        return txId;
    }

    @Override
    public void prepare(TransactionId txId, Set<NodeId> participants) {
        DistributedTransaction transaction = activeTransactions.get(txId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + txId);
        }
        
        if (transaction.isTimedOut()) {
            abort(txId);
            throw new IllegalStateException("Transaction timed out: " + txId);
        }
        
        if (!transaction.getState().canTransitionTo(TransactionState.PREPARING)) {
            throw new IllegalStateException("Cannot prepare transaction in state: " + transaction.getState());
        }
        
        transaction.setState(TransactionState.PREPARING);
        transaction.getParticipants().addAll(participants);
        
        logger.info("Preparing transaction {} with participants: {}", txId, participants);
        
        // Send prepare messages to all participants
        CompletableFuture<Void> preparePhase = CompletableFuture.allOf(
            participants.stream()
                .map(nodeId -> sendPrepareMessage(transaction, nodeId))
                .toArray(CompletableFuture[]::new)
        );
        
        try {
            preparePhase.get(defaultTimeoutMs / 2, TimeUnit.MILLISECONDS);
            
            if (transaction.allParticipantsPrepared()) {
                transaction.setState(TransactionState.PREPARED);
                logger.info("Transaction {} prepared successfully", txId);
            } else {
                logger.warn("Transaction {} prepare failed, aborting", txId);
                abort(txId);
            }
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            logger.error("Prepare phase failed for transaction {}", txId, e);
            abort(txId);
        }
    }

    @Override
    public CommitResult commit(TransactionId txId) {
        DistributedTransaction transaction = activeTransactions.get(txId);
        if (transaction == null) {
            return CommitResult.ABORTED;
        }
        
        if (transaction.isTimedOut()) {
            abort(txId);
            return CommitResult.TIMEOUT;
        }
        
        if (transaction.getState() != TransactionState.PREPARED) {
            logger.warn("Cannot commit transaction {} in state: {}", txId, transaction.getState());
            abort(txId);
            return CommitResult.ABORTED;
        }
        
        transaction.setState(TransactionState.COMMITTING);
        logger.info("Committing transaction {}", txId);
        
        try {
            // Send commit messages to all participants
            CompletableFuture<Void> commitPhase = CompletableFuture.allOf(
                transaction.getParticipants().stream()
                    .map(nodeId -> sendCommitMessage(transaction, nodeId))
                    .toArray(CompletableFuture[]::new)
            );
            
            commitPhase.get(defaultTimeoutMs / 2, TimeUnit.MILLISECONDS);
            
            transaction.setState(TransactionState.COMMITTED);
            releaseTransactionLocks(transaction);
            activeTransactions.remove(txId);
            
            logger.info("Transaction {} committed successfully", txId);
            return CommitResult.COMMITTED;
            
        } catch (Exception e) {
            logger.error("Commit phase failed for transaction {}", txId, e);
            abort(txId);
            return CommitResult.ABORTED;
        }
    }

    @Override
    public void abort(TransactionId txId) {
        DistributedTransaction transaction = activeTransactions.get(txId);
        if (transaction == null) {
            return;
        }
        
        if (transaction.getState().isTerminal()) {
            return;
        }
        
        transaction.setState(TransactionState.ABORTING);
        logger.info("Aborting transaction {}", txId);
        
        try {
            // Send abort messages to all participants
            CompletableFuture<Void> abortPhase = CompletableFuture.allOf(
                transaction.getParticipants().stream()
                    .map(nodeId -> sendAbortMessage(transaction, nodeId))
                    .toArray(CompletableFuture[]::new)
            );
            
            abortPhase.get(5000, TimeUnit.MILLISECONDS); // Shorter timeout for abort
            
        } catch (Exception e) {
            logger.warn("Error during abort phase for transaction {}", txId, e);
        } finally {
            transaction.setState(TransactionState.ABORTED);
            releaseTransactionLocks(transaction);
            activeTransactions.remove(txId);
            logger.info("Transaction {} aborted", txId);
        }
    }

    @Override
    public void handleDeadlock(Set<TransactionId> deadlockedTxs) {
        if (deadlockedTxs.isEmpty()) {
            return;
        }
        
        logger.warn("Handling deadlock involving transactions: {}", deadlockedTxs);
        
        // Choose victim transaction (youngest transaction)
        TransactionId victim = deadlockedTxs.stream()
            .map(activeTransactions::get)
            .filter(Objects::nonNull)
            .min(Comparator.comparing(DistributedTransaction::getStartTimestamp))
            .map(DistributedTransaction::getTransactionId)
            .orElse(deadlockedTxs.iterator().next());
        
        logger.info("Selected transaction {} as deadlock victim", victim);
        abort(victim);
    }

    /**
     * Acquires a distributed lock for a transaction.
     */
    public boolean acquireLock(TransactionId txId, String resourceId, NodeId nodeId, LockType lockType) {
        lockManagerLock.writeLock().lock();
        try {
            ResourceLock existingLock = resourceLocks.get(resourceId);
            
            if (existingLock != null) {
                // Check if lock is compatible
                if (!lockType.isCompatibleWith(existingLock.getLockType())) {
                    return false; // Lock conflict
                }
                
                // Check if same transaction already holds the lock
                if (existingLock.getTransactionId().equals(txId)) {
                    return true; // Already holds compatible lock
                }
            }
            
            ResourceLock newLock = new ResourceLock(resourceId, nodeId, txId, lockType);
            resourceLocks.put(resourceId, newLock);
            
            DistributedTransaction transaction = activeTransactions.get(txId);
            if (transaction != null) {
                transaction.addLock(newLock);
            }
            
            logger.debug("Acquired {} lock on resource {} for transaction {}", lockType, resourceId, txId);
            return true;
            
        } finally {
            lockManagerLock.writeLock().unlock();
        }
    }

    /**
     * Releases all locks held by a transaction.
     */
    private void releaseTransactionLocks(DistributedTransaction transaction) {
        lockManagerLock.writeLock().lock();
        try {
            for (ResourceLock lock : transaction.getAcquiredLocks()) {
                resourceLocks.remove(lock.getResourceId());
                logger.debug("Released lock on resource {} for transaction {}", 
                    lock.getResourceId(), transaction.getTransactionId());
            }
            transaction.getAcquiredLocks().clear();
        } finally {
            lockManagerLock.writeLock().unlock();
        }
    }

    /**
     * Detects deadlocks using wait-for graph analysis.
     */
    private void startDeadlockDetection() {
        timeoutExecutor.scheduleAtFixedRate(() -> {
            try {
                Set<TransactionId> deadlockedTransactions = detectDeadlocks();
                if (!deadlockedTransactions.isEmpty()) {
                    handleDeadlock(deadlockedTransactions);
                }
            } catch (Exception e) {
                logger.error("Error during deadlock detection", e);
            }
        }, deadlockDetectionIntervalMs, deadlockDetectionIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Simple deadlock detection based on lock wait times.
     */
    private Set<TransactionId> detectDeadlocks() {
        Set<TransactionId> suspectedDeadlocks = new HashSet<>();
        long currentTime = System.currentTimeMillis();
        
        lockManagerLock.readLock().lock();
        try {
            for (DistributedTransaction transaction : activeTransactions.values()) {
                if (transaction.getState() == TransactionState.PREPARING && 
                    currentTime - transaction.getStartTimestamp() > defaultTimeoutMs / 2) {
                    suspectedDeadlocks.add(transaction.getTransactionId());
                }
            }
        } finally {
            lockManagerLock.readLock().unlock();
        }
        
        return suspectedDeadlocks;
    }

    private CompletableFuture<Void> sendPrepareMessage(DistributedTransaction transaction, NodeId nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate prepare message sending and response
                Message prepareMsg = createPrepareMessage(transaction.getTransactionId(), nodeId);
                CompletableFuture<Message> response = communicationManager.sendRPC(nodeId, 
                    new RPCRequest("prepare", new Object[]{prepareMsg.getPayload()}, 5000, 3));
                
                Message responseMsg = response.get(5000, TimeUnit.MILLISECONDS);
                PrepareResponse prepareResponse = parseResponse(responseMsg, nodeId);
                transaction.addPrepareResponse(nodeId, prepareResponse);
                
                return null;
            } catch (Exception e) {
                logger.error("Failed to send prepare message to {}", nodeId, e);
                transaction.addPrepareResponse(nodeId, PrepareResponse.failure(nodeId, e.getMessage()));
                return null;
            }
        }, transactionExecutor);
    }

    private CompletableFuture<Void> sendCommitMessage(DistributedTransaction transaction, NodeId nodeId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Message commitMsg = createCommitMessage(transaction.getTransactionId(), nodeId);
                communicationManager.sendRPC(nodeId, new RPCRequest("commit", new Object[]{commitMsg.getPayload()}, 5000, 3))
                    .get(5000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.error("Failed to send commit message to {}", nodeId, e);
            }
        }, transactionExecutor);
    }

    private CompletableFuture<Void> sendAbortMessage(DistributedTransaction transaction, NodeId nodeId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Message abortMsg = createAbortMessage(transaction.getTransactionId(), nodeId);
                communicationManager.sendRPC(nodeId, new RPCRequest("abort", new Object[]{abortMsg.getPayload()}, 3000, 3))
                    .get(3000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.error("Failed to send abort message to {}", nodeId, e);
            }
        }, transactionExecutor);
    }

    private Message createPrepareMessage(TransactionId txId, NodeId targetNode) {
        return new Message(
            MessageId.generate(),
            NodeId.CORE1, // Assuming coordinator is Core1
            targetNode,
            MessageType.TRANSACTION_PREPARE,
            txId.getId().getBytes(),
            System.currentTimeMillis(),
            1 // High priority for transaction messages
        );
    }

    private Message createCommitMessage(TransactionId txId, NodeId targetNode) {
        return new Message(
            MessageId.generate(),
            NodeId.CORE1,
            targetNode,
            MessageType.TRANSACTION_COMMIT,
            txId.getId().getBytes(),
            System.currentTimeMillis(),
            1
        );
    }

    private Message createAbortMessage(TransactionId txId, NodeId targetNode) {
        return new Message(
            MessageId.generate(),
            NodeId.CORE1,
            targetNode,
            MessageType.TRANSACTION_ABORT,
            txId.getId().getBytes(),
            System.currentTimeMillis(),
            1
        );
    }

    private PrepareResponse parseResponse(Message responseMsg, NodeId nodeId) {
        // Simple response parsing - in real implementation would use proper serialization
        String response = new String(responseMsg.getPayload());
        boolean success = response.contains("SUCCESS");
        return success ? PrepareResponse.success(nodeId) : PrepareResponse.failure(nodeId, response);
    }

    /**
     * Gets current transaction statistics.
     */
    public TransactionStatistics getStatistics() {
        lockManagerLock.readLock().lock();
        try {
            int activeCount = activeTransactions.size();
            int locksHeld = resourceLocks.size();
            long avgTransactionTime = activeTransactions.values().stream()
                .mapToLong(DistributedTransaction::getElapsedTime)
                .sum() / Math.max(1, activeCount);
            
            return new TransactionStatistics(activeCount, locksHeld, avgTransactionTime);
        } finally {
            lockManagerLock.readLock().unlock();
        }
    }

    public void shutdown() {
        timeoutExecutor.shutdown();
        transactionExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
            if (!transactionExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                transactionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Statistics about transaction manager performance.
     */
    public static class TransactionStatistics {
        private final int activeTransactions;
        private final int locksHeld;
        private final long averageTransactionTime;

        public TransactionStatistics(int activeTransactions, int locksHeld, long averageTransactionTime) {
            this.activeTransactions = activeTransactions;
            this.locksHeld = locksHeld;
            this.averageTransactionTime = averageTransactionTime;
        }

        public int getActiveTransactions() { return activeTransactions; }
        public int getLocksHeld() { return locksHeld; }
        public long getAverageTransactionTime() { return averageTransactionTime; }
    }
}