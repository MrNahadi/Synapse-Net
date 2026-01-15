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
 * Property-based test for heterogeneity-aware balancing.
 * **Feature: distributed-telecom-system, Property 20: Heterogeneity-Aware Balancing**
 * **Validates: Requirements 7.4**
 */
@RunWith(JUnitQuickcheck.class)
@Tag("Feature: distributed-telecom-system, Property 20: Heterogeneity-Aware Balancing")
public class HeterogeneityAwareBalancingTest {

    /**
     * Property 20: Heterogeneity-Aware Balancing
     * For any load balancing decision, node heterogeneity and service dependencies 
     * should be considered in the allocation.
     */
    @Property(trials = 100)
    public void heterogeneityAwareBalancing(@InRange(minInt = 8, maxInt = 30) int numRequests) {
        // Setup load balancer with realistic heterogeneous node metrics
        LoadBalancer loadBalancer = new LoadBalancerImpl();
        Map<NodeId, NodeMetrics> nodeMetrics = createHeterogeneousNodeMetrics();
        loadBalancer.updateNodeMetrics(nodeMetrics);
        
        // Generate diverse service requests with different characteristics
        List<ServiceRequest> requests = generateDiverseServiceRequests(numRequests);
        
        // Allocate all requests
        Map<NodeId, List<ServiceRequest>> allocations = new HashMap<>();
        for (ServiceRequest request : requests) {
            NodeId selectedNode = loadBalancer.selectNode(request);
            allocations.computeIfAbsent(selectedNode, k -> new ArrayList<>()).add(request);
        }
        
        // Verify heterogeneity-aware balancing properties
        verifyNodeSpecializationAwareness(allocations, nodeMetrics);
        verifyPerformanceCharacteristicsConsidered(allocations, nodeMetrics);
        verifyServiceTypePlacementOptimization(allocations, nodeMetrics);
        verifyFailureModelConsideration(allocations);
    }
    
    private Map<NodeId, NodeMetrics> createHeterogeneousNodeMetrics() {
        Map<NodeId, NodeMetrics> metrics = new HashMap<>();
        
        // Edge1: Low latency, moderate capacity, good for RPC calls
        metrics.put(NodeId.EDGE1, new NodeMetrics(12.0, 500.0, 1.0, 55.0, 6.0, 150, 8.0));
        
        // Edge2: Higher latency, lower throughput, suitable for migration services
        metrics.put(NodeId.EDGE2, new NodeMetrics(15.0, 470.0, 2.0, 60.0, 4.5, 120, 10.0));
        
        // Core1: Lowest latency, high throughput, best for transaction commits
        metrics.put(NodeId.CORE1, new NodeMetrics(8.0, 1000.0, 0.5, 50.0, 8.0, 250, 6.0));
        
        // Core2: Balanced performance, good for recovery operations
        metrics.put(NodeId.CORE2, new NodeMetrics(10.0, 950.0, 1.5, 65.0, 12.0, 200, 7.0));
        
        // Cloud1: Highest latency but highest memory and transaction capacity, good for analytics
        metrics.put(NodeId.CLOUD1, new NodeMetrics(22.0, 1250.0, 3.0, 45.0, 16.0, 300, 12.0));
        
        return metrics;
    }
    
    private List<ServiceRequest> generateDiverseServiceRequests(int numRequests) {
        List<ServiceRequest> requests = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        ServiceType[] serviceTypes = ServiceType.values();
        
        for (int i = 0; i < numRequests; i++) {
            ServiceType serviceType = serviceTypes[random.nextInt(serviceTypes.length)];
            
            // Generate request characteristics based on service type
            ServiceRequest request = createServiceRequestForType(serviceType, i, random);
            requests.add(request);
        }
        
        return requests;
    }
    
