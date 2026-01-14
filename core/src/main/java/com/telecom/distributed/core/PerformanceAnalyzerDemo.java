package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstration of PerformanceAnalyzer functionality.
 */
public class PerformanceAnalyzerDemo {
    
    public static void main(String[] args) {
        PerformanceAnalyzer analyzer = new PerformanceAnalyzer();
        Map<NodeId, NodeMetrics> testMetrics = createTestMetrics();
        
        System.out.println("=== Performance Analysis Demo ===\n");
        
        // Analyze bottlenecks
        System.out.println("1. Bottleneck Analysis:");
        List<BottleneckAnalysis> bottlenecks = analyzer.analyzeBottlenecks(testMetrics);
        
        for (int i = 0; i < bottlenecks.size(); i++) {
            BottleneckAnalysis analysis = bottlenecks.get(i);
            System.out.printf("Rank %d: %s (Score: %.3f)\n", 
                i + 1, analysis.getBottleneckNode().getId(), analysis.getSeverityScore());
            System.out.printf("  Type: %s\n", analysis.getType());
            System.out.printf("  Description: %s\n", analysis.getDescription());
            System.out.printf("  Suggestions: %d optimization suggestions\n\n", 
                analysis.getSuggestions().size());
        }
        
        // Identify latency contributor
        System.out.println("2. Latency Contributor Analysis:");
        LatencyContributorAnalysis latencyAnalysis = analyzer.identifyLatencyContributor(testMetrics);
        System.out.printf("Highest latency contributor: %s\n", 
            latencyAnalysis.getContributorNode().getId());
        System.out.printf("Latency: %.1f ms\n", latencyAnalysis.getLatency());
        System.out.printf("Conditional failure probability: %.3f\n", 
            latencyAnalysis.getConditionalFailureProbability());
        System.out.printf("Justification: %s\n", latencyAnalysis.getJustification());
        
        System.out.println("\n=== Demo completed successfully ===");
    }
    
    private static Map<NodeId, NodeMetrics> createTestMetrics() {
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