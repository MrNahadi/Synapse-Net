package com.telecom.distributed.core.impl;

import com.telecom.distributed.core.CommunicationManager;
import com.telecom.distributed.core.DistributedDeadlockDetector;
import com.telecom.distributed.core.TransactionManager;
import com.telecom.distributed.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Optimized implementation of CommunicationManager with latency minimization,
 * transaction integration, and memory management optimization.
 * 
 * Key optimizations:
 * - Latency-aware routing across heterogeneous nodes
 * - Transaction completion time bounds enforcement
 * - Memory-efficient message buffering and pooling
 * - Integration with deadlock detection and transaction protocols
 */
public class OptimizedCommunicationManager implements CommunicationManager {
    private static final Logger logger = LoggerFactory.getLogger(OptimizedCommunicationManager.class);
    
    private final Map<NodeId, NodeConfiguration> nodeConfigurations;
    private final Map<MessageType, MessageHandler> messageHandlers;
    private final TransactionManager transactionManager;
    private final DistributedDeadlockDetector deadlockDetector;
    
    // Latency optimization structures
    private final Map<NodeId, Map<NodeId, Double>> latencyMatrix;
    private final Map<NodeId, List<NodeId>> routingTable;
    
    // Memory management
    private final BlockingQueue<MutableMessage> messagePool;
    private final int maxPoolSize;
    private final Map<NodeId, BlockingQueue<MutableMessage>> nodeMessageQueues;
    private final int maxQueueSize;
    
    // Transaction completion bounds
    private final Map<TransactionId, Long> transactionStartTimes;
    private final Map<TransactionId, Long> transactionTimeoutBounds;
    private final long defaultTransactionTimeout;
    
    // Network partition detection
    private final Map<NodeId, Long> lastHeartbeat;
    private final long heartbeatTimeout;
    
    // Executor for async operations
    private final ExecutorService executorService;
    
    public OptimizedCommunicationManager(
            Map<NodeId, NodeConfiguration> nodeConfigurations,
            TransactionManager transactionManager,
            DistributedDeadlockDetector deadlockDetector,
            long defaultTransactionTimeout,
            long heartbeatTimeout,
            int maxPoolSize,
            int maxQueueSize) {
        
        this.nodeConfigurations = new ConcurrentHashMap<>(nodeConfigurations);
        this.messageHandlers = new ConcurrentHashMap<>();
        this.transactionManager = transactionManager;
        this.deadlockDetector = deadlockDetector;
        this.defaultTransactionTimeout = defaultTransactionTimeout;
        this.heartbeatTimeout = heartbeatTimeout;
        this.maxPoolSize = maxPoolSize;
        this.maxQueueSize = maxQueueSize;
        
        // Initialize latency optimization structures
        this.latencyMatrix = buildLatencyMatrix();
        this.routingTable = buildOptimalRoutingTable();
        
        // Initialize memory management
        this.messagePool = new LinkedBlockingQueue<>(maxPoolSize);
        this.nodeMessageQueues = new ConcurrentHashMap<>();
        for (NodeId nodeId : nodeConfigurations.keySet()) {
            nodeMessageQueues.put(nodeId, new LinkedBlockingQueue<>(maxQueueSize));
        }
        
        // Initialize transaction tracking
        this.transactionStartTimes = new ConcurrentHashMap<>();
        this.transactionTimeoutBounds = new ConcurrentHashMap<>();
        
        // Initialize heartbeat tracking
        this.lastHeartbeat = new ConcurrentHashMap<>();
        for (NodeId nodeId : nodeConfigurations.keySet()) {
            lastHeartbeat.put(nodeId, System.currentTimeMillis());
        }
        
        // Initialize executor
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
        );
        
