package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Distributed commit/consensus protocol optimized for bottleneck nodes.
 * Implements asymmetric failure probability handling and maximizes transaction throughput.
 */
public class DistributedCommitProtocol {
    
    private final Map<NodeId, Double> nodeFailureProbabilities;
    private final ExecutorService executorService;
    private final long defaultTimeoutMs;
    private final int maxRetries;
    
    public DistributedCommitProtocol(Map<NodeId, Double> nodeFailureProbabilities) {
        this.nodeFailureProbabilities = new ConcurrentHashMap<>(nodeFailureProbabilities);
        this.executorService = Executors.newCachedThreadPool();
        this.defaultTimeoutMs = 10000; // 10 seconds default timeout
        this.maxRetries = 3;
    }
    
    /**
     * Executes distributed commit protocol optimized for bottleneck nodes.
     * @param transaction The distributed transaction to commit
     * @param bottleneckNodes Identified bottleneck nodes requiring special handling
     * @param communicationManager Communication manager for inter-node messaging
     * @return Commit result with detailed information
     */
    public CompletableFuture<DistributedCommitResult> executeCommitProtocol(
            DistributedTransaction transaction,
            Set<NodeId> bottleneckNodes,
            CommunicationManager communicationManager) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Phase 1: Prepare phase with asymmetric handling
                PreparePhaseResult prepareResult = executePreparePhase(
                    transaction, bottleneckNodes, communicationManager);
                
                if (!prepareResult.isSuccess()) {
                    // Abort transaction
                    executeAbortPhase(transaction, communicationManager);
                    return new DistributedCommitResult(
                        transaction.getTransactionId(), 
                        CommitResult.ABORTED, 
                        prepareResult.getFailureReason(),
                        prepareResult.getPhaseMetrics());
                }
                
                // Phase 2: Commit phase with bottleneck optimization
                CommitPhaseResult commitResult = executeCommitPhase(
                    transaction, bottleneckNodes, communicationManager);
                
                CommitResult finalResult = commitResult.isSuccess() ? 
                    CommitResult.COMMITTED : CommitResult.ABORTED;
                
