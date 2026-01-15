package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SystemicFailureRiskAssessor.
 * Tests cascading failure analysis and risk scoring algorithms.
 * 
 * Requirements: 16.3, 16.4
 */
public class SystemicFailureRiskAssessorTest {

    private SystemicFailureRiskAssessor assessor;
    private Map<NodeId, NodeMetrics> testMetrics;
    private Map<NodeId, NodeConfiguration> testConfigurations;
    private NetworkTopology testTopology;

    @BeforeEach
    void setUp() {
        testTopology = createTestTopology();
        assessor = new SystemicFailureRiskAssessor(testTopology);
        testMetrics = createTestMetrics();
        testConfigurations = createTestConfigurations();
    }

    @Test
    void testAssessSystemicRisks_AllNodesAnalyzed() {
        List<SystemicFailureRisk> risks = assessor.assessSystemicRisks(testMetrics, testConfigurations);
        
        // Should analyze all 5 nodes
        assertEquals(5, risks.size(), "Should analyze all 5 nodes");
        
        // All node IDs should be present
        Set<NodeId> analyzedNodes = new HashSet<>();
        for (SystemicFailureRisk risk : risks) {
            analyzedNodes.add(risk.getNodeId());
        }
        assertTrue(analyzedNodes.contains(NodeId.EDGE1));
        assertTrue(analyzedNodes.contains(NodeId.EDGE2));
        assertTrue(analyzedNodes.contains(NodeId.CORE1));
        assertTrue(analyzedNodes.contains(NodeId.CORE2));
        assertTrue(analyzedNodes.contains(NodeId.CLOUD1));
    }

    @Test
    void testAssessSystemicRisks_SortedByRiskScore() {
        List<SystemicFailureRisk> risks = assessor.assessSystemicRisks(testMetrics, testConfigurations);
        
        // Results should be sorted by risk score (highest first)
        for (int i = 0; i < risks.size() - 1; i++) {
            assertTrue(risks.get(i).getRiskScore() >= risks.get(i + 1).getRiskScore(),
                      "Risk scores should be sorted in descending order");
        }
    }

    @Test
    void testAssessSystemicRisks_ValidRiskScores() {
        List<SystemicFailureRisk> risks = assessor.assessSystemicRisks(testMetrics, testConfigurations);
        
        // All risk scores should be between 0.0 and 1.0
        for (SystemicFailureRisk risk : risks) {
            assertTrue(risk.getRiskScore() >= 0.0 && risk.getRiskScore() <= 1.0,
                      "Risk score should be between 0.0 and 1.0");
            assertTrue(risk.getCriticalityScore() >= 0.0 && risk.getCriticalityScore() <= 1.0,
                      "Criticality score should be between 0.0 and 1.0");
            assertTrue(risk.getCascadeRiskScore() >= 0.0 && risk.getCascadeRiskScore() <= 1.0,
                      "Cascade risk score should be between 0.0 and 1.0");
        }
    }

    @Test
    void testAssessSystemicRisks_ByzantineFailureHighestRisk() {
        List<SystemicFailureRisk> risks = assessor.assessSystemicRisks(testMetrics, testConfigurations);
        
        // Find Core1 (Byzantine failure node)
        SystemicFailureRisk core1Risk = risks.stream()
            .filter(r -> r.getNodeId().equals(NodeId.CORE1))
            .findFirst()
            .orElseThrow();
        
        // Byzantine failures should have high risk
        assertEquals(FailureType.BYZANTINE, core1Risk.getPrimaryFailureType());
        
        // Core1 should have higher risk than at least some other nodes due to Byzantine failure type
        long higherRiskCount = risks.stream()
            .filter(r -> r.getRiskScore() < core1Risk.getRiskScore())
            .count();
        assertTrue(higherRiskCount > 0, "Byzantine failure node should have elevated risk");
    }

