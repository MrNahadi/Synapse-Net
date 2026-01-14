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
 * Property-based test for transaction load distribution.
 * **Feature: distributed-telecom-system, Property 19: Transaction Load Distribution**
 * **Validates: Requirements 7.3**
 */
@RunWith(JUnitQuickcheck.class)
@Tag("Feature: distributed-telecom-system, Property 19: Transaction Load Distribution")
public class TransactionLoadDistributionTest {

    /**
     * Property 19: Transaction Load Distribution
     * For any transaction workload, processing should be distributed across nodes 
     * to prevent bottlenecks.
     */
    @Property(trials = 100)
    public void transactionLoadDistribution(@InRange(minInt = 10, maxInt = 60) int numRequests,
                                          @InRange(minInt = 5, maxInt = 100) int avgTransactionLoad) {
        // Setup load balancer with realistic node metrics
        LoadBalancer loadBalancer = new LoadBalancerImpl();
        Map<NodeId, NodeMetrics> nodeMetrics = createRealisticNodeMetrics();
        loadBalancer.updateNodeMetrics(nodeMetrics);
        
        // Generate service requests with varying transaction loads
        List<ServiceRequest> requests = generateServiceRequests(numRequests, avgTransactionLoad);
        
        // Allocate all requests
        Map<NodeId, List<ServiceRequest>> allocations = new HashMap<>();
        for (ServiceRequest request : requests) {
            NodeId selectedNode = loadBalancer.selectNode(request);
            allocations.computeIfAbsent(selectedNode, k -> new ArrayList<>()).add(request);
        }
        
        // Verify transaction load distribution properties
        verifyTransactionCapacityAwareness(allocations, nodeMetrics);
        verifyNoTransactionBottlenecks(allocations, nodeMetrics);
        verifyLoadSpreadAcrossNodes(allocations);
    }
    
    private Map<NodeId, NodeMetrics> createRealisticNodeMetrics() {
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        
        // Edge1: Moderate transaction capacity
        metrics.put(NodeId.EDGE1, new NodeMetrics(12.0, 500.0, 1.0, 55.0, 6.0, 150, 8.0));
        
        // Edge2: Lower transaction capacity
        metrics.put(NodeId.EDGE2, new NodeMetrics(15.0, 470.0, 2.0, 60.0, 4.5, 120, 10.0));
        
        // Core1: High transaction capacity
        metrics.put(NodeId.CORE1, new NodeMetrics(8.0, 1000.0, 0.5, 50.0, 8.0, 250, 6.0));
        
        // Core2: Good transaction capacity
        metrics.put(NodeId.CORE2, new NodeMetrics(10.0, 950.0, 1.5, 65.0, 12.0, 200, 7.0));
        
        // Cloud1: Highest transaction capacity
        metrics.put(NodeId.CLOUD1, new NodeMetrics(22.0, 1250.0, 3.0, 45.0, 16.0, 300, 12.0));
        
        return metrics;
    }
    
    private List<ServiceRequest> generateServiceRequests(int numRequests, int avgTransactionLoad) {
        List<ServiceRequest> requests = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        for (int i = 0; i < numRequests; i++) {
            // Vary transaction loads around the average
            int transactionLoad = Math.max(1, Math.min(200, 
                (int)(avgTransactionLoad + (random.nextGaussian() * avgTransactionLoad * 0.5))));
            
            ServiceRequest request = new ServiceRequest(
                new ServiceId("service-" + i),
                ServiceType.TRANSACTION_COMMIT,
                random.nextDouble() * 15.0 + 5.0, // 5-20% CPU
                random.nextDouble() * 2.0 + 0.5, // 0.5-2.5 GB memory
                transactionLoad,
                random.nextInt(10) + 1 // Priority 1-10
            );
            requests.add(request);
        }
        
        return requests;
    }
    