                return new DistributedCommitResult(
                    transaction.getTransactionId(),
                    finalResult,
                    commitResult.getDetails(),
                    mergePhaseMetrics(prepareResult.getPhaseMetrics(), commitResult.getPhaseMetrics()));
                    
            } catch (Exception e) {
                // Emergency abort
                executeAbortPhase(transaction, communicationManager);
                return new DistributedCommitResult(
                    transaction.getTransactionId(),
                    CommitResult.ABORTED,
                    "Exception during commit protocol: " + e.getMessage(),
                    new CommitPhaseMetrics());
            }
        }, executorService);
    }
    
    /**
     * Executes prepare phase with asymmetric failure probability handling.
     */
    private PreparePhaseResult executePreparePhase(
            DistributedTransaction transaction,
            Set<NodeId> bottleneckNodes,
            CommunicationManager communicationManager) {
        
        long startTime = System.currentTimeMillis();
        Set<NodeId> participants = transaction.getParticipants();
        Map<NodeId, CompletableFuture<PrepareResponse>> prepareRequests = new ConcurrentHashMap<>();
        
        // Send prepare requests with asymmetric timeouts
        for (NodeId participant : participants) {
            long timeout = calculateAsymmetricTimeout(participant, bottleneckNodes);
            
            CompletableFuture<PrepareResponse> prepareRequest = sendPrepareRequest(
                transaction, participant, timeout, communicationManager);
            
            prepareRequests.put(participant, prepareRequest);
        }
        
        // Collect responses with failure probability consideration
        Map<NodeId, PrepareResponse> responses = new ConcurrentHashMap<>();
        Set<NodeId> failedNodes = new HashSet<>();
        
        for (Map.Entry<NodeId, CompletableFuture<PrepareResponse>> entry : prepareRequests.entrySet()) {
            NodeId nodeId = entry.getKey();
            CompletableFuture<PrepareResponse> future = entry.getValue();
            
            try {
                PrepareResponse response = future.get();
                responses.put(nodeId, response);
                
                if (!response.isSuccess()) {
                    failedNodes.add(nodeId);
                }
            } catch (Exception e) {
                failedNodes.add(nodeId);
                responses.put(nodeId, PrepareResponse.failure(nodeId, "Timeout or communication failure"));
            }
        }
        
        // Evaluate prepare phase success with asymmetric failure handling
        boolean prepareSuccess = evaluatePrepareSuccess(responses, failedNodes, bottleneckNodes);
        
        long duration = System.currentTimeMillis() - startTime;
        CommitPhaseMetrics metrics = new CommitPhaseMetrics(
            participants.size(), responses.size(), failedNodes.size(), duration);
        
        String failureReason = prepareSuccess ? null : 
            generatePrepareFailureReason(failedNodes, bottleneckNodes);
        
        return new PreparePhaseResult(prepareSuccess, failureReason, metrics);
    }
    
    /**
     * Executes commit phase with bottleneck node optimization.
     */
    private CommitPhaseResult executeCommitPhase(
            DistributedTransaction transaction,
            Set<NodeId> bottleneckNodes,
            CommunicationManager communicationManager) {
        
        long startTime = System.currentTimeMillis();
        Set<NodeId> participants = transaction.getParticipants();
        Map<NodeId, CompletableFuture<Boolean>> commitRequests = new ConcurrentHashMap<>();
        
        // Send commit requests with bottleneck optimization
        for (NodeId participant : participants) {
            CompletableFuture<Boolean> commitRequest = sendCommitRequest(
                transaction, participant, bottleneckNodes.contains(participant), communicationManager);
            
            commitRequests.put(participant, commitRequest);
        }
        
        // Collect commit responses
        Map<NodeId, Boolean> responses = new ConcurrentHashMap<>();
        Set<NodeId> failedCommits = new HashSet<>();
        
        for (Map.Entry<NodeId, CompletableFuture<Boolean>> entry : commitRequests.entrySet()) {
            NodeId nodeId = entry.getKey();
            CompletableFuture<Boolean> future = entry.getValue();
            
            try {
                Boolean success = future.get();
                responses.put(nodeId, success);
                
                if (!success) {
                    failedCommits.add(nodeId);
                }
            } catch (Exception e) {
                failedCommits.add(nodeId);
                responses.put(nodeId, false);
            }
        }
        
        // Evaluate commit success
        boolean commitSuccess = failedCommits.isEmpty();
        
        long duration = System.currentTimeMillis() - startTime;
        CommitPhaseMetrics metrics = new CommitPhaseMetrics(
            participants.size(), responses.size(), failedCommits.size(), duration);
        
        String details = commitSuccess ? "All participants committed successfully" :
            "Failed commits on nodes: " + failedCommits;
        
        return new CommitPhaseResult(commitSuccess, details, metrics);
    }
    
    /**
     * Calculates asymmetric timeout based on node failure probability.
     */
    private long calculateAsymmetricTimeout(NodeId nodeId, Set<NodeId> bottleneckNodes) {
        double failureProbability = nodeFailureProbabilities.getOrDefault(nodeId, 0.01);
        
        // Base timeout
        long baseTimeout = defaultTimeoutMs;
        
        // Increase timeout for high failure probability nodes
        double timeoutMultiplier = 1.0 + (failureProbability * 2.0);
        
        // Additional timeout for bottleneck nodes
        if (bottleneckNodes.contains(nodeId)) {
            timeoutMultiplier *= 1.5;
        }
        
        return (long) (baseTimeout * timeoutMultiplier);
    }
    
    /**
     * Sends prepare request to a participant node.
     */
    private CompletableFuture<PrepareResponse> sendPrepareRequest(
            DistributedTransaction transaction,
            NodeId participant,
            long timeout,
            CommunicationManager communicationManager) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create prepare message
                Message prepareMessage = new Message(
                    MessageId.generate(),
                    new NodeId("Coordinator"), // Create coordinator node ID
                    participant,
                    MessageType.TRANSACTION_PREPARE,
                    serializeTransaction(transaction),
                    System.currentTimeMillis(),
                    1 // High priority
                );
                
                // Send with timeout
                CompletableFuture<Message> response = communicationManager.sendRPC(
                    participant, createRPCRequest(prepareMessage));
                
                Message responseMessage = response.get(timeout, TimeUnit.MILLISECONDS);
                return deserializePrepareResponse(responseMessage.getPayload());
                
            } catch (TimeoutException e) {
                return PrepareResponse.failure(participant, "Prepare timeout");
            } catch (Exception e) {
                return PrepareResponse.failure(participant, "Prepare failed: " + e.getMessage());
            }
        }, executorService);
    }
    
    /**
     * Sends commit request to a participant node.
     */
    private CompletableFuture<Boolean> sendCommitRequest(
            DistributedTransaction transaction,
            NodeId participant,
            boolean isBottleneckNode,
            CommunicationManager communicationManager) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create commit message with bottleneck optimization
                Message commitMessage = new Message(
                    MessageId.generate(),
                    new NodeId("Coordinator"),
                    participant,
                    MessageType.TRANSACTION_COMMIT,
                    serializeCommitRequest(transaction, isBottleneckNode),
                    System.currentTimeMillis(),
                    isBottleneckNode ? 2 : 1 // Higher priority for bottleneck nodes
                );
                
                // Send with appropriate timeout
                long timeout = isBottleneckNode ? defaultTimeoutMs * 2 : defaultTimeoutMs;
                CompletableFuture<Message> response = communicationManager.sendRPC(
                    participant, createRPCRequest(commitMessage));
                
                Message responseMessage = response.get(timeout, TimeUnit.MILLISECONDS);
                return deserializeCommitResponse(responseMessage.getPayload());
                
            } catch (Exception e) {
                return false;
            }
        }, executorService);
    }
    
    /**
     * Evaluates prepare phase success with asymmetric failure handling.
     */
    private boolean evaluatePrepareSuccess(
            Map<NodeId, PrepareResponse> responses,
            Set<NodeId> failedNodes,
            Set<NodeId> bottleneckNodes) {
        
        // All nodes must respond positively for success
        if (!failedNodes.isEmpty()) {
            return false;
        }
        
        // Check if all responses are successful
        return responses.values().stream().allMatch(PrepareResponse::isSuccess);
    }
    
    /**
     * Executes abort phase to rollback transaction.
     */
    private void executeAbortPhase(
            DistributedTransaction transaction,
            CommunicationManager communicationManager) {
        
        Set<NodeId> participants = transaction.getParticipants();
        
        for (NodeId participant : participants) {
            CompletableFuture.runAsync(() -> {
                try {
                    Message abortMessage = new Message(
                        MessageId.generate(),
                        new NodeId("Coordinator"),
                        participant,
                        MessageType.TRANSACTION_ABORT,
                        serializeAbortRequest(transaction),
                        System.currentTimeMillis(),
                        1
                    );
                    
                    communicationManager.sendRPC(participant, createRPCRequest(abortMessage));
                } catch (Exception e) {
                    // Log abort failure but continue
                    System.err.println("Failed to send abort to " + participant + ": " + e.getMessage());
                }
            }, executorService);
        }
    }
    
    // Helper methods for serialization/deserialization
    private byte[] serializeTransaction(DistributedTransaction transaction) {
        // Simplified serialization - in real implementation would use proper serialization
        return transaction.toString().getBytes();
    }
    
    private byte[] serializeCommitRequest(DistributedTransaction transaction, boolean isBottleneckNode) {
        String request = transaction.getTransactionId().getId() + ":" + isBottleneckNode;
        return request.getBytes();
    }
    
    private byte[] serializeAbortRequest(DistributedTransaction transaction) {
        return transaction.getTransactionId().getId().getBytes();
    }
    
    private PrepareResponse deserializePrepareResponse(byte[] payload) {
        // Simplified deserialization
        String response = new String(payload);
        NodeId dummyNode = NodeId.EDGE1; // Placeholder node for deserialization
        return response.startsWith("SUCCESS") ? 
            PrepareResponse.success(dummyNode) :
            PrepareResponse.failure(dummyNode, response);
    }
    
    private Boolean deserializeCommitResponse(byte[] payload) {
        String response = new String(payload);
        return "COMMITTED".equals(response);
    }
    
    private RPCRequest createRPCRequest(Message message) {
        return new RPCRequest(message.getType().name(), new Object[]{message.getPayload()}, 5000, 3);
    }
    
    private String generatePrepareFailureReason(Set<NodeId> failedNodes, Set<NodeId> bottleneckNodes) {
        StringBuilder reason = new StringBuilder("Prepare phase failed on nodes: ");
        reason.append(failedNodes.stream().map(NodeId::getId).collect(Collectors.joining(", ")));
        
        Set<NodeId> failedBottlenecks = failedNodes.stream()
            .filter(bottleneckNodes::contains)
            .collect(Collectors.toSet());
        
        if (!failedBottlenecks.isEmpty()) {
            reason.append(" (including bottleneck nodes: ")
                  .append(failedBottlenecks.stream().map(NodeId::getId).collect(Collectors.joining(", ")))
                  .append(")");
        }
        
        return reason.toString();
    }
    
    private CommitPhaseMetrics mergePhaseMetrics(CommitPhaseMetrics prepare, CommitPhaseMetrics commit) {
        return new CommitPhaseMetrics(
            prepare.getTotalParticipants(),
            prepare.getSuccessfulResponses() + commit.getSuccessfulResponses(),
            prepare.getFailedResponses() + commit.getFailedResponses(),
            prepare.getDurationMs() + commit.getDurationMs()
        );
    }
    
    // Result classes
    public static class DistributedCommitResult {
        private final TransactionId transactionId;
        private final CommitResult result;
        private final String details;
        private final CommitPhaseMetrics metrics;
        
        public DistributedCommitResult(TransactionId transactionId, CommitResult result, 
                                     String details, CommitPhaseMetrics metrics) {
            this.transactionId = transactionId;
            this.result = result;
            this.details = details;
            this.metrics = metrics;
        }
        
        // Getters
        public TransactionId getTransactionId() { return transactionId; }
        public CommitResult getResult() { return result; }
        public String getDetails() { return details; }
        public CommitPhaseMetrics getMetrics() { return metrics; }
    }
    
    private static class PreparePhaseResult {
        private final boolean success;
        private final String failureReason;
        private final CommitPhaseMetrics phaseMetrics;
        
        public PreparePhaseResult(boolean success, String failureReason, CommitPhaseMetrics phaseMetrics) {
            this.success = success;
            this.failureReason = failureReason;
            this.phaseMetrics = phaseMetrics;
        }
        
        public boolean isSuccess() { return success; }
        public String getFailureReason() { return failureReason; }
        public CommitPhaseMetrics getPhaseMetrics() { return phaseMetrics; }
    }
    
    private static class CommitPhaseResult {
        private final boolean success;
        private final String details;
        private final CommitPhaseMetrics phaseMetrics;
        
        public CommitPhaseResult(boolean success, String details, CommitPhaseMetrics phaseMetrics) {
            this.success = success;
            this.details = details;
            this.phaseMetrics = phaseMetrics;
        }
        
        public boolean isSuccess() { return success; }
        public String getDetails() { return details; }
        public CommitPhaseMetrics getPhaseMetrics() { return phaseMetrics; }
    }
    
    public static class CommitPhaseMetrics {
        private final int totalParticipants;
        private final int successfulResponses;
        private final int failedResponses;
        private final long durationMs;
        
        public CommitPhaseMetrics() {
            this(0, 0, 0, 0);
        }
        
        public CommitPhaseMetrics(int totalParticipants, int successfulResponses, 
                                int failedResponses, long durationMs) {
            this.totalParticipants = totalParticipants;
            this.successfulResponses = successfulResponses;
            this.failedResponses = failedResponses;
            this.durationMs = durationMs;
        }
        
        // Getters
        public int getTotalParticipants() { return totalParticipants; }
        public int getSuccessfulResponses() { return successfulResponses; }
        public int getFailedResponses() { return failedResponses; }
        public long getDurationMs() { return durationMs; }
    }
}