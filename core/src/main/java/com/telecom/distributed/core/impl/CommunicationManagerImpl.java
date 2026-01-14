package com.telecom.distributed.core.impl;

import com.telecom.distributed.core.CommunicationManager;
import com.telecom.distributed.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of CommunicationManager with asynchronous message handling,
 * RPC infrastructure, protocol buffer serialization, and priority-based queuing.
 */
public class CommunicationManagerImpl implements CommunicationManager {
    private static final Logger logger = LoggerFactory.getLogger(CommunicationManagerImpl.class);
    
    private final NodeId nodeId;
    private final Map<MessageType, MessageHandler> messageHandlers;
    private final PriorityBlockingQueue<Message> messageQueue;
    private final Map<String, CompletableFuture<Message>> pendingRPCs;
    private final ExecutorService messageProcessor;
    private final ExecutorService rpcExecutor;
    private final ScheduledExecutorService timeoutScheduler;
    private final AtomicLong messageIdCounter;
    private final MessageSerializer serializer;
    private final EventOrderingService eventOrderingService;
    
    // Configuration
    private final long defaultRpcTimeoutMs;
    private final int defaultRetryCount;
    private final int maxQueueSize;

    public CommunicationManagerImpl(NodeId nodeId) {
        this(nodeId, 5000L, 3, 10000);
    }

    public CommunicationManagerImpl(NodeId nodeId, long defaultRpcTimeoutMs, 
                                  int defaultRetryCount, int maxQueueSize) {
        this.nodeId = Objects.requireNonNull(nodeId, "Node ID cannot be null");
        this.defaultRpcTimeoutMs = defaultRpcTimeoutMs;
        this.defaultRetryCount = defaultRetryCount;
        this.maxQueueSize = maxQueueSize;
        
        this.messageHandlers = new ConcurrentHashMap<>();
        this.messageQueue = new PriorityBlockingQueue<>(maxQueueSize, 
            Comparator.comparingInt(Message::getPriority).reversed()
                     .thenComparingLong(Message::getTimestamp));
        this.pendingRPCs = new ConcurrentHashMap<>();
        this.messageIdCounter = new AtomicLong(0);
        this.serializer = new MessageSerializer();
        this.eventOrderingService = new EventOrderingServiceImpl();
        
        // Thread pools
        this.messageProcessor = Executors.newFixedThreadPool(4, 
            r -> new Thread(r, "MessageProcessor-" + nodeId));
        this.rpcExecutor = Executors.newFixedThreadPool(8, 
            r -> new Thread(r, "RPCExecutor-" + nodeId));
        this.timeoutScheduler = Executors.newScheduledThreadPool(2, 
            r -> new Thread(r, "TimeoutScheduler-" + nodeId));
        
        startMessageProcessing();
        logger.info("CommunicationManager initialized for node: {}", nodeId);
    }

    @Override
    public CompletableFuture<Message> sendRPC(NodeId target, RPCRequest request) {
        Objects.requireNonNull(target, "Target node cannot be null");
        Objects.requireNonNull(request, "RPC request cannot be null");
        
        return sendRPCWithRetry(target, request, request.getRetryCount());
    }

