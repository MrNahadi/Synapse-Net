package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements comprehensive multi-dimensional trade-off analysis across reliability, latency,
 * throughput, resource utilization, scalability, and maintainability dimensions.
 * Uses probabilistic models and simulation-based validation.
 * 
 * Validates Requirements 20.1, 20.3, 20.4, 20.5
 */
public class MultiDimensionalTradeOffAnalyzer {
    
    private final SystemicFailureRiskAssessor riskAssessor;
    private final PerformanceAnalyzer performanceAnalyzer;
    private final ThroughputLatencyTradeOffAnalyzer tradeOffAnalyzer;
    private final MonteCarloSimulator simulator;
    
    // Dimension weights for overall scoring
    private static final double RELIABILITY_WEIGHT = 0.20;
    private static final double LATENCY_WEIGHT = 0.15;
    private static final double THROUGHPUT_WEIGHT = 0.20;
    private static final double RESOURCE_WEIGHT = 0.15;
    private static final double SCALABILITY_WEIGHT = 0.15;
    private static final double MAINTAINABILITY_WEIGHT = 0.15;
    
    // Simulation parameters
    private static final int SIMULATION_RUNS = 1000;
    private static final double CONFIDENCE_THRESHOLD = 0.95;
    
    public MultiDimensionalTradeOffAnalyzer(
            SystemicFailureRiskAssessor riskAssessor,
            PerformanceAnalyzer performanceAnalyzer,
            ThroughputLatencyTradeOffAnalyzer tradeOffAnalyzer,
            MonteCarloSimulator simulator) {
        this.riskAssessor = Objects.requireNonNull(riskAssessor, "Risk assessor cannot be null");
        this.performanceAnalyzer = Objects.requireNonNull(performanceAnalyzer, "Performance analyzer cannot be null");
        this.tradeOffAnalyzer = Objects.requireNonNull(tradeOffAnalyzer, "Trade-off analyzer cannot be null");
        this.simulator = Objects.requireNonNull(simulator, "Simulator cannot be null");
    }
    
    /**
     * Performs comprehensive multi-dimensional trade-off analysis.
     * Analyzes trade-offs between reliability, latency, throughput, resource utilization,
     * scalability, and maintainability using probabilistic models and simulation validation.
     */
    public MultiDimensionalTradeOffAnalysis analyzeMultiDimensionalTradeOffs(
            Map<NodeId, NodeMetrics> currentMetrics,
            Set<ServiceId> services,
            Map<NodeId, FailureModel> failureModels) {
        
        Objects.requireNonNull(currentMetrics, "Current metrics cannot be null");
        Objects.requireNonNull(services, "Services cannot be null");
        Objects.requireNonNull(failureModels, "Failure models cannot be null");
        
        // Analyze each dimension using probabilistic models
        Map<String, Double> dimensionScores = new HashMap<>();
        Map<String, String> dimensionAnalysis = new HashMap<>();
        
        // 1. Reliability Analysis
        double reliabilityScore = analyzeReliability(currentMetrics, failureModels);
        dimensionScores.put("reliability", reliabilityScore);
        dimensionAnalysis.put("reliability", generateReliabilityAnalysis(reliabilityScore, failureModels));
        
        // 2. Latency Analysis
        double latencyScore = analyzeLatency(currentMetrics);
        dimensionScores.put("latency", latencyScore);
        dimensionAnalysis.put("latency", generateLatencyAnalysis(latencyScore, currentMetrics));
        
        // 3. Throughput Analysis
        double throughputScore = analyzeThroughput(currentMetrics);
        dimensionScores.put("throughput", throughputScore);
        dimensionAnalysis.put("throughput", generateThroughputAnalysis(throughputScore, currentMetrics));
        
        // 4. Resource Utilization Analysis
        double resourceScore = analyzeResourceUtilization(currentMetrics);
        dimensionScores.put("resource_utilization", resourceScore);
        dimensionAnalysis.put("resource_utilization", generateResourceAnalysis(resourceScore, currentMetrics));
        
        // 5. Scalability Analysis
        double scalabilityScore = analyzeScalability(currentMetrics, services);
        dimensionScores.put("scalability", scalabilityScore);
        dimensionAnalysis.put("scalability", generateScalabilityAnalysis(scalabilityScore, currentMetrics));
        
        // 6. Maintainability Analysis
        double maintainabilityScore = analyzeMaintainability(currentMetrics, services);
        dimensionScores.put("maintainability", maintainabilityScore);
        dimensionAnalysis.put("maintainability", generateMaintainabilityAnalysis(maintainabilityScore));
        
        // Calculate overall score
        double overallScore = calculateOverallScore(dimensionScores);
        
        // Generate recommended configuration based on trade-offs
        SystemConfiguration recommendedConfig = generateRecommendedConfiguration(
                dimensionScores, currentMetrics, services);
        
        // Perform simulation-based validation
        SimulationValidationResult simulationValidation = performSimulationValidation(
                recommendedConfig, currentMetrics, failureModels);
        
        // Generate critical analysis
        String criticalAnalysis = generateCriticalAnalysis(dimensionScores, dimensionAnalysis, simulationValidation);
        
        return new MultiDimensionalTradeOffAnalysis(
                dimensionScores,
                dimensionAnalysis,
                recommendedConfig,
                overallScore,
                criticalAnalysis,
                simulationValidation
        );
    }
    