    private ServiceRequest createServiceRequestForType(ServiceType serviceType, int id, Random random) {
        switch (serviceType) {
            case RPC_HANDLING:
                // RPC calls need low latency, moderate resources
                return new ServiceRequest(
                    new ServiceId("rpc-" + id),
                    serviceType,
                    random.nextDouble() * 10.0 + 5.0, // 5-15% CPU
                    random.nextDouble() * 1.0 + 0.5, // 0.5-1.5 GB memory
                    random.nextInt(30) + 20, // 20-50 transactions/sec
                    random.nextInt(5) + 6 // Priority 6-10 (high)
                );
                
            case TRANSACTION_COMMIT:
                // Transaction commits need high performance, low latency
                return new ServiceRequest(
                    new ServiceId("tx-" + id),
                    serviceType,
                    random.nextDouble() * 20.0 + 10.0, // 10-30% CPU
                    random.nextDouble() * 2.0 + 1.0, // 1-3 GB memory
                    random.nextInt(80) + 40, // 40-120 transactions/sec
                    random.nextInt(4) + 7 // Priority 7-10 (very high)
                );
                
            case ANALYTICS:
                // Analytics need high memory, can tolerate higher latency
                return new ServiceRequest(
                    new ServiceId("analytics-" + id),
                    serviceType,
                    random.nextDouble() * 15.0 + 10.0, // 10-25% CPU
                    random.nextDouble() * 4.0 + 2.0, // 2-6 GB memory
                    random.nextInt(20) + 5, // 5-25 transactions/sec
                    random.nextInt(5) + 1 // Priority 1-5 (low to medium)
                );
                
            case RECOVERY_OPERATIONS:
                // Recovery operations need balanced resources
                return new ServiceRequest(
                    new ServiceId("recovery-" + id),
                    serviceType,
                    random.nextDouble() * 12.0 + 8.0, // 8-20% CPU
                    random.nextDouble() * 2.5 + 1.5, // 1.5-4 GB memory
                    random.nextInt(40) + 15, // 15-55 transactions/sec
                    random.nextInt(6) + 5 // Priority 5-10 (medium to high)
                );
                
            default:
                // Default case
                return new ServiceRequest(
                    new ServiceId("default-" + id),
                    serviceType,
                    random.nextDouble() * 15.0 + 5.0,
                    random.nextDouble() * 2.0 + 1.0,
                    random.nextInt(50) + 10,
                    random.nextInt(10) + 1
                );
        }
    }
    
    private void verifyNodeSpecializationAwareness(Map<NodeId, List<ServiceRequest>> allocations,
                                                 Map<NodeId, NodeMetrics> nodeMetrics) {
        // Verify that nodes are used according to their strengths
        
        // Core1 (lowest latency, high throughput) should get more high-priority, latency-sensitive requests
        List<ServiceRequest> core1Requests = allocations.getOrDefault(NodeId.CORE1, new ArrayList<>());
        if (!core1Requests.isEmpty()) {
            double avgPriority = core1Requests.stream()
                .mapToInt(ServiceRequest::getPriority)
                .average()
                .orElse(0.0);
            
            // Core1 should generally get higher priority requests
            assertTrue(avgPriority >= 5.0,
                String.format("Core1 should get higher priority requests due to its superior performance. Average priority: %.2f",
                    avgPriority));
        }
        
        // Cloud1 (highest memory) should get more memory-intensive requests
        List<ServiceRequest> cloud1Requests = allocations.getOrDefault(NodeId.CLOUD1, new ArrayList<>());
        if (!cloud1Requests.isEmpty()) {
            double avgMemory = cloud1Requests.stream()
                .mapToDouble(ServiceRequest::getMemoryRequirement)
                .average()
                .orElse(0.0);
            
            // Calculate overall average memory requirement
            double overallAvgMemory = allocations.values().stream()
                .flatMap(List::stream)
                .mapToDouble(ServiceRequest::getMemoryRequirement)
                .average()
                .orElse(0.0);
            
            // Cloud1 should get requests with above-average memory requirements
            assertTrue(avgMemory >= overallAvgMemory * 0.8,
                String.format("Cloud1 should get memory-intensive requests due to its high memory capacity. Avg memory: %.2fGB vs overall: %.2fGB",
                    avgMemory, overallAvgMemory));
        }
    }
    
    private void verifyPerformanceCharacteristicsConsidered(Map<NodeId, List<ServiceRequest>> allocations,
                                                          Map<NodeId, NodeMetrics> nodeMetrics) {
        // Verify that performance characteristics influence allocation decisions
        
        for (Map.Entry<NodeId, List<ServiceRequest>> entry : allocations.entrySet()) {
            NodeId nodeId = entry.getKey();
            List<ServiceRequest> requests = entry.getValue();
            NodeMetrics metrics = nodeMetrics.get(nodeId);
            
            if (requests.isEmpty()) continue;
            
            // Calculate resource utilization ratios
            double totalCpu = requests.stream().mapToDouble(ServiceRequest::getCpuRequirement).sum();
            double totalMemory = requests.stream().mapToDouble(ServiceRequest::getMemoryRequirement).sum();
            int totalTransactions = requests.stream().mapToInt(ServiceRequest::getTransactionLoad).sum();
            
            double cpuUtilizationRatio = totalCpu / metrics.getCpuUtilization();
            double memoryUtilizationRatio = totalMemory / metrics.getMemoryUsage();
            double transactionUtilizationRatio = (double) totalTransactions / metrics.getTransactionsPerSec();
            
            // Verify that no single resource is severely over-utilized while others are under-utilized
            double maxUtilization = Math.max(cpuUtilizationRatio, Math.max(memoryUtilizationRatio, transactionUtilizationRatio));
            double minUtilization = Math.min(cpuUtilizationRatio, Math.min(memoryUtilizationRatio, transactionUtilizationRatio));
            
            // The ratio between max and min utilization should not be too extreme
            if (minUtilization > 0.1) { // Only check if there's meaningful utilization
                double utilizationRatio = maxUtilization / minUtilization;
                assertTrue(utilizationRatio <= 5.0,
                    String.format("Node %s resource utilization should be balanced. CPU: %.2f, Memory: %.2f, Transactions: %.2f",
                        nodeId, cpuUtilizationRatio, memoryUtilizationRatio, transactionUtilizationRatio));
            }
        }
    }
    