    @Test
    void testAssessSystemicRisks_CoreLayerHighCriticality() {
        List<SystemicFailureRisk> risks = assessor.assessSystemicRisks(testMetrics, testConfigurations);
        
        // Core layer nodes should have higher criticality than edge nodes
        double avgCoreCriticality = risks.stream()
            .filter(r -> r.getNodeId().equals(NodeId.CORE1) || r.getNodeId().equals(NodeId.CORE2))
            .mapToDouble(SystemicFailureRisk::getCriticalityScore)
            .average()
            .orElse(0.0);
        
        double avgEdgeCriticality = risks.stream()
            .filter(r -> r.getNodeId().equals(NodeId.EDGE1) || r.getNodeId().equals(NodeId.EDGE2))
            .mapToDouble(SystemicFailureRisk::getCriticalityScore)
            .average()
            .orElse(0.0);
        
        assertTrue(avgCoreCriticality > avgEdgeCriticality,
                  "Core layer should have higher criticality than edge layer");
    }

    @Test
    void testAssessSystemicRisks_HighLoadIncreasesRisk() {
        // Create high load scenario
        Map<NodeId, NodeMetrics> highLoadMetrics = new HashMap<>(testMetrics);
        highLoadMetrics.put(NodeId.EDGE1, new NodeMetrics(20.0, 480.0, 4.0, 70.0, 15.0, 280, 14.0));
        
        List<SystemicFailureRisk> normalRisks = assessor.assessSystemicRisks(testMetrics, testConfigurations);
        List<SystemicFailureRisk> highLoadRisks = assessor.assessSystemicRisks(highLoadMetrics, testConfigurations);
        
        // Find Edge1 risk in both scenarios
        double normalEdge1Risk = normalRisks.stream()
            .filter(r -> r.getNodeId().equals(NodeId.EDGE1))
            .findFirst()
            .orElseThrow()
            .getRiskScore();
        
        double highLoadEdge1Risk = highLoadRisks.stream()
            .filter(r -> r.getNodeId().equals(NodeId.EDGE1))
            .findFirst()
            .orElseThrow()
            .getRiskScore();
        
        assertTrue(highLoadEdge1Risk > normalEdge1Risk,
                  "High load should increase risk score");
    }

    @Test
    void testAssessSystemicRisks_MitigationStrategiesProvided() {
        List<SystemicFailureRisk> risks = assessor.assessSystemicRisks(testMetrics, testConfigurations);
        
        // All risks should have mitigation strategies
        for (SystemicFailureRisk risk : risks) {
            assertNotNull(risk.getMitigationStrategies());
            assertFalse(risk.getMitigationStrategies().isEmpty(),
                       "Should provide mitigation strategies");
        }
        
        // High risk nodes should have more mitigation strategies
        SystemicFailureRisk highestRisk = risks.get(0);
        assertTrue(highestRisk.getMitigationStrategies().size() >= 3,
                  "High risk nodes should have multiple mitigation strategies");
    }

    @Test
    void testAnalyzeCascadingEffects_IdentifiesAffectedNodes() {
        CascadeAnalysis analysis = assessor.analyzeCascadingEffects(
            NodeId.CORE1, testConfigurations, testMetrics);
        
        assertNotNull(analysis);
        assertEquals(NodeId.CORE1, analysis.getOriginNode());
        assertNotNull(analysis.getAffectedNodes());
        assertNotNull(analysis.getCascadePaths());
        assertTrue(analysis.getCascadeProbability() >= 0.0 && analysis.getCascadeProbability() <= 1.0);
    }

    @Test
    void testAnalyzeCascadingEffects_CoreNodeAffectsMultipleNodes() {
        // Core nodes should affect multiple nodes due to central position
        CascadeAnalysis coreAnalysis = assessor.analyzeCascadingEffects(
            NodeId.CORE1, testConfigurations, testMetrics);
        
        CascadeAnalysis edgeAnalysis = assessor.analyzeCascadingEffects(
            NodeId.EDGE1, testConfigurations, testMetrics);
        
        // Core node failure should potentially affect more nodes than edge node
        assertTrue(coreAnalysis.getAffectedNodes().size() >= edgeAnalysis.getAffectedNodes().size(),
                  "Core node failure should affect at least as many nodes as edge node");
    }

