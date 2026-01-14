package com.telecom.distributed.core.impl;

import com.telecom.distributed.core.LoadBalancer;
import com.telecom.distributed.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Implementation of LoadBalancer with heterogeneity-aware algorithms for
 * CPU, memory, and transaction load distribution.
 */
public class LoadBalancerImpl implements LoadBalancer {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerImpl.class);
    
    private final Map<NodeId, Double> nodeWeights;
    private final Map<NodeId, NodeMetrics> currentMetrics;
    private final Map<ServiceId, NodeId> serviceAllocations;
    private final AtomicInteger migrationCounter;
    private final AtomicInteger roundRobinCounter;
    
    private String currentStrategy;
    private long lastUpdateTimestamp;
    
    // Node capacity tracking
    private final Map<NodeId, Double> currentCpuLoad;
    private final Map<NodeId, Double> currentMemoryLoad;
    private final Map<NodeId, Integer> currentTransactionLoad;
    
    public LoadBalancerImpl() {
        this.nodeWeights = new ConcurrentHashMap<>();
        this.currentMetrics = new ConcurrentHashMap<>();
        this.serviceAllocations = new ConcurrentHashMap<>();
        this.migrationCounter = new AtomicInteger(0);
        this.roundRobinCounter = new AtomicInteger(0);
        this.currentCpuLoad = new ConcurrentHashMap<>();
        this.currentMemoryLoad = new ConcurrentHashMap<>();
        this.currentTransactionLoad = new ConcurrentHashMap<>();
        this.currentStrategy = LoadBalancingStrategy.RESOURCE_AWARE;
        this.lastUpdateTimestamp = System.currentTimeMillis();
        
        // Initialize with default weights for all nodes
        initializeDefaultWeights();
        initializeLoadTracking();
    }
    
    private void initializeDefaultWeights() {
        // Initialize weights based on node capabilities from dataset
        nodeWeights.put(NodeId.EDGE1, 0.8);   // Good performance, crash failures
        nodeWeights.put(NodeId.EDGE2, 0.7);   // Lower throughput, omission failures
        nodeWeights.put(NodeId.CORE1, 1.0);   // Best performance, Byzantine tolerance
        nodeWeights.put(NodeId.CORE2, 0.9);   // Good performance, crash failures
        nodeWeights.put(NodeId.CLOUD1, 0.6);  // Highest latency, but high memory
    }
    
    private void initializeLoadTracking() {
        for (NodeId nodeId : Arrays.asList(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1)) {
            currentCpuLoad.put(nodeId, 0.0);
            currentMemoryLoad.put(nodeId, 0.0);
            currentTransactionLoad.put(nodeId, 0);
        }
    }
    
    @Override
    public NodeId selectNode(ServiceRequest request) {
        Objects.requireNonNull(request, "Service request cannot be null");
        
        switch (currentStrategy) {
            case LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN:
                return selectNodeWeightedRoundRobin(request);
            case LoadBalancingStrategy.LEAST_CONNECTIONS:
                return selectNodeLeastConnections(request);
            case LoadBalancingStrategy.RESOURCE_AWARE:
            default:
                return selectNodeResourceAware(request);
        }
    }
    
    private NodeId selectNodeWeightedRoundRobin(ServiceRequest request) {
        List<NodeId> availableNodes = getAvailableNodes();
        if (availableNodes.isEmpty()) {
            throw new IllegalStateException("No available nodes for service allocation");
        }
        
        // Create weighted list based on node weights
        List<NodeId> weightedNodes = new ArrayList<>();
        for (NodeId nodeId : availableNodes) {
            double weight = nodeWeights.getOrDefault(nodeId, 1.0);
            int copies = Math.max(1, (int) (weight * 10)); // Scale weights to integer copies
            for (int i = 0; i < copies; i++) {
                weightedNodes.add(nodeId);
            }
        }
        
        int index = roundRobinCounter.getAndIncrement() % weightedNodes.size();
        NodeId selectedNode = weightedNodes.get(index);
        
        updateLoadAfterAllocation(selectedNode, request);
        serviceAllocations.put(request.getServiceId(), selectedNode);
        
        logger.debug("Selected node {} for service {} using weighted round robin", 
                    selectedNode, request.getServiceId());
        return selectedNode;
    }
    
    private NodeId selectNodeLeastConnections(ServiceRequest request) {
        return serviceAllocations.values().stream()
                .collect(Collectors.groupingBy(nodeId -> nodeId, Collectors.counting()))
                .entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(getAvailableNodes().get(0));
    }
    
    private NodeId selectNodeResourceAware(ServiceRequest request) {
        List<NodeId> availableNodes = getAvailableNodes();
        if (availableNodes.isEmpty()) {
            throw new IllegalStateException("No available nodes for service allocation");
        }
        
        NodeId bestNode = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (NodeId nodeId : availableNodes) {
            if (canAccommodateRequest(nodeId, request)) {
                double score = calculateNodeScore(nodeId, request);
                if (score > bestScore) {
                    bestScore = score;
                    bestNode = nodeId;
                }
            }
        }
        
        if (bestNode == null) {
            // Fallback to least loaded node if no node can fully accommodate
            bestNode = findLeastLoadedNode(availableNodes);
        }
        
        updateLoadAfterAllocation(bestNode, request);
        serviceAllocations.put(request.getServiceId(), bestNode);
        
        logger.debug("Selected node {} for service {} using resource-aware strategy (score: {})", 
                    bestNode, request.getServiceId(), bestScore);
        return bestNode;
    }
    
    private boolean canAccommodateRequest(NodeId nodeId, ServiceRequest request) {
        NodeMetrics metrics = currentMetrics.get(nodeId);
        if (metrics == null) return false;
        
        double currentCpu = currentCpuLoad.getOrDefault(nodeId, 0.0);
        double currentMemory = currentMemoryLoad.getOrDefault(nodeId, 0.0);
        int currentTransactions = currentTransactionLoad.getOrDefault(nodeId, 0);
        
        // Check if adding this request would exceed node capacity
        boolean cpuOk = (currentCpu + request.getCpuRequirement()) <= metrics.getCpuUtilization();
        boolean memoryOk = (currentMemory + request.getMemoryRequirement()) <= metrics.getMemoryUsage();
        boolean transactionOk = (currentTransactions + request.getTransactionLoad()) <= metrics.getTransactionsPerSec();
        
        return cpuOk && memoryOk && transactionOk;
    }
    
    private double calculateNodeScore(NodeId nodeId, ServiceRequest request) {
        NodeMetrics metrics = currentMetrics.get(nodeId);
        if (metrics == null) return 0.0;
        
        double weight = nodeWeights.getOrDefault(nodeId, 1.0);
        double currentCpu = currentCpuLoad.getOrDefault(nodeId, 0.0);
        double currentMemory = currentMemoryLoad.getOrDefault(nodeId, 0.0);
        int currentTransactions = currentTransactionLoad.getOrDefault(nodeId, 0);
        
        // Calculate utilization ratios (lower is better)
        double cpuUtilization = currentCpu / metrics.getCpuUtilization();
        double memoryUtilization = currentMemory / metrics.getMemoryUsage();
        double transactionUtilization = (double) currentTransactions / metrics.getTransactionsPerSec();
        
        // Score based on available capacity and node characteristics
        double capacityScore = (1.0 - cpuUtilization) * 0.4 + 
                              (1.0 - memoryUtilization) * 0.3 + 
                              (1.0 - transactionUtilization) * 0.3;
        
        // Factor in node weight and performance characteristics
        double performanceScore = weight * (1.0 / metrics.getLatency()) * (metrics.getThroughput() / 1000.0);
        
        // Priority bonus
        double priorityBonus = request.getPriority() / 10.0;
        
        return capacityScore * 0.6 + performanceScore * 0.3 + priorityBonus * 0.1;
    }
    
    private NodeId findLeastLoadedNode(List<NodeId> availableNodes) {
        return availableNodes.stream()
                .min(Comparator.comparingDouble(nodeId -> {
                    double cpu = currentCpuLoad.getOrDefault(nodeId, 0.0);
                    double memory = currentMemoryLoad.getOrDefault(nodeId, 0.0);
                    int transactions = currentTransactionLoad.getOrDefault(nodeId, 0);
                    return cpu + memory + transactions; // Simple combined load metric
                }))
                .orElse(availableNodes.get(0));
    }
    
    private void updateLoadAfterAllocation(NodeId nodeId, ServiceRequest request) {
        currentCpuLoad.merge(nodeId, request.getCpuRequirement(), Double::sum);
        currentMemoryLoad.merge(nodeId, request.getMemoryRequirement(), Double::sum);
        currentTransactionLoad.merge(nodeId, request.getTransactionLoad(), Integer::sum);
    }
    
    private List<NodeId> getAvailableNodes() {
        return Arrays.asList(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1);
    }
    
    @Override
    public void updateNodeWeights(Map<NodeId, Double> weights) {
        Objects.requireNonNull(weights, "Node weights cannot be null");
        
        for (Map.Entry<NodeId, Double> entry : weights.entrySet()) {
            if (entry.getValue() < 0.0 || entry.getValue() > 10.0) {
                throw new IllegalArgumentException("Node weight must be between 0-10, got: " + entry.getValue());
            }
        }
        
        this.nodeWeights.putAll(weights);
        this.lastUpdateTimestamp = System.currentTimeMillis();
        
        logger.info("Updated node weights: {}", weights);
    }
    
    @Override
    public void migrateService(ServiceId service, NodeId from, NodeId to) {
        Objects.requireNonNull(service, "Service ID cannot be null");
        Objects.requireNonNull(from, "Source node cannot be null");
        Objects.requireNonNull(to, "Destination node cannot be null");
        
        if (from.equals(to)) {
            logger.warn("Attempted to migrate service {} to the same node {}", service, from);
            return;
        }
        
        NodeId currentNode = serviceAllocations.get(service);
        if (currentNode == null) {
            throw new IllegalArgumentException("Service " + service + " is not currently allocated");
        }
        
        if (!currentNode.equals(from)) {
            throw new IllegalArgumentException("Service " + service + " is not on node " + from + 
                                             ", currently on " + currentNode);
        }
        
        // Update allocation
        serviceAllocations.put(service, to);
        migrationCounter.incrementAndGet();
        this.lastUpdateTimestamp = System.currentTimeMillis();
        
        logger.info("Migrated service {} from {} to {}", service, from, to);
    }
    
    @Override
    public LoadBalancingMetrics getMetrics() {
        Map<NodeId, Double> cpuDistribution = new HashMap<>(currentCpuLoad);
        Map<NodeId, Double> memoryDistribution = new HashMap<>(currentMemoryLoad);
        Map<NodeId, Integer> transactionDistribution = new HashMap<>(currentTransactionLoad);
        
        double loadBalanceIndex = calculateLoadBalanceIndex();
        int totalServices = serviceAllocations.size();
        int migrations = migrationCounter.get();
        
        return new LoadBalancingMetrics(
            cpuDistribution,
            memoryDistribution, 
            transactionDistribution,
            loadBalanceIndex,
            totalServices,
            migrations,
            lastUpdateTimestamp
        );
    }
    
    private double calculateLoadBalanceIndex() {
        if (currentCpuLoad.isEmpty()) return 1.0;
        
        // Calculate coefficient of variation for CPU load (lower is more balanced)
        double[] cpuLoads = currentCpuLoad.values().stream().mapToDouble(Double::doubleValue).toArray();
        double mean = Arrays.stream(cpuLoads).average().orElse(0.0);
        
        if (mean == 0.0) return 1.0;
        
        double variance = Arrays.stream(cpuLoads)
                .map(load -> Math.pow(load - mean, 2))
                .average().orElse(0.0);
        
        double coefficientOfVariation = Math.sqrt(variance) / mean;
        
        // Convert to balance index (1 = perfectly balanced, 0 = completely unbalanced)
        return Math.max(0.0, 1.0 - Math.min(1.0, coefficientOfVariation));
    }
    
    @Override
    public void updateNodeMetrics(Map<NodeId, NodeMetrics> nodeMetrics) {
        Objects.requireNonNull(nodeMetrics, "Node metrics cannot be null");
        
        this.currentMetrics.putAll(nodeMetrics);
        this.lastUpdateTimestamp = System.currentTimeMillis();
        
        logger.debug("Updated metrics for {} nodes", nodeMetrics.size());
    }
    
    @Override
    public void handleTrafficFluctuation(TrafficPattern trafficPattern) {
        Objects.requireNonNull(trafficPattern, "Traffic pattern cannot be null");
        
        // Adjust strategy based on traffic pattern
        switch (trafficPattern.getPatternType()) {
            case BURST:
                // Use resource-aware for burst traffic
                setStrategy(LoadBalancingStrategy.RESOURCE_AWARE);
                break;
            case STEADY:
                // Use weighted round robin for steady traffic
                setStrategy(LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN);
                break;
            case DECLINING:
                // Use least connections for declining traffic
                setStrategy(LoadBalancingStrategy.LEAST_CONNECTIONS);
                break;
        }
        
        // Update weights based on traffic intensity
        double intensityFactor = trafficPattern.getIntensity() / 100.0; // Normalize to 0-1
        Map<NodeId, Double> adjustedWeights = new HashMap<>();
        
        for (Map.Entry<NodeId, Double> entry : nodeWeights.entrySet()) {
            double adjustedWeight = entry.getValue() * (0.5 + intensityFactor * 0.5);
            adjustedWeights.put(entry.getKey(), adjustedWeight);
        }
        
        updateNodeWeights(adjustedWeights);
        
        logger.info("Handled traffic fluctuation: pattern={}, intensity={}", 
                   trafficPattern.getPatternType(), trafficPattern.getIntensity());
    }
    
    @Override
    public String getCurrentStrategy() {
        return currentStrategy;
    }
    
    @Override
    public void setStrategy(String strategy) {
        Objects.requireNonNull(strategy, "Strategy cannot be null");
        
        if (!isValidStrategy(strategy)) {
            throw new IllegalArgumentException("Invalid strategy: " + strategy);
        }
        
        this.currentStrategy = strategy;
        this.lastUpdateTimestamp = System.currentTimeMillis();
        
        logger.info("Changed load balancing strategy to: {}", strategy);
    }
    
    private boolean isValidStrategy(String strategy) {
        return LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN.equals(strategy) ||
               LoadBalancingStrategy.LEAST_CONNECTIONS.equals(strategy) ||
               LoadBalancingStrategy.RESOURCE_AWARE.equals(strategy);
    }
}