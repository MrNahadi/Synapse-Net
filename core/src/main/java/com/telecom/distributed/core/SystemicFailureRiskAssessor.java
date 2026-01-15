package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Assesses systemic failure risk across the distributed telecom system.
 * Identifies nodes most likely to precipitate systemic failure by analyzing
 * high load scenarios, correlated failures, cascading effects, and dependency chains.
 * 
 * Requirements: 16.1, 16.2, 16.3, 16.4
 */
public class SystemicFailureRiskAssessor {
    
    private static final double HIGH_LOAD_THRESHOLD = 0.75;
    private static final double CRITICAL_RESOURCE_THRESHOLD = 0.85;
    private static final double BYZANTINE_RISK_MULTIPLIER = 2.0;
    private static final double CORE_LAYER_CRITICALITY = 0.9;
    private static final double EDGE_LAYER_CRITICALITY = 0.6;
    private static final double CLOUD_LAYER_CRITICALITY = 0.7;
    
    private final NetworkTopology networkTopology;
    
    public SystemicFailureRiskAssessor(NetworkTopology networkTopology) {
        this.networkTopology = Objects.requireNonNull(networkTopology, "Network topology cannot be null");
    }
    
    /**
     * Identifies nodes most likely to precipitate systemic failure.
     * Analyzes high load scenarios and correlated failures.
     * 
     * @param nodeMetrics Current metrics for all nodes
     * @param nodeConfigurations Configuration for all nodes
     * @return List of systemic failure risks, sorted by risk score (highest first)
     */
    public List<SystemicFailureRisk> assessSystemicRisks(
            Map<NodeId, NodeMetrics> nodeMetrics,
            Map<NodeId, NodeConfiguration> nodeConfigurations) {
        
        validateInputs(nodeMetrics, nodeConfigurations);
        
        List<SystemicFailureRisk> risks = new ArrayList<>();
        
        for (Map.Entry<NodeId, NodeMetrics> entry : nodeMetrics.entrySet()) {
            NodeId nodeId = entry.getKey();
            NodeMetrics metrics = entry.getValue();
            NodeConfiguration config = nodeConfigurations.get(nodeId);
            
            // Calculate risk components
            double loadRisk = calculateLoadRisk(metrics);
            double failureTypeRisk = calculateFailureTypeRisk(config.getFailureModel().getFailureType());
            double criticalityScore = calculateCriticalityScore(config, nodeMetrics);
            double dependencyRisk = calculateDependencyRisk(nodeId, nodeConfigurations);
            double correlatedFailureRisk = calculateCorrelatedFailureRisk(nodeId, metrics, nodeMetrics);
            
            // Calculate cascade risk
            CascadeAnalysis cascadeAnalysis = analyzeCascadingEffects(nodeId, nodeConfigurations, nodeMetrics);
            double cascadeRisk = cascadeAnalysis.getCascadeProbability();
            
            // Combine risk factors
            double overallRisk = combineRiskFactors(loadRisk, failureTypeRisk, criticalityScore,
                                                   dependencyRisk, correlatedFailureRisk, cascadeRisk);
            
            // Generate description and mitigation strategies
            String description = generateRiskDescription(nodeId, metrics, config, overallRisk, cascadeAnalysis);
            Set<String> mitigationStrategies = generateMitigationStrategies(nodeId, config, overallRisk);
            
            risks.add(new SystemicFailureRisk(
                nodeId, overallRisk, config.getFailureModel().getFailureType(),
                criticalityScore, cascadeAnalysis.getAffectedNodes(),
                cascadeRisk, description, mitigationStrategies
            ));
        }
        
        // Sort by risk score (highest first)
        risks.sort((a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));
        
        return risks;
    }
    
    /**
     * Analyzes cascading effects and dependency chains for a node.
     * 
     * @param originNode Node to analyze
     * @param nodeConfigurations All node configurations
     * @param nodeMetrics Current metrics for all nodes
     * @return Cascade analysis result
     */
    public CascadeAnalysis analyzeCascadingEffects(
            NodeId originNode,
            Map<NodeId, NodeConfiguration> nodeConfigurations,
            Map<NodeId, NodeMetrics> nodeMetrics) {
        
        Set<NodeId> affectedNodes = new HashSet<>();
        List<List<NodeId>> cascadePaths = new ArrayList<>();
        
        // Find all dependency paths from origin node
        findCascadePaths(originNode, nodeConfigurations, new ArrayList<>(), 
                        cascadePaths, affectedNodes, new HashSet<>());
        
        // Calculate cascade probability based on node metrics and dependencies
        double cascadeProbability = calculateCascadeProbability(originNode, affectedNodes, 
                                                               nodeMetrics, nodeConfigurations);
        
        // Find maximum cascade depth
        int maxDepth = cascadePaths.stream()
            .mapToInt(List::size)
            .max()
            .orElse(0);
        
        String description = String.format(
            "Node %s failure could cascade to %d nodes through %d dependency paths (max depth: %d)",
            originNode.getId(), affectedNodes.size(), cascadePaths.size(), maxDepth
        );
        
        return new CascadeAnalysis(originNode, cascadePaths, affectedNodes, 
                                  cascadeProbability, maxDepth, description);
    }
    
