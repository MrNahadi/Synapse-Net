package com.telecom.distributed.core.impl;

import com.telecom.distributed.core.*;
import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Enhanced transaction manager with bottleneck identification and optimized commit protocols.
 * Ensures atomicity and consistency under concurrent access with asymmetric failure handling.
 */
public class EnhancedTransactionManager implements TransactionManager {
    
    private final Map<TransactionId, DistributedTransaction> activeTransactions;
    private final Map<String, ResourceLock> lockRegistry;
    private final TransactionBottleneckAnalyzer bottleneckAnalyzer;
    private final DistributedCommitProtocol commitProtocol;
    private final CommunicationManager communicationManager;
    private final PerformanceAnalyzer performanceAnalyzer;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Configuration parameters
    private final long transactionTimeoutMs;
    private final long bottleneckAnalysisIntervalMs;
    private final int maxConcurrentTransactions;
    
    public EnhancedTransactionManager(
            CommunicationManager communicationManager,
            PerformanceAnalyzer performanceAnalyzer,
            Map<NodeId, Double> nodeFailureProbabilities) {
        
        this.activeTransactions = new ConcurrentHashMap<>();
        this.lockRegistry = new ConcurrentHashMap<>();
        this.bottleneckAnalyzer = new TransactionBottleneckAnalyzer();
        this.commitProtocol = new DistributedCommitProtocol(nodeFailureProbabilities);
        this.communicationManager = communicationManager;
        this.performanceAnalyzer = performanceAnalyzer;
        this.scheduledExecutor = Executors.newScheduledThreadPool(4);
        
        // Configuration
        this.transactionTimeoutMs = 30000; // 30 seconds
        this.bottleneckAnalysisIntervalMs = 5000; // 5 seconds
        this.maxConcurrentTransactions = 100;
        
        // Start background tasks
        startBottleneckMonitoring();
        startTransactionTimeoutMonitoring();
    }
    
    @Override
    public TransactionId beginTransaction() {
        // Check concurrent transaction limit
        if (activeTransactions.size() >= maxConcurrentTransactions) {
            throw new IllegalStateException("Maximum concurrent transactions exceeded: " + maxConcurrentTransactions);
        }
        
        TransactionId txId = TransactionId.generate();
        
        // Create transaction with default participants (will be updated during prepare)
        Set<NodeId> initialParticipants = Set.of(); // Empty initially
        DistributedTransaction transaction = new DistributedTransaction(
            txId, initialParticipants, transactionTimeoutMs);
        
        activeTransactions.put(txId, transaction);
        
        return txId;
    }
    
    @Override
    public void prepare(TransactionId txId, Set<NodeId> participants) {
        DistributedTransaction transaction = activeTransactions.get(txId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + txId);
        }
        
        if (transaction.getState() != TransactionState.ACTIVE) {
            throw new IllegalStateException("Transaction not in ACTIVE state: " + transaction.getState());
        }
        
        // Update participants
        transaction.getParticipants().addAll(participants);
        transaction.setState(TransactionState.PREPARING);
        
        // Identify current bottleneck nodes for optimized handling
        Set<NodeId> bottleneckNodes = identifyCurrentBottleneckNodes(participants);
        
