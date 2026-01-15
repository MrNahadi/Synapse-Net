package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements quantitative throughput-latency trade-off analysis using multi-objective optimization.
 * Provides Pareto analysis and optimal system configuration parameter recommendations.
 * Integrates with replication strategies to provide comprehensive optimization.
 */
public class ThroughputLatencyTradeOffAnalyzer {
    
    private final ReplicationManager replicationManager;
    private final PerformanceAnalyzer performanceAnalyzer;
    
    // Trade-off analysis parameters
    private static final double THROUGHPUT_WEIGHT = 0.6;
    private static final double LATENCY_WEIGHT = 0.4;
    private static final int PARETO_SAMPLE_SIZE = 100;
    private static final double DOMINANCE_THRESHOLD = 0.01;
    
    public ThroughputLatencyTradeOffAnalyzer(ReplicationManager replicationManager,
                                           PerformanceAnalyzer performanceAnalyzer) {
        this.replicationManager = Objects.requireNonNull(replicationManager, "Replication manager cannot be null");
        this.performanceAnalyzer = Objects.requireNonNull(performanceAnalyzer, "Performance analyzer cannot be null");
    }
    
    /**
     * Performs quantitative trade-off analysis between throughput and latency.
     * Uses replication strategies from task 11 as input for optimization.
     */
    public TradeOffAnalysis analyzeTradeOffs(Map<NodeId, NodeMetrics> currentMetrics,
                                           Set<ServiceId> services) {
        Objects.requireNonNull(currentMetrics, "Current metrics cannot be null");
        Objects.requireNonNull(services, "Services cannot be null");
        
        // Generate candidate configurations using replication strategies
        List<SystemConfiguration> candidateConfigurations = generateCandidateConfigurations(services);
        
        // Evaluate each configuration for throughput and latency
        List<ParetoPoint> evaluatedPoints = evaluateConfigurations(candidateConfigurations, currentMetrics);
        
        // Compute Pareto frontier using multi-objective optimization
        List<ParetoPoint> paretoFrontier = computeParetoFrontier(evaluatedPoints);
        
        // Select optimal configuration from Pareto frontier
        SystemConfiguration optimalConfiguration = selectOptimalConfiguration(paretoFrontier);
        
        // Calculate trade-off ratio
        double tradeOffRatio = calculateTradeOffRatio(paretoFrontier);
        
        // Generate analysis description
        String analysisDescription = generateAnalysisDescription(paretoFrontier, optimalConfiguration, currentMetrics);
        
        // Calculate confidence score
        double confidenceScore = calculateConfidenceScore(paretoFrontier, evaluatedPoints);
        
        return new TradeOffAnalysis(paretoFrontier, optimalConfiguration, tradeOffRatio, 
                                  analysisDescription, confidenceScore);
    }
    
    /**
     * Generates candidate system configurations using different replication strategies.
     * Integrates with replication strategies from task 11.
     */
    private List<SystemConfiguration> generateCandidateConfigurations(Set<ServiceId> services) {
        List<SystemConfiguration> configurations = new ArrayList<>();
        
        // Get all available nodes
        Set<NodeId> allNodes = Set.of(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1);
        
        // Generate base node configurations
        Map<NodeId, NodeConfiguration> baseNodeConfigs = generateBaseNodeConfigurations(allNodes);
        
        // Generate different replication strategy combinations
        List<Map<ServiceId, ReplicationStrategy>> replicationCombinations = 
            generateReplicationStrategyCombinations(services);
        
        // Generate different load balancing strategies
        List<String> loadBalancingStrategies = generateLoadBalancingStrategies();
        
        // Generate parameter variations
        List<Integer> transactionLimits = Arrays.asList(50, 100, 200, 300);
        List<Double> bufferSizes = Arrays.asList(1024.0, 2048.0, 4096.0, 8192.0);
        List<Integer> poolSizes = Arrays.asList(10, 20, 50, 100);
        List<Long> timeouts = Arrays.asList(5000L, 10000L, 15000L, 30000L);
        
        // Combine all parameters to create candidate configurations
        for (Map<ServiceId, ReplicationStrategy> replicationStrategies : replicationCombinations) {
            for (String lbStrategy : loadBalancingStrategies) {
                for (Integer txLimit : transactionLimits) {
                    for (Double bufferSize : bufferSizes) {
                        for (Integer poolSize : poolSizes) {
                            for (Long timeout : timeouts) {
                                double score = calculateConfigurationScore(replicationStrategies, lbStrategy, 
                                                                         txLimit, bufferSize, poolSize, timeout);
                                
                                SystemConfiguration config = new SystemConfiguration(
                                    baseNodeConfigs, replicationStrategies, lbStrategy,
                                    txLimit, bufferSize, poolSize, timeout, score
                                );
                                configurations.add(config);
                            }
                        }
                    }
                }
            }
        }
        
        // Limit to reasonable sample size for analysis
        return configurations.stream()
            .sorted((c1, c2) -> Double.compare(c2.getConfigurationScore(), c1.getConfigurationScore()))
            .limit(PARETO_SAMPLE_SIZE)
            .collect(Collectors.toList());
    }
    