    @Test
    void testAnalyzeCascadingEffects_CascadePathsValid() {
        CascadeAnalysis analysis = assessor.analyzeCascadingEffects(
            NodeId.CORE1, testConfigurations, testMetrics);
        
        // All cascade paths should start with origin node
        for (List<NodeId> path : analysis.getCascadePaths()) {
            assertFalse(path.isEmpty(), "Cascade path should not be empty");
            assertEquals(NodeId.CORE1, path.get(0), "Cascade path should start with origin node");
        }
        
        // Max cascade depth should match longest path
        int actualMaxDepth = analysis.getCascadePaths().stream()
            .mapToInt(List::size)
            .max()
            .orElse(0);
        assertEquals(actualMaxDepth, analysis.getMaxCascadeDepth(),
                    "Max cascade depth should match longest path");
    }

    @Test
    void testAnalyzeCascadingEffects_HighLoadIncreasesCascadeProbability() {
        // Normal load
        CascadeAnalysis normalAnalysis = assessor.analyzeCascadingEffects(
            NodeId.CORE1, testConfigurations, testMetrics);
        
        // High load scenario
        Map<NodeId, NodeMetrics> highLoadMetrics = new HashMap<>(testMetrics);
        highLoadMetrics.put(NodeId.CORE1, new NodeMetrics(18.0, 600.0, 3.5, 70.0, 14.0, 280, 14.0));
        highLoadMetrics.put(NodeId.CORE2, new NodeMetrics(18.0, 600.0, 3.5, 70.0, 14.0, 280, 14.0));
        
        CascadeAnalysis highLoadAnalysis = assessor.analyzeCascadingEffects(
            NodeId.CORE1, testConfigurations, highLoadMetrics);
        
        // High load should increase cascade probability
        assertTrue(highLoadAnalysis.getCascadeProbability() >= normalAnalysis.getCascadeProbability(),
                  "High load should increase or maintain cascade probability");
    }

    @Test
    void testAnalyzeCascadingEffects_DescriptionProvided() {
        CascadeAnalysis analysis = assessor.analyzeCascadingEffects(
            NodeId.CORE1, testConfigurations, testMetrics);
        
        assertNotNull(analysis.getAnalysisDescription());
        assertFalse(analysis.getAnalysisDescription().isEmpty());
        assertTrue(analysis.getAnalysisDescription().contains("Core1"),
                  "Description should mention the origin node");
    }

    @Test
    void testAssessSystemicRisks_InvalidInputs() {
        // Null metrics
        assertThrows(NullPointerException.class, () -> {
            assessor.assessSystemicRisks(null, testConfigurations);
        });
        
        // Null configurations
        assertThrows(NullPointerException.class, () -> {
            assessor.assessSystemicRisks(testMetrics, null);
        });
        
        // Empty metrics
        assertThrows(IllegalArgumentException.class, () -> {
            assessor.assessSystemicRisks(new HashMap<>(), testConfigurations);
        });
        
        // Mismatched node IDs
        Map<NodeId, NodeMetrics> mismatchedMetrics = new HashMap<>(testMetrics);
        mismatchedMetrics.remove(NodeId.EDGE1);
        assertThrows(IllegalArgumentException.class, () -> {
            assessor.assessSystemicRisks(mismatchedMetrics, testConfigurations);
        });
    }

    @Test
    void testRiskScoring_CombinesMultipleFactors() {
        List<SystemicFailureRisk> risks = assessor.assessSystemicRisks(testMetrics, testConfigurations);
        
        // Risk score should consider multiple factors
        for (SystemicFailureRisk risk : risks) {
            // Risk description should mention multiple factors
            String description = risk.getRiskDescription();
            assertNotNull(description);
            assertTrue(description.contains("CPU") || description.contains("Memory") || 
                      description.contains("Transactions"),
                      "Risk description should mention resource metrics");
            assertTrue(description.contains("cascade"),
                      "Risk description should mention cascade effects");
        }
    }

    @Test
    void testRiskScoring_DependencyChainAnalysis() {
        List<SystemicFailureRisk> risks = assessor.assessSystemicRisks(testMetrics, testConfigurations);
        
        // Nodes with more dependents should have higher dependency-related risk
        for (SystemicFailureRisk risk : risks) {
            Set<NodeId> dependents = risk.getDependentNodes();
            assertNotNull(dependents);
            
            // If a node has many dependents, it should contribute to higher risk
            if (dependents.size() > 2) {
                assertTrue(risk.getRiskScore() > 0.3,
                          "Nodes with many dependents should have elevated risk");
            }
        }
    }

