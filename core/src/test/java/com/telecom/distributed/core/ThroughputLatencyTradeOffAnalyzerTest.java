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
import static org.mockito.Mockito.*;

/**
 * Unit tests for ThroughputLatencyTradeOffAnalyzer.
 * Tests Pareto optimization and parameter recommendation algorithms.
 * Requirements: 10.3, 10.4
 */
class ThroughputLatencyTradeOffAnalyzerTest {

    @Mock
    private ReplicationManager replicationManager;
    
    @Mock
    private PerformanceAnalyzer performanceAnalyzer;
    
    private ThroughputLatencyTradeOffAnalyzer analyzer;
    private Map<NodeId, NodeMetrics> testMetrics;
    private Set<ServiceId> testServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        analyzer = new ThroughputLatencyTradeOffAnalyzer(replicationManager, performanceAnalyzer);
        
        // Set up test data
        testMetrics = createTestMetrics();
        testServices = createTestServices();
    }

    @Test
    @DisplayName("Should perform quantitative trade-off analysis with valid inputs")
    void testAnalyzeTradeOffs_ValidInputs() {
        // Given
        when(performanceAnalyzer.analyzeBottlenecks(any())).thenReturn(createMockBottleneckAnalyses());
        
        // When
        TradeOffAnalysis result = analyzer.analyzeTradeOffs(testMetrics, testServices);
        
        // Then
        assertNotNull(result, "Trade-off analysis result should not be null");
        assertNotNull(result.getParetoFrontier(), "Pareto frontier should not be null");
        assertFalse(result.getParetoFrontier().isEmpty(), "Pareto frontier should not be empty");
        assertNotNull(result.getOptimalConfiguration(), "Optimal configuration should not be null");
        assertTrue(result.getTradeOffRatio() > 0, "Trade-off ratio should be positive");
        assertTrue(result.getConfidenceScore() >= 0 && result.getConfidenceScore() <= 1, 
                  "Confidence score should be between 0 and 1");
        assertNotNull(result.getAnalysisDescription(), "Analysis description should not be null");
        assertFalse(result.getAnalysisDescription().isEmpty(), "Analysis description should not be empty");
    }

    @Test
    @DisplayName("Should generate Pareto frontier with non-dominated points")
    void testParetoOptimization_NonDominatedPoints() {
        // Given
        when(performanceAnalyzer.analyzeBottlenecks(any())).thenReturn(createMockBottleneckAnalyses());
        
        // When
        TradeOffAnalysis result = analyzer.analyzeTradeOffs(testMetrics, testServices);
        List<ParetoPoint> paretoFrontier = result.getParetoFrontier();
        
        // Then
        assertTrue(paretoFrontier.size() > 0, "Pareto frontier should contain points");
        
        // Verify no point dominates another in the Pareto frontier
        for (int i = 0; i < paretoFrontier.size(); i++) {
            for (int j = 0; j < paretoFrontier.size(); j++) {
                if (i != j) {
                    assertFalse(paretoFrontier.get(i).dominates(paretoFrontier.get(j)),
                              "No point in Pareto frontier should dominate another");
                }
            }
        }
        
        // Verify all points are marked as optimal
        for (ParetoPoint point : paretoFrontier) {
            assertTrue(point.isOptimal(), "All Pareto frontier points should be marked as optimal");
        }
    }

    @Test
    @DisplayName("Should recommend optimal configuration parameters")
    void testParameterRecommendation_OptimalConfiguration() {
        // Given
        when(performanceAnalyzer.analyzeBottlenecks(any())).thenReturn(createMockBottleneckAnalyses());
        
        // When
        TradeOffAnalysis result = analyzer.analyzeTradeOffs(testMetrics, testServices);
        SystemConfiguration optimalConfig = result.getOptimalConfiguration();
        
        // Then
        assertNotNull(optimalConfig, "Optimal configuration should not be null");
        assertNotNull(optimalConfig.getNodeConfigurations(), "Node configurations should not be null");
        assertNotNull(optimalConfig.getReplicationStrategies(), "Replication strategies should not be null");
        assertNotNull(optimalConfig.getLoadBalancingStrategy(), "Load balancing strategy should not be null");
        
        // Verify configuration parameters are within reasonable bounds
        assertTrue(optimalConfig.getMaxConcurrentTransactions() > 0, 
                  "Max concurrent transactions should be positive");
        assertTrue(optimalConfig.getNetworkBufferSize() > 0, 
                  "Network buffer size should be positive");
        assertTrue(optimalConfig.getConnectionPoolSize() > 0, 
                  "Connection pool size should be positive");
        assertTrue(optimalConfig.getTransactionTimeout() > 0, 
                  "Transaction timeout should be positive");
        assertTrue(optimalConfig.getConfigurationScore() >= 0, 
                  "Configuration score should be non-negative");
        
        // Verify all required nodes are configured
        Set<NodeId> requiredNodes = Set.of(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1);
        assertTrue(optimalConfig.getNodeConfigurations().keySet().containsAll(requiredNodes),
                  "All required nodes should be configured");
    }

    @Test
    @DisplayName("Should integrate with replication strategies from task 11")
    void testReplicationStrategyIntegration() {
        // Given
        when(performanceAnalyzer.analyzeBottlenecks(any())).thenReturn(createMockBottleneckAnalyses());
        
        // When
        TradeOffAnalysis result = analyzer.analyzeTradeOffs(testMetrics, testServices);
        SystemConfiguration optimalConfig = result.getOptimalConfiguration();
        
        // Then
        Map<ServiceId, ReplicationStrategy> replicationStrategies = optimalConfig.getReplicationStrategies();
        assertNotNull(replicationStrategies, "Replication strategies should not be null");
        assertFalse(replicationStrategies.isEmpty(), "Replication strategies should not be empty");
        
        // Verify each service has a replication strategy
        for (ServiceId serviceId : testServices) {
            assertTrue(replicationStrategies.containsKey(serviceId),
                      "Each service should have a replication strategy");
            
            ReplicationStrategy strategy = replicationStrategies.get(serviceId);
            assertNotNull(strategy, "Replication strategy should not be null");
            assertTrue(strategy.getReplicationFactor() >= 1, 
                      "Replication factor should be at least 1");
            assertNotNull(strategy.getConsistencyLevel(), 
                         "Consistency level should not be null");
        }
    }

    @Test
    @DisplayName("Should calculate meaningful trade-off ratios")
    void testTradeOffRatioCalculation() {
        // Given
        when(performanceAnalyzer.analyzeBottlenecks(any())).thenReturn(createMockBottleneckAnalyses());
        
        // When
        TradeOffAnalysis result = analyzer.analyzeTradeOffs(testMetrics, testServices);
        double tradeOffRatio = result.getTradeOffRatio();
        
        // Then
        assertTrue(tradeOffRatio > 0, "Trade-off ratio should be positive");
        assertTrue(tradeOffRatio < 1000, "Trade-off ratio should be reasonable (< 1000)");
        
        // Verify the ratio represents meaningful trade-off information
        List<ParetoPoint> paretoFrontier = result.getParetoFrontier();
        if (paretoFrontier.size() > 1) {
            // Should reflect the slope of the Pareto frontier
            assertTrue(tradeOffRatio > 0.01, "Trade-off ratio should be meaningful for multiple points");
        }
    }

    @Test
    @DisplayName("Should handle edge case with single service")
    void testSingleServiceScenario() {
        // Given
        Set<ServiceId> singleService = Set.of(new ServiceId("test-service-1"));
        when(performanceAnalyzer.analyzeBottlenecks(any())).thenReturn(createMockBottleneckAnalyses());
        
        // When
        TradeOffAnalysis result = analyzer.analyzeTradeOffs(testMetrics, singleService);
        
        // Then
        assertNotNull(result, "Result should not be null for single service");
        assertNotNull(result.getParetoFrontier(), "Pareto frontier should not be null");
        assertNotNull(result.getOptimalConfiguration(), "Optimal configuration should not be null");
        
        // Verify single service is configured
        assertTrue(result.getOptimalConfiguration().getReplicationStrategies().containsKey(
            new ServiceId("test-service-1")), "Single service should be configured");
    }

    @Test
    @DisplayName("Should validate input parameters")
    void testInputValidation() {
        // Test null metrics
        assertThrows(NullPointerException.class, () -> {
            analyzer.analyzeTradeOffs(null, testServices);
        }, "Should throw exception for null metrics");
        
        // Test null services
        assertThrows(NullPointerException.class, () -> {
            analyzer.analyzeTradeOffs(testMetrics, null);
        }, "Should throw exception for null services");
        
        // Test empty services
        Set<ServiceId> emptyServices = new HashSet<>();
        when(performanceAnalyzer.analyzeBottlenecks(any())).thenReturn(createMockBottleneckAnalyses());
        
        TradeOffAnalysis result = analyzer.analyzeTradeOffs(testMetrics, emptyServices);
        assertNotNull(result, "Should handle empty services gracefully");
    }

    @Test
    @DisplayName("Should produce consistent results for same inputs")
    void testResultConsistency() {
        // Given
        when(performanceAnalyzer.analyzeBottlenecks(any())).thenReturn(createMockBottleneckAnalyses());
        
        // When
        TradeOffAnalysis result1 = analyzer.analyzeTradeOffs(testMetrics, testServices);
        TradeOffAnalysis result2 = analyzer.analyzeTradeOffs(testMetrics, testServices);
        
        // Then
        assertEquals(result1.getParetoFrontier().size(), result2.getParetoFrontier().size(),
                    "Pareto frontier size should be consistent");
        assertEquals(result1.getTradeOffRatio(), result2.getTradeOffRatio(), 0.001,
                    "Trade-off ratio should be consistent");
        
        // Verify optimal configurations have same key parameters
        SystemConfiguration config1 = result1.getOptimalConfiguration();
        SystemConfiguration config2 = result2.getOptimalConfiguration();
        assertEquals(config1.getMaxConcurrentTransactions(), config2.getMaxConcurrentTransactions(),
                    "Max concurrent transactions should be consistent");
        assertEquals(config1.getConnectionPoolSize(), config2.getConnectionPoolSize(),
                    "Connection pool size should be consistent");
    }

    @Test
    @DisplayName("Should generate comprehensive analysis descriptions")
    void testAnalysisDescriptionGeneration() {
        // Given
        when(performanceAnalyzer.analyzeBottlenecks(any())).thenReturn(createMockBottleneckAnalyses());
        
        // When
        TradeOffAnalysis result = analyzer.analyzeTradeOffs(testMetrics, testServices);
        String description = result.getAnalysisDescription();
        
        // Then
        assertNotNull(description, "Analysis description should not be null");
        assertFalse(description.isEmpty(), "Analysis description should not be empty");
        
        // Verify description contains key information
        assertTrue(description.contains("Pareto"), "Description should mention Pareto analysis");
        assertTrue(description.contains("throughput") || description.contains("Mbps"), 
                  "Description should mention throughput");
        assertTrue(description.contains("latency") || description.contains("ms"), 
                  "Description should mention latency");
        assertTrue(description.contains("configuration"), 
                  "Description should mention configuration");
    }

    // Helper methods for test data creation

    private Map<NodeId, NodeMetrics> createTestMetrics() {
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        metrics.put(NodeId.EDGE1, new NodeMetrics(12.0, 500.0, 0.5, 45.0, 4.0, 150, 5.0));
        metrics.put(NodeId.EDGE2, new NodeMetrics(15.0, 470.0, 0.8, 50.0, 4.5, 120, 7.0));
        metrics.put(NodeId.CORE1, new NodeMetrics(8.0, 1000.0, 0.3, 60.0, 8.0, 250, 10.0));
        metrics.put(NodeId.CORE2, new NodeMetrics(10.0, 950.0, 0.4, 55.0, 6.0, 200, 8.0));
        metrics.put(NodeId.CLOUD1, new NodeMetrics(22.0, 1250.0, 0.2, 72.0, 16.0, 300, 15.0));
        return metrics;
    }

    private Set<ServiceId> createTestServices() {
        return Set.of(
            new ServiceId("test-service-1"),
            new ServiceId("test-service-2"),
            new ServiceId("test-service-3")
        );
    }

    private List<BottleneckAnalysis> createMockBottleneckAnalyses() {
        return Arrays.asList(
            new BottleneckAnalysis(NodeId.EDGE1, BottleneckType.CPU, 0.3, 
                                 "Edge1 CPU bottleneck", Set.of()),
            new BottleneckAnalysis(NodeId.CORE1, BottleneckType.NETWORK_LATENCY, 0.2, 
                                 "Core1 network bottleneck", Set.of()),
            new BottleneckAnalysis(NodeId.CLOUD1, BottleneckType.MEMORY, 0.4, 
                                 "Cloud1 memory bottleneck", Set.of())
        );
    }
}