    /**
     * Evaluates configurations for throughput and latency performance.
     */
    private List<ParetoPoint> evaluateConfigurations(List<SystemConfiguration> configurations,
                                                   Map<NodeId, NodeMetrics> currentMetrics) {
        List<ParetoPoint> points = new ArrayList<>();
        
        for (SystemConfiguration config : configurations) {
            // Estimate throughput based on configuration
            double estimatedThroughput = estimateThroughput(config, currentMetrics);
            
            // Estimate latency based on configuration
            double estimatedLatency = estimateLatency(config, currentMetrics);
            
            // Calculate dominance score
            double dominanceScore = calculateDominanceScore(estimatedThroughput, estimatedLatency);
            
            // Determine if this is an optimal point (will be refined in Pareto analysis)
            boolean isOptimal = false;
            
            ParetoPoint point = new ParetoPoint(estimatedThroughput, estimatedLatency, config, 
                                              dominanceScore, isOptimal);
            points.add(point);
        }
        
        return points;
    }
    
    /**
     * Computes the Pareto frontier using multi-objective optimization.
     */
    private List<ParetoPoint> computeParetoFrontier(List<ParetoPoint> evaluatedPoints) {
        List<ParetoPoint> paretoFrontier = new ArrayList<>();
        
        for (ParetoPoint candidate : evaluatedPoints) {
            boolean isDominated = false;
            
            // Check if candidate is dominated by any other point
            for (ParetoPoint other : evaluatedPoints) {
                if (other != candidate && other.dominates(candidate)) {
                    isDominated = true;
                    break;
                }
            }
            
            // If not dominated, it's on the Pareto frontier
            if (!isDominated) {
                // Create new point marked as optimal
                ParetoPoint optimalPoint = new ParetoPoint(
                    candidate.getThroughput(), candidate.getLatency(),
                    candidate.getConfiguration(), candidate.getDominanceScore(), true
                );
                paretoFrontier.add(optimalPoint);
            }
        }
        
        // Sort Pareto frontier by throughput (descending) for better visualization
        paretoFrontier.sort((p1, p2) -> Double.compare(p2.getThroughput(), p1.getThroughput()));
        
        return paretoFrontier;
    }
    
    /**
     * Selects the optimal configuration from the Pareto frontier.
     * Uses weighted combination of throughput and latency objectives.
     */
    private SystemConfiguration selectOptimalConfiguration(List<ParetoPoint> paretoFrontier) {
        if (paretoFrontier.isEmpty()) {
            // Return a default configuration if no Pareto points exist
            return createDefaultSystemConfiguration();
        }
        
        ParetoPoint bestPoint = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        // Normalize throughput and latency for fair comparison
        double maxThroughput = paretoFrontier.stream().mapToDouble(ParetoPoint::getThroughput).max().orElse(1.0);
        double minLatency = paretoFrontier.stream().mapToDouble(ParetoPoint::getLatency).min().orElse(1.0);
        double maxLatency = paretoFrontier.stream().mapToDouble(ParetoPoint::getLatency).max().orElse(1.0);
        
        for (ParetoPoint point : paretoFrontier) {
            // Normalize objectives (higher is better for both)
            double normalizedThroughput = point.getThroughput() / maxThroughput;
            double normalizedLatency = (maxLatency - point.getLatency()) / (maxLatency - minLatency);
            
            // Handle case where all latencies are the same
            if (maxLatency == minLatency) {
                normalizedLatency = 1.0;
            }
            
            // Calculate weighted score
            double score = THROUGHPUT_WEIGHT * normalizedThroughput + LATENCY_WEIGHT * normalizedLatency;
            
            if (score > bestScore) {
                bestScore = score;
                bestPoint = point;
            }
        }
        
        return bestPoint != null ? bestPoint.getConfiguration() : createDefaultSystemConfiguration();
    }
    