    /**
     * Calculates load risk based on high load scenarios.
     */
    private double calculateLoadRisk(NodeMetrics metrics) {
        // Normalize metrics to 0-1 scale
        double cpuLoad = (metrics.getCpuUtilization() - 45.0) / (72.0 - 45.0);
        double memoryLoad = (metrics.getMemoryUsage() - 4.0) / (16.0 - 4.0);
        double transactionLoad = (metrics.getTransactionsPerSec() - 100.0) / (300.0 - 100.0);
        double lockLoad = (metrics.getLockContention() - 5.0) / (15.0 - 5.0);
        
        // High load scenario: exponential risk increase above threshold
        double avgLoad = (cpuLoad + memoryLoad + transactionLoad + lockLoad) / 4.0;
        
        if (avgLoad > HIGH_LOAD_THRESHOLD) {
            return Math.min(1.0, avgLoad * Math.exp(avgLoad - HIGH_LOAD_THRESHOLD));
        }
        
        return avgLoad;
    }
    
    /**
     * Calculates risk based on failure type.
     * Byzantine failures have highest risk due to arbitrary behavior.
     */
    private double calculateFailureTypeRisk(FailureType failureType) {
        switch (failureType) {
            case BYZANTINE:
                return 0.9; // Highest risk - arbitrary malicious behavior
            case OMISSION:
                return 0.6; // Medium risk - message loss can cascade
            case CRASH:
                return 0.4; // Lower risk - clean failure, easier to detect
            case NETWORK_PARTITION:
                return 0.7; // High risk - can split system
            default:
                return 0.5;
        }
    }
    
    /**
     * Calculates criticality score based on node layer and services.
     */
    private double calculateCriticalityScore(NodeConfiguration config, Map<NodeId, NodeMetrics> allMetrics) {
        // Base criticality from layer
        double layerCriticality;
        switch (config.getLayer()) {
            case CORE:
                layerCriticality = CORE_LAYER_CRITICALITY;
                break;
            case CLOUD:
                layerCriticality = CLOUD_LAYER_CRITICALITY;
                break;
            case EDGE:
                layerCriticality = EDGE_LAYER_CRITICALITY;
                break;
            default:
                layerCriticality = 0.5;
                break;
        }
        
        // Adjust for service types
        double serviceCriticality = config.getSupportedServices().stream()
            .mapToDouble(this::getServiceCriticality)
            .average()
            .orElse(0.5);
        
        // Adjust for relative performance
        double performanceCriticality = calculatePerformanceCriticality(config.getNodeId(), allMetrics);
        
        return (layerCriticality * 0.5 + serviceCriticality * 0.3 + performanceCriticality * 0.2);
    }
    
    /**
     * Gets criticality score for a service type.
     */
    private double getServiceCriticality(ServiceType serviceType) {
        switch (serviceType) {
            case TRANSACTION_COMMIT:
                return 1.0;
            case RPC_CALL:
            case RPC_HANDLING:
                return 0.8;
            case REPLICATION:
            case DATA_REPLICATION:
                return 0.7;
            case RECOVERY:
            case RECOVERY_OPERATIONS:
                return 0.9;
            case LOAD_BALANCING:
                return 0.8;
            case ANALYTICS:
                return 0.5;
            case MIGRATION:
            case MIGRATION_SERVICES:
                return 0.6;
            case DISTRIBUTED_SHARED_MEMORY:
                return 0.7;
            default:
                return 0.5;
        }
    }
    
    /**
     * Calculates performance criticality based on relative metrics.
     */
    private double calculatePerformanceCriticality(NodeId nodeId, Map<NodeId, NodeMetrics> allMetrics) {
        NodeMetrics metrics = allMetrics.get(nodeId);
        
        // Nodes with best performance are more critical (single point of excellence)
        double latencyRank = rankMetric(metrics.getLatency(), allMetrics, NodeMetrics::getLatency, true);
        double throughputRank = rankMetric(metrics.getThroughput(), allMetrics, NodeMetrics::getThroughput, false);
        
        return (latencyRank + throughputRank) / 2.0;
    }
    
