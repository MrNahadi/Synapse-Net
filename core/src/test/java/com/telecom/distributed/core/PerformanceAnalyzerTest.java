package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PerformanceAnalyzer functionality.
 */
public class PerformanceAnalyzerTest {

    private PerformanceAnalyzer analyzer;
    private Map<NodeId, NodeMetrics> testMetrics;

    @BeforeEach
    void setUp() {
        analyzer = new PerformanceAnalyzer();
        testMetrics = createTestMetrics();
    }

    @Test
    void testAnalyzeBottlenecks() {
        List<BottleneckAnalysis> results = analyzer.analyzeBottlenecks(testMetrics);
        
        // Should analyze all 5 nodes
        assertEquals(5, results.size());
        
        // Results should be sorted by severity score (highest first)
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getSeverityScore() >= results.get(i + 1).getSeverityScore());
        }
        
        // Each result should have valid data
        for (BottleneckAnalysis analysis : results) {
            assertNotNull(analysis.getBottleneckNode());
            assertNotNull(analysis.getType());
            assertTrue(analysis.getSeverityScore() >= 0.0 && analysis.getSeverityScore() <= 1.0);
            assertNotNull(analysis.getDescription());
            assertNotNull(analysis.getSuggestions());
        }
    }

    @Test
    void testIdentifyLatencyContributor() {
        LatencyContributorAnalysis result = analyzer.identifyLatencyContributor(testMetrics);
        
        assertNotNull(result.getContributorNode());
        assertTrue(result.getLatency() >= 8.0 && result.getLatency() <= 22.0);
        assertTrue(result.getConditionalFailureProbability() >= 0.0 && result.getConditionalFailureProbability() <= 1.0);
        assertNotNull(result.getJustification());
        
        // Should identify the node with highest latency
        double maxLatency = testMetrics.values().stream()
            .mapToDouble(NodeMetrics::getLatency)
            .max()
            .orElse(0.0);
        assertEquals(maxLatency, result.getLatency(), 0.001);
    }

    @Test
    void testValidateAllNodesPresent() {
        // Test with missing node
        Map<NodeId, NodeMetrics> incompleteMetrics = new HashMap<>(testMetrics);
        incompleteMetrics.remove(NodeId.EDGE1);
        
        assertThrows(IllegalArgumentException.class, () -> {
            analyzer.analyzeBottlenecks(incompleteMetrics);
        });
    }

    private Map<NodeId, NodeMetrics> createTestMetrics() {
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        
        // Edge1 - moderate performance
        metrics.put(NodeId.EDGE1, new NodeMetrics(12.0, 500.0, 1.0, 50.0, 6.0, 150, 8.0));
        
        // Edge2 - higher latency
        metrics.put(NodeId.EDGE2, new NodeMetrics(15.0, 470.0, 2.0, 55.0, 4.5, 120, 10.0));
        
        // Core1 - best performance
        metrics.put(NodeId.CORE1, new NodeMetrics(8.0, 1000.0, 0.5, 45.0, 8.0, 250, 5.0));
        
        // Core2 - moderate performance
        metrics.put(NodeId.CORE2, new NodeMetrics(10.0, 950.0, 1.5, 60.0, 12.0, 200, 12.0));
        
        // Cloud1 - highest latency but good throughput
        metrics.put(NodeId.CLOUD1, new NodeMetrics(22.0, 1250.0, 3.0, 72.0, 16.0, 300, 15.0));
        
        return metrics;
    }
}