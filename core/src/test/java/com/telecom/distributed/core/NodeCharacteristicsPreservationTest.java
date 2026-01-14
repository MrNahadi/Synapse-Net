package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.telecom.distributed.core.factory.NodeFactory;
import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for node characteristics preservation.
 * **Feature: distributed-telecom-system, Property 2: Node Characteristics Preservation**
 * **Validates: Requirements 2.1**
 */
@RunWith(JUnitQuickcheck.class)
@Tag("Feature: distributed-telecom-system, Property 2: Node Characteristics Preservation")
public class NodeCharacteristicsPreservationTest {

    /**
     * Property 2: Node Characteristics Preservation
     * For any system initialization, all five nodes (Edge1, Edge2, Core1, Core2, Cloud1) 
     * should be present with their correct baseline characteristics from the dataset.
     */
    @Property(trials = 10)
    public void nodeCharacteristicsPreservation() {
        // Create all nodes using the factory
        Set<NodeConfiguration> allNodes = NodeFactory.createAllNodes();
        
        // Verify all five nodes are present
        assertEquals(5, allNodes.size(), "All five nodes should be present");
        
        // Verify each node has correct characteristics
        for (NodeConfiguration node : allNodes) {
            validateNodeCharacteristics(node);
        }
        
        // Verify specific node IDs are present
        Set<NodeId> nodeIds = allNodes.stream()
            .map(NodeConfiguration::getNodeId)
            .collect(java.util.stream.Collectors.toSet());
            
        assertTrue(nodeIds.contains(NodeId.EDGE1), "Edge1 should be present");
        assertTrue(nodeIds.contains(NodeId.EDGE2), "Edge2 should be present");
        assertTrue(nodeIds.contains(NodeId.CORE1), "Core1 should be present");
        assertTrue(nodeIds.contains(NodeId.CORE2), "Core2 should be present");
        assertTrue(nodeIds.contains(NodeId.CLOUD1), "Cloud1 should be present");
    }

    /**
     * Validates that a node configuration has characteristics within dataset bounds.
     */
    private void validateNodeCharacteristics(NodeConfiguration node) {
        NodeMetrics metrics = node.getBaselineMetrics();
        NodeId nodeId = node.getNodeId();
        
        // Validate metrics are within dataset ranges
        assertTrue(metrics.getLatency() >= 8.0 && metrics.getLatency() <= 22.0,
            "Node " + nodeId + " latency should be within 8-22ms range, got: " + metrics.getLatency());
            
        assertTrue(metrics.getThroughput() >= 470.0 && metrics.getThroughput() <= 1250.0,
            "Node " + nodeId + " throughput should be within 470-1250 Mbps range, got: " + metrics.getThroughput());
            
        assertTrue(metrics.getCpuUtilization() >= 45.0 && metrics.getCpuUtilization() <= 72.0,
            "Node " + nodeId + " CPU utilization should be within 45-72% range, got: " + metrics.getCpuUtilization());
            
        assertTrue(metrics.getMemoryUsage() >= 4.0 && metrics.getMemoryUsage() <= 16.0,
            "Node " + nodeId + " memory usage should be within 4.0-16.0 GB range, got: " + metrics.getMemoryUsage());
            
        assertTrue(metrics.getTransactionsPerSec() >= 100 && metrics.getTransactionsPerSec() <= 300,
            "Node " + nodeId + " transactions per second should be within 100-300 range, got: " + metrics.getTransactionsPerSec());
            
        assertTrue(metrics.getLockContention() >= 5.0 && metrics.getLockContention() <= 15.0,
            "Node " + nodeId + " lock contention should be within 5-15% range, got: " + metrics.getLockContention());
        
        // Validate layer assignment
        NodeLayer expectedLayer = NodeLayer.fromNodeId(nodeId);
        assertEquals(expectedLayer, node.getLayer(),
            "Node " + nodeId + " should be in " + expectedLayer + " layer");
        
        // Validate failure model matches expected type
        FailureType expectedFailureType = FailureType.getExpectedFailureType(nodeId);
        assertEquals(expectedFailureType, node.getFailureModel().getPrimaryFailureType(),
            "Node " + nodeId + " should have " + expectedFailureType + " failure type");
        
        // Validate resource limits are consistent with metrics
        ResourceLimits limits = node.getResourceLimits();
        assertTrue(limits.getMaxCpuUtilization() >= metrics.getCpuUtilization() / 100.0,
            "Node " + nodeId + " CPU limit should be >= current utilization");
            
        assertTrue(limits.getMaxMemoryUsage() >= metrics.getMemoryUsage(),
            "Node " + nodeId + " memory limit should be >= current usage");
            
        assertTrue(limits.getMaxNetworkBandwidth() >= metrics.getThroughput(),
            "Node " + nodeId + " bandwidth limit should be >= current throughput");
        
        // Validate supported services are not empty
        assertFalse(node.getSupportedServices().isEmpty(),
            "Node " + nodeId + " should have at least one supported service");
        
        // Validate network topology
        NetworkTopology topology = node.getNetworkTopology();
        assertNotNull(topology, "Node " + nodeId + " should have network topology");
        assertFalse(topology.getConnectedNodes().isEmpty(),
            "Node " + nodeId + " should be connected to at least one other node");
    }
}