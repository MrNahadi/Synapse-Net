package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.telecom.distributed.core.impl.LoadBalancerImpl;
import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for memory load distribution.
 * **Feature: distributed-telecom-system, Property 18: Memory Load Distribution**
 * **Validates: Requirements 7.2**
 */
@RunWith(JUnitQuickcheck.class)
@Tag("Feature: distributed-telecom-system, Property 18: Memory Load Distribution")
public class MemoryLoadDistributionTest {

    /**
     * Property 18: Memory Load Distribution
     * For any load balancing operation, memory loads should be distributed appropriately 
     * across nodes based on their capacity.
     */
    @Property(trials = 100)
    public void memoryLoadDistribution(@InRange(minInt = 5, maxInt = 40) int numRequests,
                                     @InRange(minDouble = 0.5, maxDouble = 4.0) double avgMemoryRequirement) {
        // Setup load balancer with realistic node metrics
        LoadBalancer loadBalancer = new LoadBalancerImpl();
        Map<NodeId, NodeMetrics> nodeMetrics = createRealisticNodeMetrics();
        loadBalancer.updateNodeMetrics(nodeMetrics);
        
        // Generate service requests with varying memory requirements
        List<ServiceRequest> requests = generateServiceRequests(numRequests, avgMemoryRequirement);
        
        // Allocate all requests
        Map<NodeId, List<ServiceRequest>> allocations = new HashMap<>();
        for (ServiceRequest request : requests) {
            NodeId selectedNode = loadBalancer.selectNode(request);
            allocations.computeIfAbsent(selectedNode, k -> new ArrayList<>()).add(request);
        }
        
        // Verify memory load distribution properties
        verifyMemoryCapacityAwareness(allocations, nodeMetrics);
        verifyNoMemoryOverload(allocations, nodeMetrics);
        verifyHighMemoryNodesGetLargeRequests(allocations, nodeMetrics);
    }
    
    private Map<NodeId, NodeMetrics> createRealisticNodeMetrics() {
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        
        // Edge1: Moderate memory capacity
        metrics.put(NodeId.EDGE1, new NodeMetrics(12.0, 500.0, 1.0, 55.0, 6.0, 150, 8.0));
        
        // Edge2: Lower memory capacity
        metrics.put(NodeId.EDGE2, new NodeMetrics(15.0, 470.0, 2.0, 60.0, 4.5, 120, 10.0));
        
        // Core1: Good memory capacity
        metrics.put(NodeId.CORE1, new NodeMetrics(8.0, 1000.0, 0.5, 50.0, 8.0, 250, 6.0));
        
        // Core2: High memory capacity
        metrics.put(NodeId.CORE2, new NodeMetrics(10.0, 950.0, 1.5, 65.0, 12.0, 200, 7.0));
        
        // Cloud1: Highest memory capacity
        metrics.put(NodeId.CLOUD1, new NodeMetrics(22.0, 1250.0, 3.0, 45.0, 16.0, 300, 12.0));
        
        return metrics;
    }
    
    private List<ServiceRequest> generateServiceRequests(int numRequests, double avgMemoryRequirement) {
        List<ServiceRequest> requests = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        for (int i = 0; i < numRequests; i++) {
            // Vary memory requirements around the average
            double memoryReq = Math.max(0.1, Math.min(8.0, 
                avgMemoryRequirement + (random.nextGaussian() * avgMemoryRequirement * 0.4)));
            
            ServiceRequest request = new ServiceRequest(
                new ServiceId("service-" + i),
                ServiceType.RPC_HANDLING,
                random.nextDouble() * 15.0 + 5.0, // 5-20% CPU
                memoryReq,
                random.nextInt(50) + 10, // 10-60 transactions/sec
                random.nextInt(10) + 1 // Priority 1-10
            );
            requests.add(request);
        }
        
        return requests;
    }
    