    /**
     * Analyzes reliability using probabilistic failure models.
     */
    private double analyzeReliability(Map<NodeId, NodeMetrics> currentMetrics,
                                     Map<NodeId, FailureModel> failureModels) {
        // Calculate system reliability using probabilistic model
        double systemReliability = 1.0;
        
        for (Map.Entry<NodeId, FailureModel> entry : failureModels.entrySet()) {
            FailureModel model = entry.getValue();
            double nodeReliability = 1.0 - model.getFailureProbability();
            
            // Adjust for failure type severity
            double severityFactor = getFailureSeverityFactor(model.getFailureType());
            nodeReliability = nodeReliability * severityFactor;
            
            // System reliability is product of node reliabilities (series system)
            systemReliability *= nodeReliability;
        }
        
        // Normalize to 0-1 score (higher is better)
        return Math.max(0.0, Math.min(1.0, systemReliability));
    }
    
    /**
     * Analyzes latency performance across all nodes.
     */
    private double analyzeLatency(Map<NodeId, NodeMetrics> currentMetrics) {
        double avgLatency = currentMetrics.values().stream()
                .mapToDouble(NodeMetrics::getLatency)
                .average()
                .orElse(15.0);
        
        // Normalize: lower latency is better (8-22ms range)
        // Score = 1.0 at 8ms, 0.0 at 22ms
        double normalizedScore = (22.0 - avgLatency) / (22.0 - 8.0);
        return Math.max(0.0, Math.min(1.0, normalizedScore));
    }
    
    /**
     * Analyzes throughput capacity across all nodes.
     */
    private double analyzeThroughput(Map<NodeId, NodeMetrics> currentMetrics) {
        double totalThroughput = currentMetrics.values().stream()
                .mapToDouble(NodeMetrics::getThroughput)
                .sum();
        
        // Normalize: higher throughput is better (470-1250 Mbps per node, 5 nodes)
        // Max possible: 1250 * 5 = 6250 Mbps
        // Min possible: 470 * 5 = 2350 Mbps
        double normalizedScore = (totalThroughput - 2350.0) / (6250.0 - 2350.0);
        return Math.max(0.0, Math.min(1.0, normalizedScore));
    }
    
    /**
     * Analyzes resource utilization efficiency.
     */
    private double analyzeResourceUtilization(Map<NodeId, NodeMetrics> currentMetrics) {
        double avgCpu = currentMetrics.values().stream()
                .mapToDouble(NodeMetrics::getCpuUtilization)
                .average()
                .orElse(58.5);
        
        double avgMemory = currentMetrics.values().stream()
                .mapToDouble(NodeMetrics::getMemoryUsage)
                .average()
                .orElse(10.0);
        
        // Optimal CPU: 55-65% (balanced utilization)
        double cpuScore = 1.0 - Math.abs(avgCpu - 60.0) / 60.0;
        
        // Optimal Memory: 6-10 GB (balanced utilization)
        double memoryScore = 1.0 - Math.abs(avgMemory - 8.0) / 8.0;
        
        // Combined resource score
        return Math.max(0.0, Math.min(1.0, (cpuScore + memoryScore) / 2.0));
    }
    