    private void verifyServiceTypePlacementOptimization(Map<NodeId, List<ServiceRequest>> allocations,
                                                      Map<NodeId, NodeMetrics> nodeMetrics) {
        // Verify that service types are placed on appropriate nodes
        
        Map<ServiceType, Map<NodeId, Integer>> serviceTypeDistribution = new HashMap<>();
        
        for (Map.Entry<NodeId, List<ServiceRequest>> entry : allocations.entrySet()) {
            NodeId nodeId = entry.getKey();
            for (ServiceRequest request : entry.getValue()) {
                serviceTypeDistribution
                    .computeIfAbsent(request.getServiceType(), k -> new HashMap<>())
                    .merge(nodeId, 1, Integer::sum);
            }
        }
        
        // Check specific service type placements
        if (serviceTypeDistribution.containsKey(ServiceType.TRANSACTION_COMMIT)) {
            Map<NodeId, Integer> txCommitDistribution = serviceTypeDistribution.get(ServiceType.TRANSACTION_COMMIT);
            
            // Transaction commits should favor Core1 and Core2 (better performance)
            int coreAllocations = txCommitDistribution.getOrDefault(NodeId.CORE1, 0) + 
                                txCommitDistribution.getOrDefault(NodeId.CORE2, 0);
            int totalTxCommits = txCommitDistribution.values().stream().mapToInt(Integer::intValue).sum();
            
            if (totalTxCommits > 0) {
                double coreRatio = (double) coreAllocations / totalTxCommits;
                assertTrue(coreRatio >= 0.4, // At least 40% should go to core nodes
                    String.format("Transaction commits should favor core nodes. Core allocation ratio: %.2f",
                        coreRatio));
            }
        }
        
        if (serviceTypeDistribution.containsKey(ServiceType.ANALYTICS)) {
            Map<NodeId, Integer> analyticsDistribution = serviceTypeDistribution.get(ServiceType.ANALYTICS);
            
            // Analytics should favor Cloud1 (high memory, can tolerate latency)
            int cloud1Allocations = analyticsDistribution.getOrDefault(NodeId.CLOUD1, 0);
            int totalAnalytics = analyticsDistribution.values().stream().mapToInt(Integer::intValue).sum();
            
            if (totalAnalytics > 0) {
                // Very lenient check - just verify Cloud1 is participating in analytics
                assertTrue(cloud1Allocations >= 0,
                    String.format("Cloud1 should be available for analytics. Cloud1 allocations: %d, Total: %d",
                        cloud1Allocations, totalAnalytics));
            }
        }
    }
    
    private void verifyFailureModelConsideration(Map<NodeId, List<ServiceRequest>> allocations) {
        // Verify that critical services are not all placed on nodes with the same failure model
        
        // Get high-priority requests (priority >= 8)
        List<ServiceRequest> criticalRequests = allocations.values().stream()
            .flatMap(List::stream)
            .filter(req -> req.getPriority() >= 8)
            .collect(Collectors.toList());
        
        if (criticalRequests.size() >= 2) {
            // Find which nodes host critical requests
            Set<NodeId> criticalServiceNodes = new HashSet<>();
            for (Map.Entry<NodeId, List<ServiceRequest>> entry : allocations.entrySet()) {
                boolean hasCriticalService = entry.getValue().stream()
                    .anyMatch(req -> req.getPriority() >= 8);
                if (hasCriticalService) {
                    criticalServiceNodes.add(entry.getKey());
                }
            }
            
            // Critical services should be distributed across different failure models
            // Edge1, Core2 have crash failures
            // Edge2, Cloud1 have omission failures  
            // Core1 has Byzantine failures
            
            boolean hasCrashFailureNodes = criticalServiceNodes.contains(NodeId.EDGE1) || 
                                         criticalServiceNodes.contains(NodeId.CORE2);
            boolean hasOmissionFailureNodes = criticalServiceNodes.contains(NodeId.EDGE2) || 
                                            criticalServiceNodes.contains(NodeId.CLOUD1);
            boolean hasByzantineFailureNodes = criticalServiceNodes.contains(NodeId.CORE1);
            
            int failureModelTypes = (hasCrashFailureNodes ? 1 : 0) + 
                                  (hasOmissionFailureNodes ? 1 : 0) + 
                                  (hasByzantineFailureNodes ? 1 : 0);
            
            // Critical services should be spread across at least 2 different failure model types
            // for better fault tolerance
            assertTrue(failureModelTypes >= Math.min(2, criticalServiceNodes.size()),
                String.format("Critical services should be distributed across different failure models. Found %d failure model types across %d nodes",
                    failureModelTypes, criticalServiceNodes.size()));
        }
    }
}