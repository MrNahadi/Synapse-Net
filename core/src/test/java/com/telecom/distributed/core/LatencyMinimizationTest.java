package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.telecom.distributed.core.factory.NodeFactory;
import com.telecom.distributed.core.impl.OptimizedCommunicationManager;
import com.telecom.distributed.core.model.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Property-based test for latency minimization in communication management.
 * 
 * Feature: distributed-telecom-system, Property 27: Latency Minimization
 * Validates: Requirements 14.1
 * 
 * Property: For any communication between heterogeneous nodes, the routing should 
 * minimize end-to-end latency given network topology.
 */
@RunWith(JUnitQuickcheck.class)
public class LatencyMinimizationTest {
    private static final Logger logger = LoggerFactory.getLogger(LatencyMinimizationTest.class);
    
    /**
     * Property: Optimal routing minimizes latency between all node pairs.
     */
    @Property(trials = 100)
    public void optimalRoutingMinimizesLatency() {
        Map<NodeId, NodeConfiguration> allNodes = createAllNodes();
        
        OptimizedCommunicationManager commManager = new OptimizedCommunicationManager(
            allNodes, null, null, 10000L, 5000L, 100, 50
        );
        
        try {
            // Test all pairs
            List<NodeId> nodeIds = new ArrayList<>(allNodes.keySet());
            for (int i = 0; i < nodeIds.size(); i++) {
                for (int j = i + 1; j < nodeIds.size(); j++) {
                    NodeId source = nodeIds.get(i);
                    NodeId dest = nodeIds.get(j);
                    
                    List<NodeId> route = commManager.getOptimalRoute(source, dest);
                    double latency = commManager.calculateRouteLatency(route);
                    
                    assertNotNull("Route should not be null", route);
                    assertEquals("Route should start at source", source, route.get(0));
                    assertEquals("Route should end at dest", dest, route.get(route.size() - 1));
                    assertTrue("Latency should be positive", latency > 0);
                    assertTrue("Latency should be within bounds", latency <= 44.0); // max 2 hops * 22ms
                }
            }
        } finally {
            commManager.shutdown();
        }
    }
    
    /**
     * Property: Latency is symmetric for direct routes.
     */
    @Property(trials = 100)
    public void latencyIsSymmetric() {
        Map<NodeId, NodeConfiguration> allNodes = createAllNodes();
        
        OptimizedCommunicationManager commManager = new OptimizedCommunicationManager(
            allNodes, null, null, 10000L, 5000L, 100, 50
        );
        
        try {
            List<NodeId> nodeIds = new ArrayList<>(allNodes.keySet());
            for (int i = 0; i < nodeIds.size(); i++) {
                for (int j = i + 1; j < nodeIds.size(); j++) {
                    NodeId nodeA = nodeIds.get(i);
                    NodeId nodeB = nodeIds.get(j);
                    
                    double latencyAtoB = commManager.calculateRouteLatency(
                        commManager.getOptimalRoute(nodeA, nodeB)
                    );
                    double latencyBtoA = commManager.calculateRouteLatency(
                        commManager.getOptimalRoute(nodeB, nodeA)
                    );
                    
                    assertEquals("Latency should be symmetric", latencyAtoB, latencyBtoA, 0.1);
                }
            }
        } finally {
            commManager.shutdown();
        }
    }
    
    private Map<NodeId, NodeConfiguration> createAllNodes() {
        return NodeFactory.createAllNodes().stream()
            .collect(Collectors.toMap(NodeConfiguration::getNodeId, node -> node));
    }
}