    /**
     * Analyzes scalability potential using transaction capacity and resource headroom.
     */
    private double analyzeScalability(Map<NodeId, NodeMetrics> currentMetrics, Set<ServiceId> services) {
        // Calculate resource headroom
        double avgCpu = currentMetrics.values().stream()
                .mapToDouble(NodeMetrics::getCpuUtilization)
                .average()
                .orElse(58.5);
        
        double avgMemory = currentMetrics.values().stream()
                .mapToDouble(NodeMetrics::getMemoryUsage)
                .average()
                .orElse(10.0);
        
        // Headroom: how much capacity is left (higher is better for scalability)
        double cpuHeadroom = (72.0 - avgCpu) / 72.0;
        double memoryHeadroom = (16.0 - avgMemory) / 16.0;
        
        // Transaction capacity
        int totalTxCapacity = currentMetrics.values().stream()
                .mapToInt(NodeMetrics::getTransactionsPerSec)
                .sum();
        
        double txScore = totalTxCapacity / (300.0 * 5.0); // Normalize to max capacity
        
        // Service distribution (more services = better scalability)
        double serviceScore = Math.min(1.0, services.size() / 10.0);
        
        // Combined scalability score
        return (cpuHeadroom * 0.3 + memoryHeadroom * 0.3 + txScore * 0.2 + serviceScore * 0.2);
    }
    
    /**
     * Analyzes maintainability based on system complexity and failure handling.
     */
    private double analyzeMaintainability(Map<NodeId, NodeMetrics> currentMetrics, Set<ServiceId> services) {
        // Lower lock contention = easier to maintain
        double avgLockContention = currentMetrics.values().stream()
                .mapToDouble(NodeMetrics::getLockContention)
                .average()
                .orElse(10.0);
        
        double lockScore = (15.0 - avgLockContention) / 15.0;
        
        // Fewer services = easier to maintain
        double complexityScore = Math.max(0.0, 1.0 - (services.size() / 20.0));
        
        // Lower packet loss = more stable system
        double avgPacketLoss = currentMetrics.values().stream()
                .mapToDouble(NodeMetrics::getPacketLoss)
                .average()
                .orElse(0.5);
        
        double stabilityScore = (5.0 - avgPacketLoss) / 5.0;
        
        // Combined maintainability score
        return (lockScore * 0.4 + complexityScore * 0.3 + stabilityScore * 0.3);
    }
    
    /**
     * Calculates overall weighted score across all dimensions.
     */
    private double calculateOverallScore(Map<String, Double> dimensionScores) {
        double reliability = dimensionScores.getOrDefault("reliability", 0.0);
        double latency = dimensionScores.getOrDefault("latency", 0.0);
        double throughput = dimensionScores.getOrDefault("throughput", 0.0);
        double resource = dimensionScores.getOrDefault("resource_utilization", 0.0);
        double scalability = dimensionScores.getOrDefault("scalability", 0.0);
        double maintainability = dimensionScores.getOrDefault("maintainability", 0.0);
        
        return RELIABILITY_WEIGHT * reliability +
               LATENCY_WEIGHT * latency +
               THROUGHPUT_WEIGHT * throughput +
               RESOURCE_WEIGHT * resource +
               SCALABILITY_WEIGHT * scalability +
               MAINTAINABILITY_WEIGHT * maintainability;
    }
    
    /**
     * Generates recommended configuration based on multi-dimensional analysis.
     */
    private SystemConfiguration generateRecommendedConfiguration(
            Map<String, Double> dimensionScores,
            Map<NodeId, NodeMetrics> currentMetrics,
            Set<ServiceId> services) {
        
        // Use existing trade-off analyzer to get base configuration
        TradeOffAnalysis baseAnalysis = tradeOffAnalyzer.analyzeTradeOffs(currentMetrics, services);
        SystemConfiguration baseConfig = baseAnalysis.getOptimalConfiguration();
        
        // Adjust configuration based on dimension priorities
        double reliabilityScore = dimensionScores.getOrDefault("reliability", 0.5);
        double scalabilityScore = dimensionScores.getOrDefault("scalability", 0.5);
        
        // If reliability is low, increase replication
        Map<ServiceId, ReplicationStrategy> adjustedReplication = baseConfig.getReplicationStrategies();
        if (reliabilityScore < 0.6) {
            adjustedReplication = increaseReplicationFactors(adjustedReplication);
        }
        
        // If scalability is low, adjust resource limits
        int adjustedTxLimit = baseConfig.getMaxConcurrentTransactions();
        if (scalabilityScore < 0.6) {
            adjustedTxLimit = (int) (adjustedTxLimit * 1.5);
        }
        
        return new SystemConfiguration(
                baseConfig.getNodeConfigurations(),
                adjustedReplication,
                baseConfig.getLoadBalancingStrategy(),
                adjustedTxLimit,
                baseConfig.getNetworkBufferSize(),
                baseConfig.getConnectionPoolSize(),
                baseConfig.getTransactionTimeout(),
                calculateOverallScore(dimensionScores)
        );
    }
    