    /**
     * Calculates the trade-off ratio between throughput and latency.
     */
    private double calculateTradeOffRatio(List<ParetoPoint> paretoFrontier) {
        if (paretoFrontier.size() < 2) {
            return 1.0; // No trade-off if only one point
        }
        
        // Calculate average slope of Pareto frontier
        double totalSlope = 0.0;
        int slopeCount = 0;
        
        for (int i = 0; i < paretoFrontier.size() - 1; i++) {
            ParetoPoint p1 = paretoFrontier.get(i);
            ParetoPoint p2 = paretoFrontier.get(i + 1);
            
            double throughputDiff = p2.getThroughput() - p1.getThroughput();
            double latencyDiff = p2.getLatency() - p1.getLatency();
            
            if (Math.abs(throughputDiff) > DOMINANCE_THRESHOLD) {
                double slope = Math.abs(latencyDiff / throughputDiff);
                totalSlope += slope;
                slopeCount++;
            }
        }
        
        return slopeCount > 0 ? totalSlope / slopeCount : 1.0;
    }
    
    /**
     * Generates analysis description with quantitative insights.
     */
    private String generateAnalysisDescription(List<ParetoPoint> paretoFrontier,
                                             SystemConfiguration optimalConfiguration,
                                             Map<NodeId, NodeMetrics> currentMetrics) {
        if (paretoFrontier.isEmpty()) {
            return "No valid configurations found for trade-off analysis";
        }
        
        ParetoPoint bestThroughput = paretoFrontier.stream()
            .max(Comparator.comparing(ParetoPoint::getThroughput))
            .orElse(paretoFrontier.get(0));
        
        ParetoPoint bestLatency = paretoFrontier.stream()
            .min(Comparator.comparing(ParetoPoint::getLatency))
            .orElse(paretoFrontier.get(0));
        
        double currentTotalThroughput = currentMetrics.values().stream()
            .mapToDouble(NodeMetrics::getThroughput)
            .sum();
        
        double currentAvgLatency = currentMetrics.values().stream()
            .mapToDouble(NodeMetrics::getLatency)
            .average()
            .orElse(0.0);
        
        return String.format(
            "Trade-off analysis identified %d Pareto-optimal configurations. " +
            "Best throughput: %.1f Mbps (%.1f%% improvement), " +
            "Best latency: %.1f ms (%.1f%% improvement). " +
            "Optimal configuration balances both objectives with " +
            "replication factor %d and %s load balancing.",
            paretoFrontier.size(),
            bestThroughput.getThroughput(),
            ((bestThroughput.getThroughput() - currentTotalThroughput) / currentTotalThroughput) * 100,
            bestLatency.getLatency(),
            ((currentAvgLatency - bestLatency.getLatency()) / currentAvgLatency) * 100,
            getAverageReplicationFactor(optimalConfiguration),
            optimalConfiguration.getLoadBalancingStrategy()
        );
    }
    
    /**
     * Calculates confidence score based on Pareto frontier quality.
     */
    private double calculateConfidenceScore(List<ParetoPoint> paretoFrontier, List<ParetoPoint> allPoints) {
        if (paretoFrontier.isEmpty() || allPoints.isEmpty()) {
            return 0.0;
        }
        
        // Confidence based on:
        // 1. Coverage of objective space
        // 2. Number of Pareto points
        // 3. Dominance quality
        
        double coverage = (double) paretoFrontier.size() / allPoints.size();
        double diversityScore = calculateDiversityScore(paretoFrontier);
        double dominanceQuality = calculateDominanceQuality(paretoFrontier, allPoints);
        
        return (coverage * 0.3 + diversityScore * 0.4 + dominanceQuality * 0.3);
    }
    
    // Helper methods for configuration generation and evaluation
    
    private Map<NodeId, NodeConfiguration> generateBaseNodeConfigurations(Set<NodeId> nodes) {
        Map<NodeId, NodeConfiguration> configs = new HashMap<>();
        
        for (NodeId nodeId : nodes) {
            // Create default configuration based on node characteristics
            NodeConfiguration config = createDefaultNodeConfiguration(nodeId);
            configs.put(nodeId, config);
        }
        
        return configs;
    }
    
    private NodeConfiguration createDefaultNodeConfiguration(NodeId nodeId) {
        // Create configuration based on node type from dataset
        String nodeIdStr = nodeId.getId().toLowerCase();
        NodeLayer layer;
        ResourceLimits limits;
        
        if (nodeIdStr.contains("edge")) {
            layer = NodeLayer.EDGE;
            limits = new ResourceLimits(0.72, 4.5, 200, 500.0);
        } else if (nodeIdStr.contains("core")) {
            layer = NodeLayer.CORE;
            limits = new ResourceLimits(0.72, 8.0, 300, 1000.0);
        } else {
            layer = NodeLayer.CLOUD;
            limits = new ResourceLimits(0.72, 16.0, 300, 1250.0);
        }
        
        return new NodeConfiguration(nodeId, layer, createDefaultMetrics(nodeId),
                                   Set.of(ServiceType.RPC_HANDLING), createDefaultFailureModel(nodeId),
                                   limits, createDefaultNetworkTopology());
    }
    