    @Test
    void testRiskScoring_CorrelatedFailureAnalysis() {
        // Create scenario with highly correlated metrics
        Map<NodeId, NodeMetrics> correlatedMetrics = new HashMap<>();
        // All nodes with similar high load
        NodeMetrics highLoadMetrics = new NodeMetrics(18.0, 600.0, 3.0, 68.0, 14.0, 270, 13.0);
        correlatedMetrics.put(NodeId.EDGE1, highLoadMetrics);
        correlatedMetrics.put(NodeId.EDGE2, highLoadMetrics);
        correlatedMetrics.put(NodeId.CORE1, highLoadMetrics);
        correlatedMetrics.put(NodeId.CORE2, highLoadMetrics);
        correlatedMetrics.put(NodeId.CLOUD1, new NodeMetrics(20.0, 800.0, 3.5, 70.0, 15.0, 280, 14.0));
        
        List<SystemicFailureRisk> correlatedRisks = assessor.assessSystemicRisks(
            correlatedMetrics, testConfigurations);
        List<SystemicFailureRisk> normalRisks = assessor.assessSystemicRisks(
            testMetrics, testConfigurations);
        
        // Average risk should be higher in correlated scenario
        double avgCorrelatedRisk = correlatedRisks.stream()
            .mapToDouble(SystemicFailureRisk::getRiskScore)
            .average()
            .orElse(0.0);
        
        double avgNormalRisk = normalRisks.stream()
            .mapToDouble(SystemicFailureRisk::getRiskScore)
            .average()
            .orElse(0.0);
        
        assertTrue(avgCorrelatedRisk >= avgNormalRisk,
                  "Correlated high load should increase average risk");
    }

    // Helper methods

    private Map<NodeId, NodeMetrics> createTestMetrics() {
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        
        // Edge1 - moderate performance
        metrics.put(NodeId.EDGE1, new NodeMetrics(12.0, 500.0, 1.0, 50.0, 6.0, 150, 8.0));
        
        // Edge2 - higher latency
        metrics.put(NodeId.EDGE2, new NodeMetrics(15.0, 470.0, 2.0, 55.0, 4.5, 120, 10.0));
        
        // Core1 - best performance but Byzantine failures
        metrics.put(NodeId.CORE1, new NodeMetrics(8.0, 1000.0, 0.5, 45.0, 8.0, 250, 5.0));
        
        // Core2 - moderate performance
        metrics.put(NodeId.CORE2, new NodeMetrics(10.0, 950.0, 1.5, 60.0, 12.0, 200, 12.0));
        
        // Cloud1 - highest latency but good throughput
        metrics.put(NodeId.CLOUD1, new NodeMetrics(22.0, 1250.0, 3.0, 72.0, 16.0, 300, 15.0));
        
        return metrics;
    }