    private void verifyTransactionCapacityAwareness(Map<NodeId, List<ServiceRequest>> allocations,
                                                  Map<NodeId, NodeMetrics> nodeMetrics) {
        // Calculate total transaction load per node
        Map<NodeId, Integer> totalTransactionLoad = new HashMap<>();
        for (Map.Entry<NodeId, List<ServiceRequest>> entry : allocations.entrySet()) {
            int totalLoad = entry.getValue().stream()
                .mapToInt(ServiceRequest::getTransactionLoad)
                .sum();
            totalTransactionLoad.put(entry.getKey(), totalLoad);
        }
        
        // Verify that higher-capacity nodes get proportionally more transaction load
        List<NodeId> nodesByTransactionCapacity = nodeMetrics.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(
                e2.getValue().getTransactionsPerSec(),
                e1.getValue().getTransactionsPerSec()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Check that distribution follows transaction capacity ordering
        for (int i = 0; i < nodesByTransactionCapacity.size() - 1; i++) {
            NodeId higherCapacityNode = nodesByTransactionCapacity.get(i);
            NodeId lowerCapacityNode = nodesByTransactionCapacity.get(i + 1);
            
            int higherLoad = totalTransactionLoad.getOrDefault(higherCapacityNode, 0);
            int lowerLoad = totalTransactionLoad.getOrDefault(lowerCapacityNode, 0);
            
            NodeMetrics higherMetrics = nodeMetrics.get(higherCapacityNode);
            NodeMetrics lowerMetrics = nodeMetrics.get(lowerCapacityNode);
            
            double capacityRatio = (double) higherMetrics.getTransactionsPerSec() / lowerMetrics.getTransactionsPerSec();
            
            // Only enforce if transaction capacity difference is significant (>20%)
            if (capacityRatio > 1.2) {
                assertTrue(higherLoad >= lowerLoad * 0.6, // Allow 40% variance
                    String.format("Higher transaction capacity node %s (%d tx/sec, load: %d) should generally get more transaction load than %s (%d tx/sec, load: %d)",
                        higherCapacityNode, higherMetrics.getTransactionsPerSec(), higherLoad,
                        lowerCapacityNode, lowerMetrics.getTransactionsPerSec(), lowerLoad));
            }
        }
    }
    
    private void verifyNoTransactionBottlenecks(Map<NodeId, List<ServiceRequest>> allocations,
                                              Map<NodeId, NodeMetrics> nodeMetrics) {
        for (Map.Entry<NodeId, List<ServiceRequest>> entry : allocations.entrySet()) {
            NodeId nodeId = entry.getKey();
            List<ServiceRequest> nodeRequests = entry.getValue();
            NodeMetrics metrics = nodeMetrics.get(nodeId);
            
            int totalTransactionLoad = nodeRequests.stream()
                .mapToInt(ServiceRequest::getTransactionLoad)
                .sum();
            
            // Verify node is not severely overloaded with transactions
            // Allow some oversubscription but prevent bottlenecks
            assertTrue(totalTransactionLoad <= metrics.getTransactionsPerSec() * 1.4,
                String.format("Node %s should not have transaction bottleneck. Total transaction load: %d tx/sec, Capacity: %d tx/sec",
                    nodeId, totalTransactionLoad, metrics.getTransactionsPerSec()));
        }
    }
    
    private void verifyLoadSpreadAcrossNodes(Map<NodeId, List<ServiceRequest>> allocations) {
        // Verify that load is spread across multiple nodes to prevent bottlenecks
        int nodesWithLoad = (int) allocations.entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .count();
        
        // At least 60% of available nodes should have some load for good distribution
        int totalNodes = 5; // We have 5 nodes in the system
        assertTrue(nodesWithLoad >= Math.max(1, totalNodes * 0.6),
            String.format("Load should be spread across multiple nodes. Only %d out of %d nodes have load",
                nodesWithLoad, totalNodes));
        
        // Calculate load distribution variance to ensure it's not too skewed
        if (nodesWithLoad > 1) {
            List<Integer> loads = allocations.values().stream()
                .mapToInt(requests -> requests.stream()
                    .mapToInt(ServiceRequest::getTransactionLoad)
                    .sum())
                .boxed()
                .collect(Collectors.toList());
            
            double mean = loads.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            double variance = loads.stream()
                .mapToDouble(load -> Math.pow(load - mean, 2))
                .average()
                .orElse(0.0);
            
            double coefficientOfVariation = mean > 0 ? Math.sqrt(variance) / mean : 0.0;
            
            // Coefficient of variation should not be too high (indicating good distribution)
            assertTrue(coefficientOfVariation <= 2.0,
                String.format("Transaction load distribution should not be too skewed. Coefficient of variation: %.2f",
                    coefficientOfVariation));
        }
    }
}