        // Execute prepare phase asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                executePreparePhase(transaction, bottleneckNodes);
            } catch (Exception e) {
                transaction.setState(TransactionState.ABORTING);
                System.err.println("Prepare phase failed for transaction " + txId + ": " + e.getMessage());
            }
        }, scheduledExecutor);
    }
    
    @Override
    public CommitResult commit(TransactionId txId) {
        DistributedTransaction transaction = activeTransactions.get(txId);
        if (transaction == null) {
            return CommitResult.ABORTED; // Transaction not found
        }
        
        if (transaction.getState() != TransactionState.PREPARED) {
            return CommitResult.ABORTED; // Not prepared
        }
        
        try {
            // Identify bottleneck nodes for commit optimization
            Set<NodeId> bottleneckNodes = identifyCurrentBottleneckNodes(transaction.getParticipants());
            
            // Execute distributed commit protocol
            CompletableFuture<DistributedCommitProtocol.DistributedCommitResult> commitFuture = 
                commitProtocol.executeCommitProtocol(transaction, bottleneckNodes, communicationManager);
            
            DistributedCommitProtocol.DistributedCommitResult result = 
                commitFuture.get(transactionTimeoutMs, TimeUnit.MILLISECONDS);
            
            // Update transaction state based on result
            if (result.getResult() == CommitResult.COMMITTED) {
                transaction.setState(TransactionState.COMMITTED);
                releaseTransactionLocks(transaction);
                activeTransactions.remove(txId);
            } else {
                transaction.setState(TransactionState.ABORTED);
                releaseTransactionLocks(transaction);
                activeTransactions.remove(txId);
            }
            
            return result.getResult();
            
        } catch (TimeoutException e) {
            transaction.setState(TransactionState.ABORTED);
            releaseTransactionLocks(transaction);
            activeTransactions.remove(txId);
            return CommitResult.TIMEOUT;
        } catch (Exception e) {
            transaction.setState(TransactionState.ABORTED);
            releaseTransactionLocks(transaction);
            activeTransactions.remove(txId);
            return CommitResult.ABORTED;
        }
    }
    
    @Override
    public void abort(TransactionId txId) {
        DistributedTransaction transaction = activeTransactions.get(txId);
        if (transaction == null) {
            return; // Already aborted or doesn't exist
        }
        
        transaction.setState(TransactionState.ABORTING);
        
        // Release all locks held by this transaction
        releaseTransactionLocks(transaction);
        
        // Send abort messages to all participants
        sendAbortMessages(transaction);
        
        transaction.setState(TransactionState.ABORTED);
        activeTransactions.remove(txId);
    }
    
    @Override
    public void handleDeadlock(Set<TransactionId> deadlockedTxs) {
        if (deadlockedTxs.isEmpty()) {
            return;
        }
        
        // Select victim transaction(s) to abort
        Set<TransactionId> victimsToAbort = selectDeadlockVictims(deadlockedTxs);
        
        // Abort victim transactions
        for (TransactionId victimTxId : victimsToAbort) {
            abort(victimTxId);
        }
    }
    
    /**
     * Identifies current bottleneck nodes from the given participants.
     */
    private Set<NodeId> identifyCurrentBottleneckNodes(Set<NodeId> participants) {
        try {
            // Get current node metrics
            Map<NodeId, NodeMetrics> nodeMetrics = getCurrentNodeMetrics(participants);
            
            // Analyze transaction bottlenecks
            List<TransactionBottleneckAnalyzer.TransactionBottleneckResult> bottleneckResults = 
                bottleneckAnalyzer.identifyTransactionBottlenecks(nodeMetrics, activeTransactions, lockRegistry);
            
            // Return top bottleneck nodes (score > 0.5)
            return bottleneckResults.stream()
                .filter(result -> result.getBottleneckScore() > 0.5)
                .filter(result -> participants.contains(result.getNodeId()))
                .map(TransactionBottleneckAnalyzer.TransactionBottleneckResult::getNodeId)
                .collect(Collectors.toSet());
                
        } catch (Exception e) {
            // Fallback: return empty set if analysis fails
            System.err.println("Failed to identify bottleneck nodes: " + e.getMessage());
            return Set.of();
        }
    }
    
    /**
     * Executes prepare phase with bottleneck optimization.
     */
    private void executePreparePhase(DistributedTransaction transaction, Set<NodeId> bottleneckNodes) {
        // Acquire necessary locks
        acquireTransactionLocks(transaction);
        
        // Send prepare messages to participants
        Map<NodeId, CompletableFuture<PrepareResponse>> prepareRequests = new HashMap<>();
        
        for (NodeId participant : transaction.getParticipants()) {
            CompletableFuture<PrepareResponse> prepareRequest = sendPrepareMessage(
                transaction, participant, bottleneckNodes.contains(participant));
            prepareRequests.put(participant, prepareRequest);
        }
        
        // Collect prepare responses
        boolean allPrepared = true;
        for (Map.Entry<NodeId, CompletableFuture<PrepareResponse>> entry : prepareRequests.entrySet()) {
            try {
                PrepareResponse response = entry.getValue().get(transactionTimeoutMs, TimeUnit.MILLISECONDS);
                transaction.addPrepareResponse(entry.getKey(), response);
                
                if (!response.isSuccess()) {
                    allPrepared = false;
                }
            } catch (Exception e) {
                allPrepared = false;
                transaction.addPrepareResponse(entry.getKey(), 
                    PrepareResponse.failure(entry.getKey(), "Prepare timeout or failure"));
            }
        }
        
        // Update transaction state
        if (allPrepared) {
            transaction.setState(TransactionState.PREPARED);
        } else {
            transaction.setState(TransactionState.ABORTING);
            releaseTransactionLocks(transaction);
        }
    }
    
    /**
     * Acquires necessary locks for the transaction.
     */
    private void acquireTransactionLocks(DistributedTransaction transaction) {
        // Simplified lock acquisition - in real implementation would be more sophisticated
        for (NodeId participant : transaction.getParticipants()) {
            String resourceId = "node_resource_" + participant.getId();
            ResourceLock lock = new ResourceLock(
                resourceId, participant, transaction.getTransactionId(), LockType.EXCLUSIVE);
            
            lockRegistry.put(resourceId, lock);
            transaction.addLock(lock);
        }
    }
    
    /**
     * Releases all locks held by the transaction.
     */
    private void releaseTransactionLocks(DistributedTransaction transaction) {
        for (ResourceLock lock : transaction.getAcquiredLocks()) {
            lockRegistry.remove(lock.getResourceId());
            transaction.removeLock(lock);
        }
    }
    
    /**
     * Sends prepare message to a participant node.
     */
    private CompletableFuture<PrepareResponse> sendPrepareMessage(
            DistributedTransaction transaction, NodeId participant, boolean isBottleneckNode) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create prepare message
                Message prepareMessage = new Message(
                    MessageId.generate(),
                    new NodeId("Coordinator"),
                    participant,
                    MessageType.TRANSACTION_PREPARE,
                    serializeTransactionForPrepare(transaction),
                    System.currentTimeMillis(),
                    isBottleneckNode ? 2 : 1 // Higher priority for bottleneck nodes
                );
                
                // Send with appropriate timeout
                long timeout = isBottleneckNode ? transactionTimeoutMs * 2 : transactionTimeoutMs;
                CompletableFuture<Message> response = communicationManager.sendRPC(
                    participant, createRPCRequest(prepareMessage));
                
                Message responseMessage = response.get(timeout, TimeUnit.MILLISECONDS);
                return deserializePrepareResponse(responseMessage.getPayload());
                
            } catch (Exception e) {
                return PrepareResponse.failure(participant, "Prepare failed: " + e.getMessage());
            }
        }, scheduledExecutor);
    }
    
    /**
     * Sends abort messages to all participants.
     */
    private void sendAbortMessages(DistributedTransaction transaction) {
        for (NodeId participant : transaction.getParticipants()) {
            CompletableFuture.runAsync(() -> {
                try {
                    Message abortMessage = new Message(
                        MessageId.generate(),
                        new NodeId("Coordinator"),
                        participant,
                        MessageType.TRANSACTION_ABORT,
                        transaction.getTransactionId().getId().getBytes(),
                        System.currentTimeMillis(),
                        1
                    );
                    
                    communicationManager.sendRPC(participant, createRPCRequest(abortMessage));
                } catch (Exception e) {
                    System.err.println("Failed to send abort to " + participant + ": " + e.getMessage());
                }
            }, scheduledExecutor);
        }
    }
    
    /**
     * Selects victim transactions for deadlock resolution.
     */
    private Set<TransactionId> selectDeadlockVictims(Set<TransactionId> deadlockedTxs) {
        // Simple victim selection: abort youngest transaction
        return deadlockedTxs.stream()
            .map(activeTransactions::get)
            .filter(Objects::nonNull)
            .min(Comparator.comparing(DistributedTransaction::getStartTimestamp))
            .map(DistributedTransaction::getTransactionId)
            .map(Set::of)
            .orElse(Set.of());
    }
    
    /**
     * Gets current node metrics for the specified nodes.
     */
    private Map<NodeId, NodeMetrics> getCurrentNodeMetrics(Set<NodeId> nodes) {
        // Simplified implementation - would integrate with actual monitoring system
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        
        for (NodeId nodeId : nodes) {
            // Create sample metrics - in real implementation would fetch from monitoring
            NodeMetrics nodeMetrics = createSampleMetrics(nodeId);
            metrics.put(nodeId, nodeMetrics);
        }
        
        return metrics;
    }
    
    /**
     * Creates sample metrics for testing purposes.
     */
    private NodeMetrics createSampleMetrics(NodeId nodeId) {
        // Sample metrics based on node type
        if (nodeId.equals(NodeId.EDGE1)) {
            return new NodeMetrics(12.0, 500.0, 1.2, 45.0, 4.0, 150, 8.0);
        } else if (nodeId.equals(NodeId.EDGE2)) {
            return new NodeMetrics(15.0, 470.0, 1.8, 52.0, 4.5, 120, 10.0);
        } else if (nodeId.equals(NodeId.CORE1)) {
            return new NodeMetrics(8.0, 1000.0, 0.8, 60.0, 8.0, 250, 12.0);
        } else if (nodeId.equals(NodeId.CORE2)) {
            return new NodeMetrics(10.0, 950.0, 1.0, 55.0, 6.0, 200, 9.0);
        } else if (nodeId.equals(NodeId.CLOUD1)) {
            return new NodeMetrics(22.0, 1250.0, 2.0, 65.0, 16.0, 300, 5.0);
        } else {
            return new NodeMetrics(15.0, 750.0, 1.5, 50.0, 8.0, 175, 8.0);
        }
    }
    
    /**
     * Starts background bottleneck monitoring.
     */
    private void startBottleneckMonitoring() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                if (!activeTransactions.isEmpty()) {
                    Set<NodeId> allParticipants = activeTransactions.values().stream()
                        .flatMap(tx -> tx.getParticipants().stream())
                        .collect(Collectors.toSet());
                    
                    if (!allParticipants.isEmpty()) {
                        Map<NodeId, NodeMetrics> nodeMetrics = getCurrentNodeMetrics(allParticipants);
                        List<TransactionBottleneckAnalyzer.TransactionBottleneckResult> results = 
                            bottleneckAnalyzer.identifyTransactionBottlenecks(
                                nodeMetrics, activeTransactions, lockRegistry);
                        
                        // Log bottleneck information
                        results.stream()
                            .filter(result -> result.getBottleneckScore() > 0.5)
                            .forEach(result -> System.out.println("Bottleneck detected: " + result.getExplanation()));
                    }
                }
            } catch (Exception e) {
                System.err.println("Bottleneck monitoring failed: " + e.getMessage());
            }
        }, bottleneckAnalysisIntervalMs, bottleneckAnalysisIntervalMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Starts background transaction timeout monitoring.
     */
    private void startTransactionTimeoutMonitoring() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                Set<TransactionId> timedOutTransactions = activeTransactions.values().stream()
                    .filter(DistributedTransaction::isTimedOut)
                    .map(DistributedTransaction::getTransactionId)
                    .collect(Collectors.toSet());
                
                for (TransactionId txId : timedOutTransactions) {
                    abort(txId);
                }
            } catch (Exception e) {
                System.err.println("Transaction timeout monitoring failed: " + e.getMessage());
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS); // Check every second
    }
    
    // Helper methods for serialization
    private byte[] serializeTransactionForPrepare(DistributedTransaction transaction) {
        return transaction.toString().getBytes();
    }
    
    private PrepareResponse deserializePrepareResponse(byte[] payload) {
        String response = new String(payload);
        NodeId dummyNode = NodeId.EDGE1; // Placeholder node for deserialization
        return response.startsWith("SUCCESS") ? 
            PrepareResponse.success(dummyNode) :
            PrepareResponse.failure(dummyNode, response);
    }
    
    private RPCRequest createRPCRequest(Message message) {
        return new RPCRequest(message.getType().name(), new Object[]{message.getPayload()}, 5000, 3);
    }
    
    // Getters for testing and monitoring
    public Map<TransactionId, DistributedTransaction> getActiveTransactions() {
        return new HashMap<>(activeTransactions);
    }
    
    public Map<String, ResourceLock> getLockRegistry() {
        return new HashMap<>(lockRegistry);
    }
    
    public void shutdown() {
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}