    private NodeMetrics createDefaultMetrics(NodeId nodeId) {
        // Use same logic as ReplicationManager for consistency
        String nodeIdStr = nodeId.getId().toLowerCase();
        if (nodeIdStr.contains("edge1")) {
            return new NodeMetrics(12.0, 500.0, 0.5, 45.0, 4.0, 150, 5.0);
        } else if (nodeIdStr.contains("edge2")) {
            return new NodeMetrics(15.0, 470.0, 0.8, 50.0, 4.5, 120, 7.0);
        } else if (nodeIdStr.contains("core1")) {
            return new NodeMetrics(8.0, 1000.0, 0.3, 60.0, 8.0, 250, 10.0);
        } else if (nodeIdStr.contains("core2")) {
            return new NodeMetrics(10.0, 950.0, 0.4, 55.0, 6.0, 200, 8.0);
        } else if (nodeIdStr.contains("cloud1")) {
            return new NodeMetrics(22.0, 1250.0, 0.2, 72.0, 16.0, 300, 15.0);
        } else {
            return new NodeMetrics(15.0, 750.0, 0.5, 50.0, 8.0, 200, 10.0);
        }
    }
    
    private FailureModel createDefaultFailureModel(NodeId nodeId) {
        String nodeIdStr = nodeId.getId().toLowerCase();
        if (nodeIdStr.contains("edge1") || nodeIdStr.contains("core2")) {
            return new FailureModel(FailureType.CRASH, 0.01, 5000L, 10000L);
        } else if (nodeIdStr.contains("edge2") || nodeIdStr.contains("cloud1")) {
            return new FailureModel(FailureType.OMISSION, 0.02, 3000L, 8000L);
        } else if (nodeIdStr.contains("core1")) {
            return new FailureModel(FailureType.BYZANTINE, 0.005, 10000L, 15000L);
        } else {
            return new FailureModel(FailureType.CRASH, 0.01, 5000L, 10000L);
        }
    }
    
    private NetworkTopology createDefaultNetworkTopology() {
        return new NetworkTopology(Set.of(), Map.of(), Map.of());
    }
    
    private List<Map<ServiceId, ReplicationStrategy>> generateReplicationStrategyCombinations(Set<ServiceId> services) {
        List<Map<ServiceId, ReplicationStrategy>> combinations = new ArrayList<>();
        
        // Generate different replication strategy combinations
        List<ReplicationStrategy> strategies = Arrays.asList(
            new ReplicationStrategy(ReplicationStrategy.ReplicationType.ACTIVE, 2, Set.of(),
                                  ReplicationStrategy.ConsistencyLevel.STRONG, true),
            new ReplicationStrategy(ReplicationStrategy.ReplicationType.PASSIVE, 2, Set.of(),
                                  ReplicationStrategy.ConsistencyLevel.EVENTUAL, true),
            new ReplicationStrategy(ReplicationStrategy.ReplicationType.BYZANTINE_TOLERANT, 3, Set.of(),
                                  ReplicationStrategy.ConsistencyLevel.STRONG, true)
        );
        
        // Create combinations for each service
        for (ReplicationStrategy strategy : strategies) {
            Map<ServiceId, ReplicationStrategy> combination = new HashMap<>();
            for (ServiceId serviceId : services) {
                combination.put(serviceId, strategy);
            }
            combinations.add(combination);
        }
        
        return combinations;
    }
    
    private List<String> generateLoadBalancingStrategies() {
        return Arrays.asList(
            LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN,
            LoadBalancingStrategy.LEAST_CONNECTIONS,
            LoadBalancingStrategy.RESOURCE_AWARE
        );
    }
    