    /**
     * Ranks a metric across all nodes (0.0 = worst, 1.0 = best).
     */
    private double rankMetric(double value, Map<NodeId, NodeMetrics> allMetrics,
                            java.util.function.Function<NodeMetrics, Double> extractor,
                            boolean lowerIsBetter) {
        List<Double> values = allMetrics.values().stream()
            .map(extractor)
            .sorted()
            .collect(Collectors.toList());
        
        int index = values.indexOf(value);
        double rank = (double) index / (values.size() - 1);
        
        return lowerIsBetter ? (1.0 - rank) : rank;
    }
    
    /**
     * Calculates dependency risk based on number of dependent nodes.
     */
    private double calculateDependencyRisk(NodeId nodeId, Map<NodeId, NodeConfiguration> configs) {
        Set<NodeId> dependents = findDependentNodes(nodeId, configs);
        
        // More dependents = higher risk
        double dependencyRatio = (double) dependents.size() / (configs.size() - 1);
        
        return Math.min(1.0, dependencyRatio * 1.5);
    }
    
    /**
     * Finds all nodes that depend on the given node.
     */
    private Set<NodeId> findDependentNodes(NodeId nodeId, Map<NodeId, NodeConfiguration> configs) {
        Set<NodeId> dependents = new HashSet<>();
        
        for (Map.Entry<NodeId, NodeConfiguration> entry : configs.entrySet()) {
            NodeId otherNode = entry.getKey();
            if (!otherNode.equals(nodeId)) {
                NetworkTopology topology = entry.getValue().getNetworkTopology();
                if (topology.isConnected(otherNode, nodeId)) {
                    dependents.add(otherNode);
                }
            }
        }
        
        return dependents;
    }
    
    /**
     * Calculates correlated failure risk based on similar load patterns.
     */
    private double calculateCorrelatedFailureRisk(NodeId nodeId, NodeMetrics metrics,
                                                  Map<NodeId, NodeMetrics> allMetrics) {
        double correlationSum = 0.0;
        int count = 0;
        
        for (Map.Entry<NodeId, NodeMetrics> entry : allMetrics.entrySet()) {
            if (!entry.getKey().equals(nodeId)) {
                double correlation = calculateMetricCorrelation(metrics, entry.getValue());
                correlationSum += correlation;
                count++;
            }
        }
        
        double avgCorrelation = count > 0 ? correlationSum / count : 0.0;
        
        // High correlation means failures might be correlated
        return avgCorrelation * 0.8; // Scale down as it's a contributing factor
    }
    
    /**
     * Calculates correlation between two nodes' metrics.
     */
    private double calculateMetricCorrelation(NodeMetrics m1, NodeMetrics m2) {
        // Normalize and compare key metrics
        double cpuSimilarity = 1.0 - Math.abs(m1.getCpuUtilization() - m2.getCpuUtilization()) / 72.0;
        double memorySimilarity = 1.0 - Math.abs(m1.getMemoryUsage() - m2.getMemoryUsage()) / 16.0;
        double transactionSimilarity = 1.0 - Math.abs(m1.getTransactionsPerSec() - m2.getTransactionsPerSec()) / 300.0;
        
        return (cpuSimilarity + memorySimilarity + transactionSimilarity) / 3.0;
    }
    
    /**
     * Finds all cascade paths from origin node using DFS.
     */
    private void findCascadePaths(NodeId current, Map<NodeId, NodeConfiguration> configs,
                                 List<NodeId> currentPath, List<List<NodeId>> allPaths,
                                 Set<NodeId> affectedNodes, Set<NodeId> visited) {
        if (visited.contains(current)) {
            return; // Avoid cycles
        }
        
        visited.add(current);
        currentPath.add(current);
        
        Set<NodeId> dependents = findDependentNodes(current, configs);
        
        if (!dependents.isEmpty()) {
            for (NodeId dependent : dependents) {
                if (!visited.contains(dependent)) {
                    affectedNodes.add(dependent);
                    findCascadePaths(dependent, configs, new ArrayList<>(currentPath), 
                                   allPaths, affectedNodes, new HashSet<>(visited));
                }
            }
        }
        
        if (currentPath.size() > 1) {
            allPaths.add(new ArrayList<>(currentPath));
        }
    }
    