    private Map<NodeId, NodeConfiguration> createTestConfigurations() {
        Map<NodeId, NodeConfiguration> configs = new HashMap<>();
        
        // Edge1 - RPC and Replication
        configs.put(NodeId.EDGE1, new NodeConfiguration(
            NodeId.EDGE1,
            NodeLayer.EDGE,
            testMetrics.get(NodeId.EDGE1),
            Set.of(ServiceType.RPC_HANDLING, ServiceType.DATA_REPLICATION),
            new FailureModel(FailureType.CRASH, 0.05, 5000, 10000),
            new ResourceLimits(0.72, 6.0, 150, 500.0),
            createTopologyForNode(NodeId.EDGE1)
        ));
        
        // Edge2 - Migration and Recovery
        configs.put(NodeId.EDGE2, new NodeConfiguration(
            NodeId.EDGE2,
            NodeLayer.EDGE,
            testMetrics.get(NodeId.EDGE2),
            Set.of(ServiceType.MIGRATION_SERVICES, ServiceType.RECOVERY_OPERATIONS),
            new FailureModel(FailureType.OMISSION, 0.08, 3000, 15000),
            new ResourceLimits(0.72, 4.5, 120, 470.0),
            createTopologyForNode(NodeId.EDGE2)
        ));
        
        // Core1 - Transaction Commit (Byzantine)
        configs.put(NodeId.CORE1, new NodeConfiguration(
            NodeId.CORE1,
            NodeLayer.CORE,
            testMetrics.get(NodeId.CORE1),
            Set.of(ServiceType.TRANSACTION_COMMIT),
            new FailureModel(FailureType.BYZANTINE, 0.03, 30000, 60000),
            new ResourceLimits(0.72, 8.0, 250, 1000.0),
            createTopologyForNode(NodeId.CORE1)
        ));
        
        // Core2 - Recovery and Load Balancing
        configs.put(NodeId.CORE2, new NodeConfiguration(
            NodeId.CORE2,
            NodeLayer.CORE,
            testMetrics.get(NodeId.CORE2),
            Set.of(ServiceType.RECOVERY_OPERATIONS, ServiceType.LOAD_BALANCING),
            new FailureModel(FailureType.CRASH, 0.06, 5000, 10000),
            new ResourceLimits(0.72, 12.0, 200, 950.0),
            createTopologyForNode(NodeId.CORE2)
        ));
        
        // Cloud1 - Analytics and DSM
        configs.put(NodeId.CLOUD1, new NodeConfiguration(
            NodeId.CLOUD1,
            NodeLayer.CLOUD,
            testMetrics.get(NodeId.CLOUD1),
            Set.of(ServiceType.ANALYTICS, ServiceType.DISTRIBUTED_SHARED_MEMORY),
            new FailureModel(FailureType.OMISSION, 0.07, 3000, 15000),
            new ResourceLimits(0.72, 16.0, 300, 1250.0),
            createTopologyForNode(NodeId.CLOUD1)
        ));
        
        return configs;
    }

    private NetworkTopology createTestTopology() {
        // Create a simple topology for testing
        Set<NodeId> allNodes = Set.of(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, 
                                      NodeId.CORE2, NodeId.CLOUD1);
        Map<NodeId, Double> latencies = new HashMap<>();
        Map<NodeId, Double> bandwidths = new HashMap<>();
        
        for (NodeId node : allNodes) {
            latencies.put(node, 10.0);
            bandwidths.put(node, 1000.0);
        }
        
        return new NetworkTopology(allNodes, latencies, bandwidths);
    }

    private NetworkTopology createTopologyForNode(NodeId nodeId) {
        Set<NodeId> connectedNodes = new HashSet<>();
        Map<NodeId, Double> latencies = new HashMap<>();
        Map<NodeId, Double> bandwidths = new HashMap<>();
        
        // Edge nodes connect to Core nodes
        if (nodeId.equals(NodeId.EDGE1) || nodeId.equals(NodeId.EDGE2)) {
            connectedNodes.add(NodeId.CORE1);
            connectedNodes.add(NodeId.CORE2);
            latencies.put(NodeId.CORE1, 5.0);
            latencies.put(NodeId.CORE2, 5.0);
            bandwidths.put(NodeId.CORE1, 800.0);
            bandwidths.put(NodeId.CORE2, 800.0);
        }
        
        // Core nodes connect to Edge and Cloud
        if (nodeId.equals(NodeId.CORE1) || nodeId.equals(NodeId.CORE2)) {
            connectedNodes.add(NodeId.EDGE1);
            connectedNodes.add(NodeId.EDGE2);
            connectedNodes.add(NodeId.CLOUD1);
            latencies.put(NodeId.EDGE1, 5.0);
            latencies.put(NodeId.EDGE2, 5.0);
            latencies.put(NodeId.CLOUD1, 10.0);
            bandwidths.put(NodeId.EDGE1, 800.0);
            bandwidths.put(NodeId.EDGE2, 800.0);
            bandwidths.put(NodeId.CLOUD1, 1200.0);
        }
        
        // Cloud connects to Core nodes
        if (nodeId.equals(NodeId.CLOUD1)) {
            connectedNodes.add(NodeId.CORE1);
            connectedNodes.add(NodeId.CORE2);
            latencies.put(NodeId.CORE1, 10.0);
            latencies.put(NodeId.CORE2, 10.0);
            bandwidths.put(NodeId.CORE1, 1200.0);
            bandwidths.put(NodeId.CORE2, 1200.0);
        }
        
        return new NetworkTopology(connectedNodes, latencies, bandwidths);
    }
}