    private void verifyMemoryCapacityAwareness(Map<NodeId, List<ServiceRequest>> allocations,
                                             Map<NodeId, NodeMetrics> nodeMetrics) {
        // Calculate total memory load per node
        Map<NodeId, Double> totalMemoryLoad = new HashMap<>();
        for (Map.Entry<NodeId, List<ServiceRequest>> entry : allocations.entrySet()) {
            double totalLoad = entry.getValue().stream()
                .mapToDouble(ServiceRequest::getMemoryRequirement)
                .sum();
            totalMemoryLoad.put(entry.getKey(), totalLoad);
        }
        
        // Verify that higher-memory nodes get proportionally more memory-intensive requests
        List<NodeId> nodesByMemoryCapacity = nodeMetrics.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(
                e2.getValue().getMemoryUsage(),
                e1.getValue().getMemoryUsage()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Check that distribution follows memory capacity ordering
        for (int i = 0; i < nodesByMemoryCapacity.size() - 1; i++) {
            NodeId higherMemoryNode = nodesByMemoryCapacity.get(i);
            NodeId lowerMemoryNode = nodesByMemoryCapacity.get(i + 1);
            
            double higherLoad = totalMemoryLoad.getOrDefault(higherMemoryNode, 0.0);
            double lowerLoad = totalMemoryLoad.getOrDefault(lowerMemoryNode, 0.0);
            
            NodeMetrics higherMetrics = nodeMetrics.get(higherMemoryNode);
            NodeMetrics lowerMetrics = nodeMetrics.get(lowerMemoryNode);
            
            double memoryCapacityRatio = higherMetrics.getMemoryUsage() / lowerMetrics.getMemoryUsage();
            
            // Only enforce if memory capacity difference is significant (>20%)
            if (memoryCapacityRatio > 1.2) {
                assertTrue(higherLoad >= lowerLoad * 0.6, // Allow 40% variance
                    String.format("Higher memory capacity node %s (%.1fGB, load: %.2fGB) should generally get more memory load than %s (%.1fGB, load: %.2fGB)",
                        higherMemoryNode, higherMetrics.getMemoryUsage(), higherLoad,
                        lowerMemoryNode, lowerMetrics.getMemoryUsage(), lowerLoad));
            }
        }
    }
    
    private void verifyNoMemoryOverload(Map<NodeId, List<ServiceRequest>> allocations,
                                      Map<NodeId, NodeMetrics> nodeMetrics) {
        for (Map.Entry<NodeId, List<ServiceRequest>> entry : allocations.entrySet()) {
            NodeId nodeId = entry.getKey();
            List<ServiceRequest> nodeRequests = entry.getValue();
            NodeMetrics metrics = nodeMetrics.get(nodeId);
            
            double totalMemoryLoad = nodeRequests.stream()
                .mapToDouble(ServiceRequest::getMemoryRequirement)
                .sum();
            
            // Verify node is not severely overloaded
            // Allow some oversubscription but not excessive
            assertTrue(totalMemoryLoad <= metrics.getMemoryUsage() * 1.3,
                String.format("Node %s should not be severely memory overloaded. Total memory load: %.2fGB, Capacity: %.2fGB",
                    nodeId, totalMemoryLoad, metrics.getMemoryUsage()));
        }
    }
    
    private void verifyHighMemoryNodesGetLargeRequests(Map<NodeId, List<ServiceRequest>> allocations,
                                                     Map<NodeId, NodeMetrics> nodeMetrics) {
        // Find the node with highest memory capacity
        NodeId highestMemoryNode = nodeMetrics.entrySet().stream()
            .max(Comparator.comparingDouble(e -> e.getValue().getMemoryUsage()))
            .map(Map.Entry::getKey)
            .orElse(null);
        
        // Find the node with lowest memory capacity
        NodeId lowestMemoryNode = nodeMetrics.entrySet().stream()
            .min(Comparator.comparingDouble(e -> e.getValue().getMemoryUsage()))
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (highestMemoryNode != null && lowestMemoryNode != null && 
            !highestMemoryNode.equals(lowestMemoryNode)) {
            
            List<ServiceRequest> highMemoryNodeRequests = allocations.getOrDefault(highestMemoryNode, new ArrayList<>());
            List<ServiceRequest> lowMemoryNodeRequests = allocations.getOrDefault(lowestMemoryNode, new ArrayList<>());
            
            if (!highMemoryNodeRequests.isEmpty() && !lowMemoryNodeRequests.isEmpty()) {
                double avgMemoryHighNode = highMemoryNodeRequests.stream()
                    .mapToDouble(ServiceRequest::getMemoryRequirement)
                    .average()
                    .orElse(0.0);
                
                double avgMemoryLowNode = lowMemoryNodeRequests.stream()
                    .mapToDouble(ServiceRequest::getMemoryRequirement)
                    .average()
                    .orElse(0.0);
                
                // High memory node should generally get requests with higher average memory requirements
                // Allow some variance due to other factors (CPU, priority, etc.)
                assertTrue(avgMemoryHighNode >= avgMemoryLowNode * 0.7,
                    String.format("Highest memory node %s (avg req: %.2fGB) should generally get higher memory requests than lowest memory node %s (avg req: %.2fGB)",
                        highestMemoryNode, avgMemoryHighNode, lowestMemoryNode, avgMemoryLowNode));
            }
        }
    }
}