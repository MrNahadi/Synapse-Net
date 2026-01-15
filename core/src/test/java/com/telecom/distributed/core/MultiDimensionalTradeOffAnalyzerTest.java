package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MultiDimensionalTradeOffAnalyzer.
 * Tests trade-off calculations and simulation validation.
 * Requirements: 20.3, 20.4, 20.5
 */
class MultiDimensionalTradeOffAnalyzerTest {

    @Mock
    private SystemicFailureRiskAssessor riskAssessor;
    
    @Mock
    private PerformanceAnalyzer performanceAnalyzer;
    
    @Mock
    private ThroughputLatencyTradeOffAnalyzer tradeOffAnalyzer;
    
    @Mock
    private MonteCarloSimulator simulator;
    
    private MultiDimensionalTradeOffAnalyzer analyzer;
    private Map<NodeId, NodeMetrics> testMetrics;
    private Set<ServiceId> testServices;
    private Map<NodeId, FailureModel> testFailureModels;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        analyzer = new MultiDimensionalTradeOffAnalyzer(
            riskAssessor, performanceAnalyzer, tradeOffAnalyzer, simulator);
        
        // Set up test data
        testMetrics = createTestMetrics();
        testServices = createTestServices();
        testFailureModels = createTestFailureModels();
    }

    @Test
    @DisplayName("Should analyze all six dimensions comprehensively")
    void testMultiDimensionalAnalysis_AllDimensions() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        assertNotNull(result, "Analysis result should not be null");
        
        // Verify all six dimensions are analyzed
        Map<String, Double> dimensionScores = result.getDimensionScores();
        assertTrue(dimensionScores.containsKey("reliability"), "Should analyze reliability");
        assertTrue(dimensionScores.containsKey("latency"), "Should analyze latency");
        assertTrue(dimensionScores.containsKey("throughput"), "Should analyze throughput");
        assertTrue(dimensionScores.containsKey("resource_utilization"), "Should analyze resource utilization");
        assertTrue(dimensionScores.containsKey("scalability"), "Should analyze scalability");
        assertTrue(dimensionScores.containsKey("maintainability"), "Should analyze maintainability");
        
        // Verify all scores are in valid range [0, 1]
        for (Map.Entry<String, Double> entry : dimensionScores.entrySet()) {
            double score = entry.getValue();
            assertTrue(score >= 0.0 && score <= 1.0, 
                String.format("Score for %s should be between 0 and 1, got: %.2f", entry.getKey(), score));
        }
    }

    @Test
    @DisplayName("Should use probabilistic models for analysis")
    void testProbabilisticModelUsage() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        // Verify reliability analysis uses failure probabilities
        double reliabilityScore = result.getReliabilityScore();
        assertTrue(reliabilityScore > 0.0, "Reliability score should be positive");
        assertTrue(reliabilityScore < 1.0, "Reliability score should account for failure probabilities");
        
        // Verify analysis considers probabilistic failure models
        Map<String, String> dimensionAnalysis = result.getDimensionAnalysis();
        String reliabilityAnalysis = dimensionAnalysis.get("reliability");
        assertNotNull(reliabilityAnalysis, "Reliability analysis should exist");
        assertTrue(reliabilityAnalysis.contains("reliability") || reliabilityAnalysis.contains("%"),
            "Reliability analysis should mention probabilistic metrics");
    }

    @Test
    @DisplayName("Should perform simulation-based validation")
    void testSimulationBasedValidation() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        SimulationValidationResult validation = result.getSimulationValidation();
        assertNotNull(validation, "Simulation validation should not be null");
        
        // Verify simulation was performed
        assertTrue(validation.getSimulationRuns() > 0, "Should perform simulation runs");
        assertTrue(validation.getConfidenceLevel() >= 0.0 && validation.getConfidenceLevel() <= 1.0,
            "Confidence level should be between 0 and 1");
        assertTrue(validation.getAverageError() >= 0.0, "Average error should be non-negative");
        
        assertNotNull(validation.getValidationMetrics(), "Validation metrics should not be null");
        assertNotNull(validation.getValidationSummary(), "Validation summary should not be null");
        assertFalse(validation.getValidationSummary().isEmpty(), "Validation summary should not be empty");
        
        // Verify simulator was called
        verify(simulator, atLeastOnce()).simulate(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Should provide critical analysis of trade-offs")
    void testCriticalAnalysisGeneration() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        String criticalAnalysis = result.getCriticalAnalysis();
        assertNotNull(criticalAnalysis, "Critical analysis should not be null");
        assertFalse(criticalAnalysis.isEmpty(), "Critical analysis should not be empty");
        
        // Verify critical analysis contains key sections
        assertTrue(criticalAnalysis.contains("Strengths") || criticalAnalysis.contains("strengths"),
            "Critical analysis should identify strengths");
        assertTrue(criticalAnalysis.contains("Weaknesses") || criticalAnalysis.contains("weaknesses"),
            "Critical analysis should identify weaknesses");
        assertTrue(criticalAnalysis.contains("Trade-off") || criticalAnalysis.contains("trade-off"),
            "Critical analysis should discuss trade-offs");
        assertTrue(criticalAnalysis.contains("Validation") || criticalAnalysis.contains("validation"),
            "Critical analysis should include validation results");
    }

    @Test
    @DisplayName("Should calculate overall weighted score correctly")
    void testOverallScoreCalculation() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        double overallScore = result.getOverallScore();
        assertTrue(overallScore >= 0.0 && overallScore <= 1.0, 
            "Overall score should be between 0 and 1");
        
        // Verify overall score is weighted combination of dimension scores
        Map<String, Double> dimensionScores = result.getDimensionScores();
        double calculatedScore = 
            0.20 * dimensionScores.get("reliability") +
            0.15 * dimensionScores.get("latency") +
            0.20 * dimensionScores.get("throughput") +
            0.15 * dimensionScores.get("resource_utilization") +
            0.15 * dimensionScores.get("scalability") +
            0.15 * dimensionScores.get("maintainability");
        
        assertEquals(calculatedScore, overallScore, 0.001, 
            "Overall score should match weighted combination");
    }

    @Test
    @DisplayName("Should generate recommended configuration based on trade-offs")
    void testRecommendedConfigurationGeneration() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        SystemConfiguration recommendedConfig = result.getRecommendedConfiguration();
        assertNotNull(recommendedConfig, "Recommended configuration should not be null");
        
        // Verify configuration is complete
        assertNotNull(recommendedConfig.getNodeConfigurations(), "Node configurations should not be null");
        assertNotNull(recommendedConfig.getReplicationStrategies(), "Replication strategies should not be null");
        assertNotNull(recommendedConfig.getLoadBalancingStrategy(), "Load balancing strategy should not be null");
        
        // Verify configuration parameters are reasonable
        assertTrue(recommendedConfig.getMaxConcurrentTransactions() > 0, 
            "Max concurrent transactions should be positive");
        assertTrue(recommendedConfig.getNetworkBufferSize() > 0, 
            "Network buffer size should be positive");
        assertTrue(recommendedConfig.getConnectionPoolSize() > 0, 
            "Connection pool size should be positive");
        assertTrue(recommendedConfig.getTransactionTimeout() > 0, 
            "Transaction timeout should be positive");
    }

    @Test
    @DisplayName("Should analyze reliability using failure models")
    void testReliabilityAnalysis() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        double reliabilityScore = result.getReliabilityScore();
        assertTrue(reliabilityScore > 0.0, "Reliability score should be positive");
        
        // Verify reliability considers different failure types
        String reliabilityAnalysis = result.getDimensionAnalysis().get("reliability");
        assertNotNull(reliabilityAnalysis, "Reliability analysis should exist");
        
        // Byzantine failures should impact reliability more than crash failures
        assertTrue(reliabilityScore < 1.0, "Reliability should account for failure probabilities");
    }

    @Test
    @DisplayName("Should analyze latency performance")
    void testLatencyAnalysis() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        double latencyScore = result.getLatencyScore();
        assertTrue(latencyScore >= 0.0 && latencyScore <= 1.0, 
            "Latency score should be between 0 and 1");
        
        // Verify latency analysis exists
        String latencyAnalysis = result.getDimensionAnalysis().get("latency");
        assertNotNull(latencyAnalysis, "Latency analysis should exist");
        assertTrue(latencyAnalysis.contains("latency") || latencyAnalysis.contains("ms"),
            "Latency analysis should mention latency metrics");
    }

    @Test
    @DisplayName("Should analyze throughput capacity")
    void testThroughputAnalysis() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        double throughputScore = result.getThroughputScore();
        assertTrue(throughputScore >= 0.0 && throughputScore <= 1.0, 
            "Throughput score should be between 0 and 1");
        
        // Verify throughput analysis exists
        String throughputAnalysis = result.getDimensionAnalysis().get("throughput");
        assertNotNull(throughputAnalysis, "Throughput analysis should exist");
        assertTrue(throughputAnalysis.contains("throughput") || throughputAnalysis.contains("Mbps"),
            "Throughput analysis should mention throughput metrics");
    }

    @Test
    @DisplayName("Should analyze resource utilization efficiency")
    void testResourceUtilizationAnalysis() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        double resourceScore = result.getResourceUtilizationScore();
        assertTrue(resourceScore >= 0.0 && resourceScore <= 1.0, 
            "Resource utilization score should be between 0 and 1");
        
        // Verify resource analysis considers CPU and memory
        String resourceAnalysis = result.getDimensionAnalysis().get("resource_utilization");
        assertNotNull(resourceAnalysis, "Resource utilization analysis should exist");
    }

    @Test
    @DisplayName("Should analyze scalability potential")
    void testScalabilityAnalysis() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        double scalabilityScore = result.getScalabilityScore();
        assertTrue(scalabilityScore >= 0.0 && scalabilityScore <= 1.0, 
            "Scalability score should be between 0 and 1");
        
        // Verify scalability analysis exists
        String scalabilityAnalysis = result.getDimensionAnalysis().get("scalability");
        assertNotNull(scalabilityAnalysis, "Scalability analysis should exist");
    }

    @Test
    @DisplayName("Should analyze maintainability")
    void testMaintainabilityAnalysis() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        double maintainabilityScore = result.getMaintainabilityScore();
        assertTrue(maintainabilityScore >= 0.0 && maintainabilityScore <= 1.0, 
            "Maintainability score should be between 0 and 1");
        
        // Verify maintainability analysis exists
        String maintainabilityAnalysis = result.getDimensionAnalysis().get("maintainability");
        assertNotNull(maintainabilityAnalysis, "Maintainability analysis should exist");
    }

    @Test
    @DisplayName("Should validate input parameters")
    void testInputValidation() {
        // Test null metrics
        assertThrows(NullPointerException.class, () -> {
            analyzer.analyzeMultiDimensionalTradeOffs(null, testServices, testFailureModels);
        }, "Should throw exception for null metrics");
        
        // Test null services
        assertThrows(NullPointerException.class, () -> {
            analyzer.analyzeMultiDimensionalTradeOffs(testMetrics, null, testFailureModels);
        }, "Should throw exception for null services");
        
        // Test null failure models
        assertThrows(NullPointerException.class, () -> {
            analyzer.analyzeMultiDimensionalTradeOffs(testMetrics, testServices, null);
        }, "Should throw exception for null failure models");
    }

    @Test
    @DisplayName("Should handle varying system configurations")
    void testVaryingConfigurations() {
        // Given
        setupMockBehaviors();
        
        // Test with high-performance configuration
        Map<NodeId, NodeMetrics> highPerfMetrics = createHighPerformanceMetrics();
        MultiDimensionalTradeOffAnalysis highPerfResult = analyzer.analyzeMultiDimensionalTradeOffs(
            highPerfMetrics, testServices, testFailureModels);
        
        // Test with low-performance configuration
        Map<NodeId, NodeMetrics> lowPerfMetrics = createLowPerformanceMetrics();
        MultiDimensionalTradeOffAnalysis lowPerfResult = analyzer.analyzeMultiDimensionalTradeOffs(
            lowPerfMetrics, testServices, testFailureModels);
        
        // Then
        // High performance should have better throughput and latency scores
        assertTrue(highPerfResult.getThroughputScore() > lowPerfResult.getThroughputScore(),
            "High performance config should have better throughput score");
        assertTrue(highPerfResult.getLatencyScore() > lowPerfResult.getLatencyScore(),
            "High performance config should have better latency score");
    }

    @Test
    @DisplayName("Should produce consistent results for same inputs")
    void testResultConsistency() {
        // Given
        setupMockBehaviors();
        
        // When
        MultiDimensionalTradeOffAnalysis result1 = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        MultiDimensionalTradeOffAnalysis result2 = analyzer.analyzeMultiDimensionalTradeOffs(
            testMetrics, testServices, testFailureModels);
        
        // Then
        assertEquals(result1.getOverallScore(), result2.getOverallScore(), 0.001,
            "Overall score should be consistent");
        assertEquals(result1.getReliabilityScore(), result2.getReliabilityScore(), 0.001,
            "Reliability score should be consistent");
        assertEquals(result1.getLatencyScore(), result2.getLatencyScore(), 0.001,
            "Latency score should be consistent");
        assertEquals(result1.getThroughputScore(), result2.getThroughputScore(), 0.001,
            "Throughput score should be consistent");
    }

    // Helper methods for test data creation

    private void setupMockBehaviors() {
        // Mock trade-off analyzer
        TradeOffAnalysis mockTradeOffAnalysis = createMockTradeOffAnalysis();
        when(tradeOffAnalyzer.analyzeTradeOffs(any(), any())).thenReturn(mockTradeOffAnalysis);
        
        // Mock simulator
        MonteCarloResult mockSimResult = createMockSimulationResult();
        when(simulator.simulate(any(), any(), any(), anyInt())).thenReturn(mockSimResult);
    }

    private Map<NodeId, NodeMetrics> createTestMetrics() {
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        metrics.put(NodeId.EDGE1, new NodeMetrics(12.0, 500.0, 0.5, 45.0, 4.0, 150, 5.0));
        metrics.put(NodeId.EDGE2, new NodeMetrics(15.0, 470.0, 0.8, 50.0, 4.5, 120, 7.0));
        metrics.put(NodeId.CORE1, new NodeMetrics(8.0, 1000.0, 0.3, 60.0, 8.0, 250, 10.0));
        metrics.put(NodeId.CORE2, new NodeMetrics(10.0, 950.0, 0.4, 55.0, 6.0, 200, 8.0));
        metrics.put(NodeId.CLOUD1, new NodeMetrics(22.0, 1250.0, 0.2, 72.0, 16.0, 300, 15.0));
        return metrics;
    }

    private Map<NodeId, NodeMetrics> createHighPerformanceMetrics() {
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        metrics.put(NodeId.EDGE1, new NodeMetrics(8.0, 500.0, 0.2, 45.0, 4.0, 200, 5.0));
        metrics.put(NodeId.EDGE2, new NodeMetrics(9.0, 490.0, 0.3, 46.0, 4.2, 180, 6.0));
        metrics.put(NodeId.CORE1, new NodeMetrics(8.0, 1200.0, 0.1, 50.0, 6.0, 300, 8.0));
        metrics.put(NodeId.CORE2, new NodeMetrics(8.5, 1100.0, 0.2, 52.0, 5.5, 280, 7.0));
        metrics.put(NodeId.CLOUD1, new NodeMetrics(10.0, 1250.0, 0.1, 55.0, 12.0, 300, 10.0));
        return metrics;
    }

    private Map<NodeId, NodeMetrics> createLowPerformanceMetrics() {
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        metrics.put(NodeId.EDGE1, new NodeMetrics(20.0, 470.0, 2.0, 70.0, 4.0, 100, 12.0));
        metrics.put(NodeId.EDGE2, new NodeMetrics(22.0, 470.0, 2.5, 72.0, 4.5, 100, 13.0));
        metrics.put(NodeId.CORE1, new NodeMetrics(18.0, 800.0, 1.5, 68.0, 8.0, 150, 14.0));
        metrics.put(NodeId.CORE2, new NodeMetrics(19.0, 750.0, 1.8, 70.0, 7.0, 140, 13.0));
        metrics.put(NodeId.CLOUD1, new NodeMetrics(22.0, 900.0, 1.0, 72.0, 16.0, 200, 15.0));
        return metrics;
    }

    private Set<ServiceId> createTestServices() {
        return Set.of(
            new ServiceId("test-service-1"),
            new ServiceId("test-service-2"),
            new ServiceId("test-service-3")
        );
    }

    private Map<NodeId, FailureModel> createTestFailureModels() {
        Map<NodeId, FailureModel> models = new HashMap<>();
        models.put(NodeId.EDGE1, new FailureModel(FailureType.CRASH, 0.01, 5000L, 10000L));
        models.put(NodeId.EDGE2, new FailureModel(FailureType.OMISSION, 0.02, 3000L, 8000L));
        models.put(NodeId.CORE1, new FailureModel(FailureType.BYZANTINE, 0.005, 10000L, 15000L));
        models.put(NodeId.CORE2, new FailureModel(FailureType.CRASH, 0.01, 5000L, 10000L));
        models.put(NodeId.CLOUD1, new FailureModel(FailureType.OMISSION, 0.015, 4000L, 9000L));
        return models;
    }

    private TradeOffAnalysis createMockTradeOffAnalysis() {
        List<ParetoPoint> paretoFrontier = new ArrayList<>();
        SystemConfiguration mockConfig = createMockSystemConfiguration();
        
        paretoFrontier.add(new ParetoPoint(3000.0, 12.0, mockConfig, 0.8, true));
        paretoFrontier.add(new ParetoPoint(2500.0, 10.0, mockConfig, 0.75, true));
        
        return new TradeOffAnalysis(paretoFrontier, mockConfig, 1.5, 
            "Mock trade-off analysis", 0.85);
    }

    private SystemConfiguration createMockSystemConfiguration() {
        Map<NodeId, NodeConfiguration> nodeConfigs = new HashMap<>();
        Map<ServiceId, ReplicationStrategy> replicationStrategies = new HashMap<>();
        
        for (ServiceId serviceId : testServices) {
            replicationStrategies.put(serviceId, 
                new ReplicationStrategy(ReplicationStrategy.ReplicationType.ACTIVE, 2, Set.of(),
                    ReplicationStrategy.ConsistencyLevel.STRONG, true));
        }
        
        return new SystemConfiguration(nodeConfigs, replicationStrategies, 
            LoadBalancingStrategy.RESOURCE_AWARE, 100, 2048.0, 20, 10000L, 0.75);
    }

    private MonteCarloResult createMockSimulationResult() {
        ConfidenceInterval throughputCI = new ConfidenceInterval(2700.0, 2900.0);
        ConfidenceInterval latencyCI = new ConfidenceInterval(11.0, 12.0);
        return new MonteCarloResult(2800.0, 11.5, throughputCI, latencyCI);
    }
}