    /**
     * Performs simulation-based validation of the recommended configuration.
     */
    private SimulationValidationResult performSimulationValidation(
            SystemConfiguration config,
            Map<NodeId, NodeMetrics> currentMetrics,
            Map<NodeId, FailureModel> failureModels) {
        
        List<String> validationMetrics = new ArrayList<>();
        double totalError = 0.0;
        int validRuns = 0;
        
        // Create optimization scenario for simulation
        Set<NodeId> allNodes = Set.of(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1);
        OptimizationScenario scenario = new OptimizationScenario(
            "multi-dimensional-validation",
            "Validation scenario for multi-dimensional trade-off analysis",
            allNodes,
            0.1, // Expected throughput improvement
            0.1, // Expected latency improvement
            Set.of()
        );
        
        // Run Monte Carlo simulations
        MonteCarloResult result = simulator.simulate(
            scenario,
            config.getNodeConfigurations(),
            currentMetrics,
            SIMULATION_RUNS
        );
        
        // Calculate validation metrics
        double avgThroughput = result.getAverageThroughput();
        double avgLatency = result.getAverageLatency();
        
        // Calculate expected values
        double expectedThroughput = currentMetrics.values().stream()
            .mapToDouble(NodeMetrics::getThroughput)
            .sum();
        double expectedLatency = currentMetrics.values().stream()
            .mapToDouble(NodeMetrics::getLatency)
            .average()
            .orElse(15.0);
        
        // Calculate error
        double throughputError = Math.abs(avgThroughput - expectedThroughput) / expectedThroughput;
        double latencyError = Math.abs(avgLatency - expectedLatency) / expectedLatency;
        double averageError = (throughputError + latencyError) / 2.0;
        
        // Determine if validation is successful
        boolean isValid = averageError < 0.2; // 20% error threshold
        double confidenceLevel = 1.0 - averageError;
        
        // Collect validation metrics
        validationMetrics.add(String.format("Average throughput: %.1f Mbps", avgThroughput));
        validationMetrics.add(String.format("Average latency: %.1f ms", avgLatency));
        validationMetrics.add(String.format("Throughput error: %.2f%%", throughputError * 100));
        validationMetrics.add(String.format("Latency error: %.2f%%", latencyError * 100));
        
        String validationSummary = String.format(
                "Simulation validation: %d runs completed, confidence: %.1f%%, average error: %.3f",
                SIMULATION_RUNS, confidenceLevel * 100, averageError);
        
        return new SimulationValidationResult(
                isValid, confidenceLevel, validationMetrics, validationSummary,
                SIMULATION_RUNS, averageError);
    }
    
    /**
     * Generates critical analysis of multi-dimensional trade-offs.
     */
    private String generateCriticalAnalysis(
            Map<String, Double> dimensionScores,
            Map<String, String> dimensionAnalysis,
            SimulationValidationResult simulationValidation) {
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("Multi-Dimensional Trade-Off Analysis:\n\n");
        
        // Identify strengths and weaknesses
        List<Map.Entry<String, Double>> sortedDimensions = dimensionScores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());
        
        analysis.append("Strengths:\n");
        for (int i = 0; i < Math.min(2, sortedDimensions.size()); i++) {
            Map.Entry<String, Double> entry = sortedDimensions.get(i);
            analysis.append(String.format("- %s (score: %.2f): %s\n",
                    formatDimensionName(entry.getKey()),
                    entry.getValue(),
                    dimensionAnalysis.get(entry.getKey())));
        }
        
        analysis.append("\nWeaknesses:\n");
        for (int i = Math.max(0, sortedDimensions.size() - 2); i < sortedDimensions.size(); i++) {
            Map.Entry<String, Double> entry = sortedDimensions.get(i);
            analysis.append(String.format("- %s (score: %.2f): %s\n",
                    formatDimensionName(entry.getKey()),
                    entry.getValue(),
                    dimensionAnalysis.get(entry.getKey())));
        }
        
        // Trade-off insights
        analysis.append("\nKey Trade-offs:\n");
        analysis.append(analyzeKeyTradeOffs(dimensionScores));
        
        // Simulation validation summary
        analysis.append("\nValidation: ");
        analysis.append(simulationValidation.getValidationSummary());
        