    /**
     * Calculates probability of cascade failure.
     */
    private double calculateCascadeProbability(NodeId originNode, Set<NodeId> affectedNodes,
                                              Map<NodeId, NodeMetrics> nodeMetrics,
                                              Map<NodeId, NodeConfiguration> configs) {
        if (affectedNodes.isEmpty()) {
            return 0.0;
        }
        
        // Base probability from origin node load
        NodeMetrics originMetrics = nodeMetrics.get(originNode);
        double originLoadRisk = calculateLoadRisk(originMetrics);
        
        // Probability increases with number of affected nodes
        double cascadeScale = Math.min(1.0, (double) affectedNodes.size() / configs.size());
        
        // Probability increases if affected nodes are also under high load
        double affectedLoadRisk = affectedNodes.stream()
            .map(nodeMetrics::get)
            .mapToDouble(this::calculateLoadRisk)
            .average()
            .orElse(0.0);
        
        return Math.min(1.0, originLoadRisk * cascadeScale * (1.0 + affectedLoadRisk));
    }
    
    /**
     * Combines multiple risk factors into overall risk score.
     */
    private double combineRiskFactors(double loadRisk, double failureTypeRisk, double criticalityScore,
                                     double dependencyRisk, double correlatedFailureRisk, double cascadeRisk) {
        // Weighted combination
        double weightedSum = loadRisk * 0.25 +
                           failureTypeRisk * 0.20 +
                           criticalityScore * 0.20 +
                           dependencyRisk * 0.15 +
                           correlatedFailureRisk * 0.10 +
                           cascadeRisk * 0.10;
        
        // Non-linear amplification for high-risk scenarios
        if (weightedSum > 0.7 && cascadeRisk > 0.5) {
            weightedSum = Math.min(1.0, weightedSum * 1.2);
        }
        
        return Math.min(1.0, weightedSum);
    }
    
    /**
     * Generates human-readable risk description.
     */
    private String generateRiskDescription(NodeId nodeId, NodeMetrics metrics, 
                                          NodeConfiguration config, double riskScore,
                                          CascadeAnalysis cascadeAnalysis) {
        return String.format(
            "Node %s (%s layer, %s failures) has systemic risk score %.3f. " +
            "Current load: CPU=%.1f%%, Memory=%.1fGB, Transactions=%d/sec. " +
            "Failure could cascade to %d nodes with probability %.3f.",
            nodeId.getId(), config.getLayer(), config.getFailureModel().getFailureType(),
            riskScore, metrics.getCpuUtilization(), metrics.getMemoryUsage(),
            metrics.getTransactionsPerSec(), cascadeAnalysis.getAffectedNodes().size(),
            cascadeAnalysis.getCascadeProbability()
        );
    }
    
    /**
     * Generates mitigation strategies based on risk profile.
     */
    private Set<String> generateMitigationStrategies(NodeId nodeId, NodeConfiguration config, double riskScore) {
        Set<String> strategies = new HashSet<>();
        
        if (riskScore > 0.7) {
            strategies.add("Implement redundancy with hot standby");
            strategies.add("Increase monitoring frequency and alerting");
            strategies.add("Pre-position failover resources");
        }
        
        if (config.getFailureModel().getFailureType() == FailureType.BYZANTINE) {
            strategies.add("Deploy Byzantine fault tolerance protocols");
            strategies.add("Implement multi-node verification");
        }
        
        if (config.getLayer() == NodeLayer.CORE) {
            strategies.add("Replicate critical services across multiple core nodes");
            strategies.add("Implement circuit breakers for dependent services");
        }
        
        strategies.add("Load shedding during high utilization");
        strategies.add("Graceful degradation planning");
        
        return strategies;
    }
    
    /**
     * Validates input parameters.
     */
    private void validateInputs(Map<NodeId, NodeMetrics> nodeMetrics,
                               Map<NodeId, NodeConfiguration> nodeConfigurations) {
        Objects.requireNonNull(nodeMetrics, "Node metrics cannot be null");
        Objects.requireNonNull(nodeConfigurations, "Node configurations cannot be null");
        
        if (nodeMetrics.isEmpty() || nodeConfigurations.isEmpty()) {
            throw new IllegalArgumentException("Node metrics and configurations cannot be empty");
        }
        
        if (!nodeMetrics.keySet().equals(nodeConfigurations.keySet())) {
            throw new IllegalArgumentException("Node metrics and configurations must have matching node IDs");
        }
    }
}