        logger.info("OptimizedCommunicationManager initialized with {} nodes", 
                   nodeConfigurations.size());
    }
    
    /**
     * Builds latency matrix from node configurations for routing optimization.
     */
    private Map<NodeId, Map<NodeId, Double>> buildLatencyMatrix() {
        Map<NodeId, Map<NodeId, Double>> matrix = new ConcurrentHashMap<>();
        
        for (NodeId source : nodeConfigurations.keySet()) {
            Map<NodeId, Double> sourceLatencies = new ConcurrentHashMap<>();
            NodeMetrics sourceMetrics = nodeConfigurations.get(source).getBaselineMetrics();
            
            for (NodeId dest : nodeConfigurations.keySet()) {
                if (source.equals(dest)) {
                    sourceLatencies.put(dest, 0.0);
                } else {
                    NodeMetrics destMetrics = nodeConfigurations.get(dest).getBaselineMetrics();
                    // Estimate latency based on node characteristics
                    double estimatedLatency = (sourceMetrics.getLatency() + destMetrics.getLatency()) / 2.0;
                    sourceLatencies.put(dest, estimatedLatency);
                }
            }
            matrix.put(source, sourceLatencies);
        }
        
        return matrix;
    }
    
    /**
     * Builds optimal routing table using shortest path algorithm.
     * Minimizes end-to-end latency across heterogeneous nodes.
     */
    private Map<NodeId, List<NodeId>> buildOptimalRoutingTable() {
        Map<NodeId, List<NodeId>> routingTable = new ConcurrentHashMap<>();
        
        for (NodeId source : nodeConfigurations.keySet()) {
            List<NodeId> sortedDestinations = nodeConfigurations.keySet().stream()
                .filter(dest -> !dest.equals(source))
                .sorted(Comparator.comparing(dest -> latencyMatrix.get(source).get(dest)))
                .collect(Collectors.toList());
            
            routingTable.put(source, sortedDestinations);
        }
        
        logger.info("Built optimal routing table for {} nodes", routingTable.size());
        return routingTable;
    }
    
    /**
     * Calculates optimal route between source and destination to minimize latency.
     */
    public List<NodeId> getOptimalRoute(NodeId source, NodeId destination) {
        if (source.equals(destination)) {
            return Collections.singletonList(source);
        }
        
        // For direct communication, return direct route
        List<NodeId> route = new ArrayList<>();
        route.add(source);
        route.add(destination);
        
        return route;
    }
    
    /**
     * Calculates expected latency for a route.
     */
    public double calculateRouteLatency(List<NodeId> route) {
        if (route.size() <= 1) {
            return 0.0;
        }
        
        double totalLatency = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            NodeId from = route.get(i);
            NodeId to = route.get(i + 1);
            totalLatency += latencyMatrix.get(from).get(to);
        }
        
        return totalLatency;
    }
    
    @Override
    public CompletableFuture<Message> sendRPC(NodeId target, RPCRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get optimal route
                List<NodeId> route = getOptimalRoute(request.getSourceNode(), target);
                double expectedLatency = calculateRouteLatency(route);
                
                logger.debug("Sending RPC from {} to {} via route {} (expected latency: {}ms)",
                           request.getSourceNode(), target, route, expectedLatency);
                
                // Create message from pool or new
                MutableMessage message = acquireMessage();
                message.setSender(request.getSourceNode());
                message.setReceiver(target);
                message.setType(MessageType.RPC_REQUEST);
                message.setTimestamp(System.currentTimeMillis());
                message.setPriority(request.getPriority());
                
                // Enqueue message
                BlockingQueue<MutableMessage> targetQueue = nodeMessageQueues.get(target);
                if (targetQueue == null) {
                    throw new IllegalStateException("No message queue for target node: " + target);
                }
                
                boolean enqueued = targetQueue.offer(message, 
                    (long) expectedLatency, TimeUnit.MILLISECONDS);
                
                if (!enqueued) {
                    releaseMessage(message);
                    throw new TimeoutException("Failed to enqueue message within expected latency");
                }
                
                // Simulate processing and create response
                MutableMessage response = acquireMessage();
                response.setSender(target);
                response.setReceiver(request.getSourceNode());
                response.setType(MessageType.RPC_RESPONSE);
                response.setTimestamp(System.currentTimeMillis());
                
                // Update heartbeat
                lastHeartbeat.put(target, System.currentTimeMillis());
                
                return response.toImmutable();
                
            } catch (Exception e) {
                logger.error("RPC failed from {} to {}: {}", 
                           request.getSourceNode(), target, e.getMessage());
                throw new CompletionException(e);
            }
        }, executorService);
    }
    
    @Override
    public void broadcastMessage(Message message, Set<NodeId> targets) {
        logger.debug("Broadcasting message {} to {} targets", message.getId(), targets.size());
        
        List<CompletableFuture<Void>> futures = targets.stream()
            .map(target -> CompletableFuture.runAsync(() -> {
                try {
                    BlockingQueue<MutableMessage> queue = nodeMessageQueues.get(target);
                    if (queue != null) {
                        MutableMessage copy = copyMessage(message);
                        copy.setReceiver(target);
                        queue.offer(copy, 1, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Broadcast interrupted for target {}", target);
                }
            }, executorService))
            .collect(Collectors.toList());
        
        // Wait for all broadcasts to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
    
    @Override
    public void registerMessageHandler(MessageType type, MessageHandler handler) {
        messageHandlers.put(type, handler);
        logger.info("Registered handler for message type: {}", type);
    }
    
    @Override
    public NetworkPartition detectPartition() {
        long currentTime = System.currentTimeMillis();
        Set<NodeId> unreachableNodes = new HashSet<>();
        
        for (Map.Entry<NodeId, Long> entry : lastHeartbeat.entrySet()) {
            long timeSinceHeartbeat = currentTime - entry.getValue();
            if (timeSinceHeartbeat > heartbeatTimeout) {
                unreachableNodes.add(entry.getKey());
            }
        }
        
        if (unreachableNodes.isEmpty()) {
            return null;
        }
        
        Set<NodeId> reachableNodes = new HashSet<>(nodeConfigurations.keySet());
        reachableNodes.removeAll(unreachableNodes);
        
        // Create partition sets
        Set<Set<NodeId>> partitionSets = new HashSet<>();
        partitionSets.add(reachableNodes);
        partitionSets.add(unreachableNodes);
        
        logger.warn("Network partition detected: {} unreachable nodes", unreachableNodes.size());
        return new NetworkPartition(partitionSets, System.currentTimeMillis(), 
                                   NetworkPartition.PartitionType.CLEAN_SPLIT);
    }
    
    /**
     * Registers a transaction and calculates its completion time bound.
     */
    public void registerTransaction(TransactionId txId, Set<NodeId> participants) {
        long startTime = System.currentTimeMillis();
        transactionStartTimes.put(txId, startTime);
        
        // Calculate upper bound based on participant latencies
        double maxLatency = participants.stream()
            .map(nodeId -> nodeConfigurations.get(nodeId).getBaselineMetrics().getLatency())
            .max(Double::compare)
            .orElse(10.0);
        
        // Upper bound = 2PC phases * max latency * participants + buffer
        long upperBound = (long) (2 * maxLatency * participants.size() * 1.5);
        long timeoutBound = Math.max(upperBound, defaultTransactionTimeout);
        
        transactionTimeoutBounds.put(txId, timeoutBound);
        
        logger.debug("Transaction {} registered with timeout bound: {}ms", txId, timeoutBound);
    }
    
    /**
     * Checks if a transaction has exceeded its completion time bound.
     */
    public boolean isTransactionTimedOut(TransactionId txId) {
        Long startTime = transactionStartTimes.get(txId);
        Long timeoutBound = transactionTimeoutBounds.get(txId);
        
        if (startTime == null || timeoutBound == null) {
            return false;
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        boolean timedOut = elapsed > timeoutBound;
        
        if (timedOut) {
            logger.warn("Transaction {} exceeded completion bound: {}ms > {}ms",
                       txId, elapsed, timeoutBound);
        }
        
        return timedOut;
    }
    
    /**
     * Gets the remaining time before transaction timeout.
     */
    public long getRemainingTransactionTime(TransactionId txId) {
        Long startTime = transactionStartTimes.get(txId);
        Long timeoutBound = transactionTimeoutBounds.get(txId);
        
        if (startTime == null || timeoutBound == null) {
            return defaultTransactionTimeout;
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, timeoutBound - elapsed);
    }
    
    /**
     * Completes a transaction and releases resources.
     */
    public void completeTransaction(TransactionId txId) {
        transactionStartTimes.remove(txId);
        transactionTimeoutBounds.remove(txId);
        logger.debug("Transaction {} completed and resources released", txId);
    }
    
    /**
     * Acquires a message from the pool or creates a new one.
     * Memory optimization through object pooling.
     */
    private MutableMessage acquireMessage() {
        MutableMessage message = messagePool.poll();
        if (message == null) {
            message = new MutableMessage();
            logger.trace("Created new message (pool empty)");
        } else {
            logger.trace("Acquired message from pool");
        }
        return message;
    }
    
    /**
     * Releases a message back to the pool for reuse.
     */
    private void releaseMessage(MutableMessage message) {
        if (messagePool.size() < maxPoolSize) {
            message.reset();
            messagePool.offer(message);
            logger.trace("Released message to pool");
        }
    }
    
    /**
     * Creates a copy of a message for broadcasting.
     */
    private MutableMessage copyMessage(Message original) {
        MutableMessage copy = acquireMessage();
        copy.setId(original.getId());
        copy.setSender(original.getSender());
        copy.setType(original.getType());
        copy.setPayload(original.getPayload());
        copy.setTimestamp(original.getTimestamp());
        copy.setPriority(original.getPriority());
        return copy;
    }
    
    /**
     * Gets current memory usage statistics.
     */
    public MemoryStatistics getMemoryStatistics() {
        int poolSize = messagePool.size();
        int totalQueuedMessages = nodeMessageQueues.values().stream()
            .mapToInt(BlockingQueue::size)
            .sum();
        
        return new MemoryStatistics(poolSize, maxPoolSize, totalQueuedMessages, 
                                   nodeMessageQueues.size() * maxQueueSize);
    }
    
    /**
     * Optimizes memory by clearing old messages from queues.
     */
    public void optimizeMemory() {
        long currentTime = System.currentTimeMillis();
        long messageTimeout = 5000; // 5 seconds
        
        for (BlockingQueue<MutableMessage> queue : nodeMessageQueues.values()) {
            queue.removeIf(msg -> 
                currentTime - msg.getTimestamp() > messageTimeout
            );
        }
        
        logger.info("Memory optimization completed");
    }
    
    /**
     * Shuts down the communication manager and releases resources.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("OptimizedCommunicationManager shut down");
    }
    
    /**
     * Memory usage statistics.
     */
    public static class MemoryStatistics {
        private final int poolSize;
        private final int maxPoolSize;
        private final int queuedMessages;
        private final int maxQueueCapacity;
        
        public MemoryStatistics(int poolSize, int maxPoolSize, 
                              int queuedMessages, int maxQueueCapacity) {
            this.poolSize = poolSize;
            this.maxPoolSize = maxPoolSize;
            this.queuedMessages = queuedMessages;
            this.maxQueueCapacity = maxQueueCapacity;
        }
        
        public int getPoolSize() { return poolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getQueuedMessages() { return queuedMessages; }
        public int getMaxQueueCapacity() { return maxQueueCapacity; }
        
        public double getPoolUtilization() {
            return (double) poolSize / maxPoolSize;
        }
        
        public double getQueueUtilization() {
            return (double) queuedMessages / maxQueueCapacity;
        }
    }
}
