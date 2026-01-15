package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransactionBottleneckAnalyzer functionality.
 * Tests lock contention and resource usage analysis as specified in Requirements 11.2, 11.3, 11.4.
 */
public class TransactionBottleneckAnalyzerTest {

    private TransactionBottleneckAnalyzer analyzer;
    private Map<NodeId, NodeMetrics> testNodeMetrics;
    private Map<TransactionId, DistributedTransaction> testActiveTransactions;
    private Map<String, ResourceLock> testLockRegistry;

    @BeforeEach
    void setUp() {
        analyzer = new TransactionBottleneckAnalyzer();
        testNodeMetrics = createTestNodeMetrics();
        testActiveTransactions = createTestActiveTransactions();
        testLockRegistry = createTestLockRegistry();
    }

    @Test
    void testIdentifyTransactionBottlenecks() {
        List<TransactionBottleneckAnalyzer.TransactionBottleneckResult> results = 
            analyzer.identifyTransactionBottlenecks(testNodeMetrics, testActiveTransactions, testLockRegistry);
        
        // Should analyze all 5 nodes
        assertEquals(5, results.size());
        
        // Results should be sorted by bottleneck score (highest first)
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getBottleneckScore() >= results.get(i + 1).getBottleneckScore(),
                "Results should be sorted by bottleneck score");
        }
        
        // Each result should have valid data
        for (TransactionBottleneckAnalyzer.TransactionBottleneckResult result : results) {
            assertNotNull(result.getNodeId());
            assertTrue(result.getBottleneckScore() >= 0.0 && result.getBottleneckScore() <= 1.0);
            assertNotNull(result.getLockAnalysis());
            assertNotNull(result.getResourceAnalysis());
            assertNotNull(result.getExplanation());
        }
    }

    @Test
    void testLockContentionAnalysis() {
        // Test with high lock contention node (Core2)
        NodeMetrics highContentionMetrics = new NodeMetrics(10.0, 950.0, 1.0, 55.0, 6.0, 200, 14.0);
        testNodeMetrics.put(NodeId.CORE2, highContentionMetrics);
        
        List<TransactionBottleneckAnalyzer.TransactionBottleneckResult> results = 
            analyzer.identifyTransactionBottlenecks(testNodeMetrics, testActiveTransactions, testLockRegistry);
        
        // Find Core2 result
        TransactionBottleneckAnalyzer.TransactionBottleneckResult core2Result = results.stream()
            .filter(r -> r.getNodeId() == NodeId.CORE2)
            .findFirst()
            .orElseThrow();
        
        TransactionBottleneckAnalyzer.LockContentionAnalysis lockAnalysis = core2Result.getLockAnalysis();
        
        // Verify lock contention analysis
        assertEquals(14.0, lockAnalysis.getLockContentionPercentage(), 0.001);
        assertTrue(lockAnalysis.getLocksHeld() >= 0);
        assertTrue(lockAnalysis.getAvgLockHoldTime() >= 0.0);
        assertTrue(lockAnalysis.getWaitingTransactions() >= 0);
        assertNotNull(lockAnalysis.getSeverity());
        
        // High contention should result in HIGH severity
        assertEquals(TransactionBottleneckAnalyzer.ContentionSeverity.HIGH, lockAnalysis.getSeverity());
    }

    @Test
    void testResourceUsageAnalysis() {
        // Test with high resource usage node (Cloud1)
        NodeMetrics highResourceMetrics = new NodeMetrics(22.0, 1250.0, 3.0, 70.0, 15.0, 300, 5.0);
        testNodeMetrics.put(NodeId.CLOUD1, highResourceMetrics);
        
        List<TransactionBottleneckAnalyzer.TransactionBottleneckResult> results = 
            analyzer.identifyTransactionBottlenecks(testNodeMetrics, testActiveTransactions, testLockRegistry);
        
        // Find Cloud1 result
        TransactionBottleneckAnalyzer.TransactionBottleneckResult cloud1Result = results.stream()
            .filter(r -> r.getNodeId() == NodeId.CLOUD1)
            .findFirst()
            .orElseThrow();
        
        TransactionBottleneckAnalyzer.ResourceUsageAnalysis resourceAnalysis = cloud1Result.getResourceAnalysis();
        
        // Verify resource usage analysis
        assertTrue(resourceAnalysis.getNormalizedCpu() >= 0.0 && resourceAnalysis.getNormalizedCpu() <= 1.0);
        assertTrue(resourceAnalysis.getNormalizedMemory() >= 0.0 && resourceAnalysis.getNormalizedMemory() <= 1.0);
        assertTrue(resourceAnalysis.getResourcePressure() >= 0.0 && resourceAnalysis.getResourcePressure() <= 1.0);
        assertTrue(resourceAnalysis.getTransactionEfficiency() >= 0.0);
        
        // High resource usage should be detected
        assertTrue(resourceAnalysis.isCpuConstrained() || resourceAnalysis.isMemoryConstrained());
    }

    @Test
    void testBottleneckScoreCalculation() {
        List<TransactionBottleneckAnalyzer.TransactionBottleneckResult> results = 
            analyzer.identifyTransactionBottlenecks(testNodeMetrics, testActiveTransactions, testLockRegistry);
        
        // All bottleneck scores should be valid
        for (TransactionBottleneckAnalyzer.TransactionBottleneckResult result : results) {
            double score = result.getBottleneckScore();
            assertTrue(score >= 0.0 && score <= 1.0, 
                "Bottleneck score should be between 0.0 and 1.0, got: " + score);
        }
        
        // Node with highest lock contention should have higher score
        TransactionBottleneckAnalyzer.TransactionBottleneckResult highContentionResult = results.stream()
            .filter(r -> r.getLockAnalysis().getLockContentionPercentage() > 10.0)
            .findFirst()
            .orElse(null);
        
        if (highContentionResult != null) {
            assertTrue(highContentionResult.getBottleneckScore() > 0.3, 
                "High contention node should have significant bottleneck score");
        }
    }

    @Test
    void testLockContentionSeverityClassification() {
        // Test LOW severity
        NodeMetrics lowContentionMetrics = new NodeMetrics(8.0, 1000.0, 0.5, 45.0, 8.0, 250, 6.0);
        testNodeMetrics.put(NodeId.CORE1, lowContentionMetrics);
        
        // Test MEDIUM severity
        NodeMetrics mediumContentionMetrics = new NodeMetrics(12.0, 500.0, 1.0, 50.0, 6.0, 150, 9.0);
        testNodeMetrics.put(NodeId.EDGE1, mediumContentionMetrics);
        
        // Test HIGH severity
        NodeMetrics highContentionMetrics = new NodeMetrics(15.0, 470.0, 2.0, 55.0, 4.5, 120, 13.0);
        testNodeMetrics.put(NodeId.EDGE2, highContentionMetrics);
        
        List<TransactionBottleneckAnalyzer.TransactionBottleneckResult> results = 
            analyzer.identifyTransactionBottlenecks(testNodeMetrics, testActiveTransactions, testLockRegistry);
        
        // Verify severity classifications
        Map<NodeId, TransactionBottleneckAnalyzer.ContentionSeverity> severityMap = new HashMap<>();
        for (TransactionBottleneckAnalyzer.TransactionBottleneckResult result : results) {
            severityMap.put(result.getNodeId(), result.getLockAnalysis().getSeverity());
        }
        
        assertEquals(TransactionBottleneckAnalyzer.ContentionSeverity.LOW, severityMap.get(NodeId.CORE1));
        assertEquals(TransactionBottleneckAnalyzer.ContentionSeverity.MEDIUM, severityMap.get(NodeId.EDGE1));
        assertEquals(TransactionBottleneckAnalyzer.ContentionSeverity.HIGH, severityMap.get(NodeId.EDGE2));
    }

    @Test
    void testResourceConstraintDetection() {
        // Test CPU constrained node
        NodeMetrics cpuConstrainedMetrics = new NodeMetrics(10.0, 950.0, 1.0, 68.0, 6.0, 200, 8.0);
        testNodeMetrics.put(NodeId.CORE2, cpuConstrainedMetrics);
        
        // Test memory constrained node
        NodeMetrics memoryConstrainedMetrics = new NodeMetrics(22.0, 1250.0, 3.0, 60.0, 14.0, 300, 5.0);
        testNodeMetrics.put(NodeId.CLOUD1, memoryConstrainedMetrics);
        
        List<TransactionBottleneckAnalyzer.TransactionBottleneckResult> results = 
            analyzer.identifyTransactionBottlenecks(testNodeMetrics, testActiveTransactions, testLockRegistry);
        
        // Find constrained nodes
        TransactionBottleneckAnalyzer.TransactionBottleneckResult core2Result = results.stream()
            .filter(r -> r.getNodeId() == NodeId.CORE2)
            .findFirst()
            .orElseThrow();
        
        TransactionBottleneckAnalyzer.TransactionBottleneckResult cloud1Result = results.stream()
            .filter(r -> r.getNodeId() == NodeId.CLOUD1)
            .findFirst()
            .orElseThrow();
        
        // Verify constraint detection
        assertTrue(core2Result.getResourceAnalysis().isCpuConstrained(), 
            "Core2 should be detected as CPU constrained");
        assertTrue(cloud1Result.getResourceAnalysis().isMemoryConstrained(), 
            "Cloud1 should be detected as memory constrained");
    }

    @Test
    void testTransactionEfficiencyCalculation() {
        List<TransactionBottleneckAnalyzer.TransactionBottleneckResult> results = 
            analyzer.identifyTransactionBottlenecks(testNodeMetrics, testActiveTransactions, testLockRegistry);
        
        for (TransactionBottleneckAnalyzer.TransactionBottleneckResult result : results) {
            double efficiency = result.getResourceAnalysis().getTransactionEfficiency();
            assertTrue(efficiency >= 0.0, "Transaction efficiency should be non-negative");
            
            // Higher transaction rate with lower resource usage should yield higher efficiency
            NodeMetrics metrics = testNodeMetrics.get(result.getNodeId());
            if (metrics.getTransactionsPerSec() > 200 && 
                metrics.getCpuUtilization() < 50.0 && 
                metrics.getMemoryUsage() < 8.0) {
                assertTrue(efficiency > 1.0, "Efficient nodes should have efficiency > 1.0");
            }
        }
    }

    @Test
    void testBottleneckExplanationGeneration() {
        List<TransactionBottleneckAnalyzer.TransactionBottleneckResult> results = 
            analyzer.identifyTransactionBottlenecks(testNodeMetrics, testActiveTransactions, testLockRegistry);
        
        for (TransactionBottleneckAnalyzer.TransactionBottleneckResult result : results) {
            String explanation = result.getExplanation();
            
            // Explanation should contain key information
            assertTrue(explanation.contains(result.getNodeId().getId()), 
                "Explanation should contain node ID");
            assertTrue(explanation.contains("bottleneck analysis"), 
                "Explanation should mention bottleneck analysis");
            assertTrue(explanation.contains("Lock contention"), 
                "Explanation should contain lock contention info");
            assertTrue(explanation.contains("Resource usage"), 
                "Explanation should contain resource usage info");
            assertTrue(explanation.contains("Transaction processing"), 
                "Explanation should contain transaction processing info");
        }
    }

    @Test
    void testInputValidation() {
        // Test null inputs
        assertThrows(NullPointerException.class, () -> {
            analyzer.identifyTransactionBottlenecks(null, testActiveTransactions, testLockRegistry);
        });
        
        assertThrows(NullPointerException.class, () -> {
            analyzer.identifyTransactionBottlenecks(testNodeMetrics, null, testLockRegistry);
        });
        
        assertThrows(NullPointerException.class, () -> {
            analyzer.identifyTransactionBottlenecks(testNodeMetrics, testActiveTransactions, null);
        });
        
        // Test empty node metrics
        assertThrows(IllegalArgumentException.class, () -> {
            analyzer.identifyTransactionBottlenecks(new HashMap<>(), testActiveTransactions, testLockRegistry);
        });
    }

    private Map<NodeId, NodeMetrics> createTestNodeMetrics() {
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        
        // Edge1 - moderate performance
        metrics.put(NodeId.EDGE1, new NodeMetrics(12.0, 500.0, 1.0, 50.0, 6.0, 150, 8.0));
        
        // Edge2 - higher contention
        metrics.put(NodeId.EDGE2, new NodeMetrics(15.0, 470.0, 2.0, 55.0, 4.5, 120, 11.0));
        
        // Core1 - best performance, low contention
        metrics.put(NodeId.CORE1, new NodeMetrics(8.0, 1000.0, 0.5, 45.0, 8.0, 250, 5.0));
        
        // Core2 - moderate performance, medium contention
        metrics.put(NodeId.CORE2, new NodeMetrics(10.0, 950.0, 1.5, 60.0, 12.0, 200, 9.0));
        
        // Cloud1 - high latency, high resources, low contention
        metrics.put(NodeId.CLOUD1, new NodeMetrics(22.0, 1250.0, 3.0, 65.0, 16.0, 300, 6.0));
        
        return metrics;
    }

    private Map<TransactionId, DistributedTransaction> createTestActiveTransactions() {
        Map<TransactionId, DistributedTransaction> transactions = new HashMap<>();
        
        // Create some test transactions
        TransactionId tx1 = TransactionId.generate();
        Set<NodeId> participants1 = Set.of(NodeId.EDGE1, NodeId.CORE1);
        DistributedTransaction transaction1 = new DistributedTransaction(tx1, participants1, 30000);
        transaction1.setState(TransactionState.PREPARING);
        transactions.put(tx1, transaction1);
        
        TransactionId tx2 = TransactionId.generate();
        Set<NodeId> participants2 = Set.of(NodeId.EDGE2, NodeId.CORE2, NodeId.CLOUD1);
        DistributedTransaction transaction2 = new DistributedTransaction(tx2, participants2, 30000);
        transaction2.setState(TransactionState.ACTIVE);
        transactions.put(tx2, transaction2);
        
        TransactionId tx3 = TransactionId.generate();
        Set<NodeId> participants3 = Set.of(NodeId.CORE1, NodeId.CORE2);
        DistributedTransaction transaction3 = new DistributedTransaction(tx3, participants3, 30000);
        transaction3.setState(TransactionState.PREPARING);
        transactions.put(tx3, transaction3);
        
        return transactions;
    }

    private Map<String, ResourceLock> createTestLockRegistry() {
        Map<String, ResourceLock> locks = new HashMap<>();
        
        // Create some test locks
        TransactionId tx1 = TransactionId.generate();
        TransactionId tx2 = TransactionId.generate();
        
        ResourceLock lock1 = new ResourceLock("resource_1", NodeId.EDGE1, tx1, LockType.EXCLUSIVE);
        ResourceLock lock2 = new ResourceLock("resource_2", NodeId.CORE2, tx2, LockType.SHARED);
        ResourceLock lock3 = new ResourceLock("resource_3", NodeId.CORE1, tx1, LockType.EXCLUSIVE);
        
        locks.put("resource_1", lock1);
        locks.put("resource_2", lock2);
        locks.put("resource_3", lock3);
        
        return locks;
    }
}