    private double calculateConfigurationScore(Map<ServiceId, ReplicationStrategy> replicationStrategies,
                                             String lbStrategy,
                                             int txLimit, double bufferSize, int poolSize, long timeout) {
        // Score based on expected performance impact
        double replicationScore = replicationStrategies.values().stream()
            .mapToDouble(rs -> rs.getReplicationFactor() * 0.1)
            .average().orElse(0.2);
        
        double lbScore = lbStrategy.equals(LoadBalancingStrategy.RESOURCE_AWARE) ? 0.3 : 0.2;
        double txScore = Math.min(1.0, txLimit / 300.0) * 0.2;
        double bufferScore = Math.min(1.0, bufferSize / 8192.0) * 0.1;
        double poolScore = Math.min(1.0, poolSize / 100.0) * 0.1;
        double timeoutScore = (30000.0 - timeout) / 30000.0 * 0.1;
        
        return replicationScore + lbScore + txScore + bufferScore + poolScore + timeoutScore;
    }
    
    private double estimateThroughput(SystemConfiguration config, Map<NodeId, NodeMetrics> currentMetrics) {
        // Estimate throughput based on configuration parameters
        double baseThroughput = currentMetrics.values().stream()
            .mapToDouble(NodeMetrics::getThroughput)
            .sum();
        
        // Adjust based on configuration
        double replicationOverhead = getAverageReplicationFactor(config) * 0.1;
        double poolingBonus = Math.min(0.3, config.getConnectionPoolSize() / 100.0 * 0.3);
        double bufferBonus = Math.min(0.2, config.getNetworkBufferSize() / 8192.0 * 0.2);
        
        return baseThroughput * (1.0 - replicationOverhead + poolingBonus + bufferBonus);
    }
    
    private double estimateLatency(SystemConfiguration config, Map<NodeId, NodeMetrics> currentMetrics) {
        // Estimate latency based on configuration parameters
        double baseLatency = currentMetrics.values().stream()
            .mapToDouble(NodeMetrics::getLatency)
            .average()
            .orElse(15.0);
        
        // Adjust based on configuration
        double replicationLatency = getAverageReplicationFactor(config) * 2.0;
        double timeoutPenalty = config.getTransactionTimeout() > 15000 ? 3.0 : 0.0;
        double poolingBonus = Math.min(3.0, config.getConnectionPoolSize() / 50.0);
        
        return Math.max(1.0, baseLatency + replicationLatency + timeoutPenalty - poolingBonus);
    }
    
    private double calculateDominanceScore(double throughput, double latency) {
        // Higher throughput and lower latency are better
        return (throughput / 1250.0) + ((22.0 - latency) / 22.0);
    }
    
    private int getAverageReplicationFactor(SystemConfiguration config) {
        return (int) config.getReplicationStrategies().values().stream()
            .mapToInt(ReplicationStrategy::getReplicationFactor)
            .average()
            .orElse(2.0);
    }
    
    private double calculateDiversityScore(List<ParetoPoint> paretoFrontier) {
        if (paretoFrontier.size() < 2) {
            return 0.0;
        }
        
        // Calculate average distance between consecutive points
        double totalDistance = 0.0;
        for (int i = 0; i < paretoFrontier.size() - 1; i++) {
            totalDistance += paretoFrontier.get(i).distanceTo(paretoFrontier.get(i + 1));
        }
        
        return Math.min(1.0, totalDistance / (paretoFrontier.size() - 1) / 100.0);
    }
    
    private double calculateDominanceQuality(List<ParetoPoint> paretoFrontier, List<ParetoPoint> allPoints) {
        if (allPoints.isEmpty()) {
            return 0.0;
        }
        
        // Calculate how many points each Pareto point dominates
        int totalDominated = 0;
        for (ParetoPoint paretoPoint : paretoFrontier) {
            for (ParetoPoint point : allPoints) {
                if (paretoPoint.dominates(point)) {
                    totalDominated++;
                }
            }
        }
        
        return (double) totalDominated / (allPoints.size() * paretoFrontier.size());
    }
    
    /**
     * Creates a default system configuration when no Pareto points are available.
     */
    private SystemConfiguration createDefaultSystemConfiguration() {
        Set<NodeId> allNodes = Set.of(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1);
        Map<NodeId, NodeConfiguration> nodeConfigs = generateBaseNodeConfigurations(allNodes);
        
        // Create default replication strategies
        Map<ServiceId, ReplicationStrategy> defaultReplicationStrategies = new HashMap<>();
        ReplicationStrategy defaultStrategy = new ReplicationStrategy(
            ReplicationStrategy.ReplicationType.ACTIVE, 2, Set.of(),
            ReplicationStrategy.ConsistencyLevel.STRONG, true
        );
        
        // Default load balancing strategy
        String defaultLoadBalancing = LoadBalancingStrategy.RESOURCE_AWARE;
        
        return new SystemConfiguration(
            nodeConfigs, defaultReplicationStrategies, defaultLoadBalancing,
            100, 2048.0, 20, 10000L, 0.5
        );
    }
}