        return analysis.toString();
    }
    
    // Helper methods
    
    private double getFailureSeverityFactor(FailureType failureType) {
        switch (failureType) {
            case CRASH:
                return 0.95; // Moderate impact
            case OMISSION:
                return 0.90; // Higher impact
            case BYZANTINE:
                return 0.80; // Highest impact
            default:
                return 0.90;
        }
    }
    
    private String generateReliabilityAnalysis(double score, Map<NodeId, FailureModel> failureModels) {
        long byzantineCount = failureModels.values().stream()
                .filter(fm -> fm.getFailureType() == FailureType.BYZANTINE)
                .count();
        
        return String.format("System reliability %.1f%% with %d Byzantine-tolerant nodes",
                score * 100, byzantineCount);
    }
    
    private String generateLatencyAnalysis(double score, Map<NodeId, NodeMetrics> metrics) {
        double avgLatency = metrics.values().stream()
                .mapToDouble(NodeMetrics::getLatency)
                .average()
                .orElse(15.0);
        
        return String.format("Average latency %.1fms (score: %.2f)", avgLatency, score);
    }
    
    private String generateThroughputAnalysis(double score, Map<NodeId, NodeMetrics> metrics) {
        double totalThroughput = metrics.values().stream()
                .mapToDouble(NodeMetrics::getThroughput)
                .sum();
        
        return String.format("Total throughput %.1f Mbps (score: %.2f)", totalThroughput, score);
    }
    
    private String generateResourceAnalysis(double score, Map<NodeId, NodeMetrics> metrics) {
        double avgCpu = metrics.values().stream()
                .mapToDouble(NodeMetrics::getCpuUtilization)
                .average()
                .orElse(58.5);
        
        return String.format("Average CPU utilization %.1f%% (score: %.2f)", avgCpu, score);
    }
    
    private String generateScalabilityAnalysis(double score, Map<NodeId, NodeMetrics> metrics) {
        int totalTx = metrics.values().stream()
                .mapToInt(NodeMetrics::getTransactionsPerSec)
                .sum();
        
        return String.format("Transaction capacity %d tx/sec (score: %.2f)", totalTx, score);
    }
    
    private String generateMaintainabilityAnalysis(double score) {
        return String.format("System maintainability score: %.2f", score);
    }
    
    private String formatDimensionName(String dimension) {
        return dimension.replace("_", " ").substring(0, 1).toUpperCase() +
               dimension.replace("_", " ").substring(1);
    }
    
    private String analyzeKeyTradeOffs(Map<String, Double> dimensionScores) {
        StringBuilder tradeOffs = new StringBuilder();
        
        double reliability = dimensionScores.getOrDefault("reliability", 0.0);
        double throughput = dimensionScores.getOrDefault("throughput", 0.0);
        double latency = dimensionScores.getOrDefault("latency", 0.0);
        double scalability = dimensionScores.getOrDefault("scalability", 0.0);
        
        // Reliability vs Throughput
        if (reliability > 0.7 && throughput < 0.5) {
            tradeOffs.append("- High reliability achieved at cost of throughput (replication overhead)\n");
        }
        
        // Latency vs Throughput
        if (latency > 0.7 && throughput < 0.5) {
            tradeOffs.append("- Low latency prioritized over maximum throughput\n");
        }
        
        // Scalability vs Maintainability
        if (scalability > 0.7 && dimensionScores.getOrDefault("maintainability", 0.0) < 0.5) {
            tradeOffs.append("- High scalability increases system complexity\n");
        }
        
        if (tradeOffs.length() == 0) {
            tradeOffs.append("- Balanced trade-offs across all dimensions\n");
        }
        
        return tradeOffs.toString();
    }
    
    private Map<ServiceId, ReplicationStrategy> increaseReplicationFactors(
            Map<ServiceId, ReplicationStrategy> strategies) {
        Map<ServiceId, ReplicationStrategy> increased = new HashMap<>();
        
        for (Map.Entry<ServiceId, ReplicationStrategy> entry : strategies.entrySet()) {
            ReplicationStrategy original = entry.getValue();
            int newFactor = Math.min(5, original.getReplicationFactor() + 1);
            
            ReplicationStrategy increasedStrategy = new ReplicationStrategy(
                    original.getType(),
                    newFactor,
                    original.getPreferredNodes(),
                    original.getConsistencyLevel(),
                    original.isCrossLayerReplication()
            );
            
            increased.put(entry.getKey(), increasedStrategy);
        }
        
        return increased;
    }
}