    private CompletableFuture<Message> sendRPCWithRetry(NodeId target, RPCRequest request, int retriesLeft) {
        String requestId = generateRequestId();
        CompletableFuture<Message> future = new CompletableFuture<>();
        
        // Store the pending RPC
        pendingRPCs.put(requestId, future);
        
        // Create RPC message
        Message rpcMessage = createRPCMessage(target, request, requestId);
        
        // Schedule timeout
        ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
            CompletableFuture<Message> pendingFuture = pendingRPCs.remove(requestId);
            if (pendingFuture != null && !pendingFuture.isDone()) {
                if (retriesLeft > 0) {
                    logger.warn("RPC timeout for request {}, retrying ({} retries left)", 
                              requestId, retriesLeft);
                    // Retry
                    sendRPCWithRetry(target, request, retriesLeft - 1)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                pendingFuture.completeExceptionally(error);
                            } else {
                                pendingFuture.complete(result);
                            }
                        });
                } else {
                    logger.error("RPC timeout for request {} after all retries", requestId);
                    pendingFuture.completeExceptionally(
                        new TimeoutException("RPC timeout after " + request.getRetryCount() + " retries"));
                }
            }
        }, request.getTimeoutMs(), TimeUnit.MILLISECONDS);
        
        // Cancel timeout if RPC completes
        future.whenComplete((result, error) -> timeoutTask.cancel(false));
        
        // Send the message asynchronously
        rpcExecutor.submit(() -> {
            try {
                sendMessageInternal(rpcMessage);
                logger.debug("RPC request {} sent to {}", requestId, target);
            } catch (Exception e) {
                CompletableFuture<Message> pendingFuture = pendingRPCs.remove(requestId);
                if (pendingFuture != null) {
                    pendingFuture.completeExceptionally(e);
                }
                logger.error("Failed to send RPC request {} to {}", requestId, target, e);
            }
        });
        
        return future;
    }

    @Override
    public void broadcastMessage(Message message, Set<NodeId> targets) {
        Objects.requireNonNull(message, "Message cannot be null");
        Objects.requireNonNull(targets, "Targets cannot be null");
        
        if (targets.isEmpty()) {
            logger.warn("No targets specified for broadcast message");
            return;
        }
        
        logger.debug("Broadcasting message {} to {} targets", message.getId(), targets.size());
        
        // Send to each target asynchronously
        for (NodeId target : targets) {
            rpcExecutor.submit(() -> {
                try {
                    Message targetMessage = createTargetedMessage(message, target);
                    sendMessageInternal(targetMessage);
                    logger.debug("Broadcast message {} sent to {}", message.getId(), target);
                } catch (Exception e) {
                    logger.error("Failed to send broadcast message {} to {}", message.getId(), target, e);
                }
            });
        }
    }

    @Override
    public void registerMessageHandler(MessageType type, MessageHandler handler) {
        Objects.requireNonNull(type, "Message type cannot be null");
        Objects.requireNonNull(handler, "Message handler cannot be null");
        
        messageHandlers.put(type, handler);
        logger.debug("Registered handler for message type: {}", type);
    }

    @Override
    public NetworkPartition detectPartition() {
        // Simple partition detection based on failed communications
        // In a real implementation, this would use more sophisticated algorithms
        Set<NodeId> unreachableNodes = new HashSet<>();
        
        // Check for nodes that haven't responded to recent heartbeats
        // This is a simplified implementation
        for (String requestId : pendingRPCs.keySet()) {
            CompletableFuture<Message> future = pendingRPCs.get(requestId);
            if (future.isCompletedExceptionally()) {
                // Could indicate network partition
                logger.debug("Detected potential partition based on failed RPC: {}", requestId);
            }
        }
        
        if (!unreachableNodes.isEmpty()) {
            // Create partition with unreachable nodes and current node as separate partitions
            Set<NodeId> currentNodePartition = Set.of(this.nodeId);
            Set<Set<NodeId>> partitions = Set.of(unreachableNodes, currentNodePartition);
            return new NetworkPartition(partitions, System.currentTimeMillis(), 
                NetworkPartition.PartitionType.ASYMMETRIC);
        }
        
        return null;
    }

    /**
     * Handles incoming messages from other nodes.
     */
    public void handleIncomingMessage(Message message) {
        Objects.requireNonNull(message, "Message cannot be null");
        
        // Add to priority queue for processing
        if (!messageQueue.offer(message)) {
            logger.error("Message queue full, dropping message: {}", message.getId());
            return;
        }
        
        logger.debug("Queued incoming message: {} from {}", message.getId(), message.getSender());
    }

    private void startMessageProcessing() {
        // Start message processing threads
        for (int i = 0; i < 4; i++) {
            messageProcessor.submit(this::processMessages);
        }
    }

    private void processMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Message message = messageQueue.take(); // Blocks until message available
                processMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error processing message", e);
            }
        }
    }

    private void processMessage(Message message) {
        try {
            logger.debug("Processing message: {} of type {}", message.getId(), message.getType());
            
            // Handle RPC responses
            if (message.getType() == MessageType.RPC_RESPONSE) {
                handleRPCResponse(message);
                return;
            }
            
            // Handle event ordering messages
            if (message.getType() == MessageType.EVENT_ORDERING) {
                eventOrderingService.handleEventMessage(message);
                return;
            }
            
            // Delegate to registered handlers
            MessageHandler handler = messageHandlers.get(message.getType());
            if (handler != null) {
                handler.handle(message);
            } else {
                logger.warn("No handler registered for message type: {}", message.getType());
            }
            
        } catch (Exception e) {
            logger.error("Error processing message: {}", message.getId(), e);
        }
    }

    private void handleRPCResponse(Message message) {
        try {
            // Extract request ID from message payload
            RPCResponse response = serializer.deserializeRPCResponse(message.getPayload());
            String requestId = response.getRequestId();
            
            CompletableFuture<Message> future = pendingRPCs.remove(requestId);
            if (future != null) {
                future.complete(message);
                logger.debug("Completed RPC request: {}", requestId);
            } else {
                logger.warn("Received RPC response for unknown request: {}", requestId);
            }
        } catch (Exception e) {
            logger.error("Error handling RPC response", e);
        }
    }

    private Message createRPCMessage(NodeId target, RPCRequest request, String requestId) {
        try {
            // Serialize RPC request with request ID
            byte[] payload = serializer.serializeRPCRequest(request, requestId);
            
            return new Message(
                new MessageId(generateMessageId()),
                nodeId,
                target,
                MessageType.RPC_REQUEST,
                payload,
                System.currentTimeMillis(),
                5 // Default priority for RPC
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create RPC message", e);
        }
    }

    private Message createTargetedMessage(Message original, NodeId target) {
        return new Message(
            new MessageId(generateMessageId()),
            original.getSender(),
            target,
            original.getType(),
            original.getPayload(),
            original.getTimestamp(),
            original.getPriority()
        );
    }

    private void sendMessageInternal(Message message) {
        // In a real implementation, this would send over network
        // For now, we simulate network delay and log the send
        try {
            Thread.sleep(10); // Simulate network latency
            logger.debug("Sent message {} to {}", message.getId(), message.getReceiver());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while sending message", e);
        }
    }

    private String generateRequestId() {
        return nodeId + "-rpc-" + messageIdCounter.incrementAndGet();
    }

    private String generateMessageId() {
        return nodeId + "-msg-" + messageIdCounter.incrementAndGet();
    }

    public void shutdown() {
        logger.info("Shutting down CommunicationManager for node: {}", nodeId);
        
        messageProcessor.shutdown();
        rpcExecutor.shutdown();
        timeoutScheduler.shutdown();
        
        try {
            if (!messageProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                messageProcessor.shutdownNow();
            }
            if (!rpcExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                rpcExecutor.shutdownNow();
            }
            if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            messageProcessor.shutdownNow();
            rpcExecutor.shutdownNow();
            timeoutScheduler.shutdownNow();
        }
        
        // Complete any pending RPCs with cancellation
        for (CompletableFuture<Message> future : pendingRPCs.values()) {
            future.cancel(true);
        }
        pendingRPCs.clear();
        
        logger.info("CommunicationManager shutdown complete for node: {}", nodeId);
    }
}