package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service placement strategy using formal optimization algorithms.
 * Optimizes placement based on latency, throughput, and resource metrics.
 */
public class ServicePlacementStrategy {
    
    /**
     * Optimizes service placement across nodes using multi-objective optimization.
     * @param nodeConfigurations Available node configurations
     * @return Optimal service placement mapping
     */
    public Map<ServiceType, Set<NodeId>> optimizeServicePlacement(
            Map<NodeId, NodeConfiguration> nodeConfigurations) {
        
        Map<ServiceType, Set<NodeId>> placement = new HashMap<>();
        
        // Initialize placement sets
        for (ServiceType service : ServiceType.values()) {
            placement.put(service, new HashSet<>());
        }
        
        // Apply formal optimization criteria
        optimizeLatencySensitiveServices(nodeConfigurations, placement);
        optimizeThroughputIntensiveServices(nodeConfigurations, placement);
        optimizeResourceConstrainedServices(nodeConfigurations, placement);
        optimizeFaultToleranceServices(nodeConfigurations, placement);
        
        return placement;
    }
    
    /**
     * Places latency-sensitive services on nodes with lowest latency.
     */
    private void optimizeLatencySensitiveServices(
            Map<NodeId, NodeConfiguration> nodeConfigurations,
            Map<ServiceType, Set<NodeId>> placement) {
        
        // Sort nodes by latency (ascending)
        List<Map.Entry<NodeId, NodeConfiguration>> sortedByLatency = 
            nodeConfigurations.entrySet().stream()
                .sorted(Comparator.comparing(entry -> 
                    entry.getValue().getBaselineMetrics().getLatency()))
                .collect(Collectors.toList());
        
        // Place RPC handling on lowest latency nodes
        for (Map.Entry<NodeId, NodeConfiguration> entry : sortedByLatency) {
            if (entry.getValue().getSupportedServices().contains(ServiceType.RPC_HANDLING)) {
                placement.get(ServiceType.RPC_HANDLING).add(entry.getKey());
                break; // Place on single best node for latency
            }
        }
        
        // Place event ordering on low-latency edge nodes
        for (Map.Entry<NodeId, NodeConfiguration> entry : sortedByLatency) {
            NodeConfiguration config = entry.getValue();
            if (config.getLayer() == NodeLayer.EDGE && 
                config.getSupportedServices().contains(ServiceType.EVENT_ORDERING)) {
                placement.get(ServiceType.EVENT_ORDERING).add(entry.getKey());
            }
        }
    }
    
    /**
     * Places throughput-intensive services on high-bandwidth nodes.
     */
    private void optimizeThroughputIntensiveServices(
            Map<NodeId, NodeConfiguration> nodeConfigurations,
            Map<ServiceType, Set<NodeId>> placement) {
        
        // Sort nodes by throughput (descending)
        List<Map.Entry<NodeId, NodeConfiguration>> sortedByThroughput = 
            nodeConfigurations.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<NodeId, NodeConfiguration> entry) -> 
                    entry.getValue().getBaselineMetrics().getThroughput()).reversed())
                .collect(Collectors.toList());
        
        // Place analytics on highest throughput nodes
        for (Map.Entry<NodeId, NodeConfiguration> entry : sortedByThroughput) {
            if (entry.getValue().getSupportedServices().contains(ServiceType.ANALYTICS)) {
                placement.get(ServiceType.ANALYTICS).add(entry.getKey());
            }
        }
        
        // Place distributed shared memory on high-throughput cloud nodes
        for (Map.Entry<NodeId, NodeConfiguration> entry : sortedByThroughput) {
            NodeConfiguration config = entry.getValue();
            if (config.getLayer() == NodeLayer.CLOUD && 
                config.getSupportedServices().contains(ServiceType.DISTRIBUTED_SHARED_MEMORY)) {
                placement.get(ServiceType.DISTRIBUTED_SHARED_MEMORY).add(entry.getKey());
            }
        }
    }
    
    /**
     * Places resource-constrained services based on CPU and memory availability.
     */
    private void optimizeResourceConstrainedServices(
            Map<NodeId, NodeConfiguration> nodeConfigurations,
            Map<ServiceType, Set<NodeId>> placement) {
        
        // Sort nodes by available resources (CPU and memory combined score)
        List<Map.Entry<NodeId, NodeConfiguration>> sortedByResources = 
            nodeConfigurations.entrySet().stream()
                .sorted(Comparator.comparing(entry -> {
                    NodeMetrics metrics = entry.getValue().getBaselineMetrics();
                    // Lower CPU utilization and higher memory is better
                    return -(1.0 - metrics.getCpuUtilization()/100.0) * metrics.getMemoryUsage();
                }))
                .collect(Collectors.toList());
        
        // Place transaction commit on nodes with good resource balance
        for (Map.Entry<NodeId, NodeConfiguration> entry : sortedByResources) {
            if (entry.getValue().getSupportedServices().contains(ServiceType.TRANSACTION_COMMIT)) {
                placement.get(ServiceType.TRANSACTION_COMMIT).add(entry.getKey());
            }
        }
        
        // Place load balancing on nodes with moderate resource usage
        for (Map.Entry<NodeId, NodeConfiguration> entry : sortedByResources) {
            if (entry.getValue().getSupportedServices().contains(ServiceType.LOAD_BALANCING)) {
                placement.get(ServiceType.LOAD_BALANCING).add(entry.getKey());
            }
        }
    }
    
    /**
     * Places fault-tolerant services considering failure modes.
     */
    private void optimizeFaultToleranceServices(
            Map<NodeId, NodeConfiguration> nodeConfigurations,
            Map<ServiceType, Set<NodeId>> placement) {
        
        // Group nodes by failure type for diversity
        Map<FailureType, List<NodeId>> nodesByFailureType = new HashMap<>();
        for (Map.Entry<NodeId, NodeConfiguration> entry : nodeConfigurations.entrySet()) {
            FailureType failureType = entry.getValue().getFailureModel().getPrimaryFailureType();
            nodesByFailureType.computeIfAbsent(failureType, k -> new ArrayList<>())
                .add(entry.getKey());
        }
        
        // Place data replication across nodes with different failure modes
        for (Map.Entry<NodeId, NodeConfiguration> entry : nodeConfigurations.entrySet()) {
            if (entry.getValue().getSupportedServices().contains(ServiceType.DATA_REPLICATION)) {
                placement.get(ServiceType.DATA_REPLICATION).add(entry.getKey());
            }
        }
        
        // Place recovery operations on crash-tolerant nodes
        for (Map.Entry<NodeId, NodeConfiguration> entry : nodeConfigurations.entrySet()) {
            NodeConfiguration config = entry.getValue();
            if (config.getSupportedServices().contains(ServiceType.RECOVERY_OPERATIONS) &&
                config.getFailureModel().getPrimaryFailureType() == FailureType.CRASH) {
                placement.get(ServiceType.RECOVERY_OPERATIONS).add(entry.getKey());
            }
        }
        
        // Place migration services on omission-tolerant nodes
        for (Map.Entry<NodeId, NodeConfiguration> entry : nodeConfigurations.entrySet()) {
            NodeConfiguration config = entry.getValue();
            if (config.getSupportedServices().contains(ServiceType.MIGRATION_SERVICES)) {
                placement.get(ServiceType.MIGRATION_SERVICES).add(entry.getKey());
            }
        }
        
        // Place deadlock detection on reliable core nodes
        for (Map.Entry<NodeId, NodeConfiguration> entry : nodeConfigurations.entrySet()) {
            NodeConfiguration config = entry.getValue();
            if (config.getLayer() == NodeLayer.CORE && 
                config.getSupportedServices().contains(ServiceType.DEADLOCK_DETECTION)) {
                placement.get(ServiceType.DEADLOCK_DETECTION).add(entry.getKey());
            }
        }
    }
    
    /**
     * Calculates placement quality score based on optimization criteria.
     * @param placement Service placement to evaluate
     * @param nodeConfigurations Node configurations
     * @return Quality score (higher is better)
     */
    public double calculatePlacementScore(Map<ServiceType, Set<NodeId>> placement,
                                        Map<NodeId, NodeConfiguration> nodeConfigurations) {
        double totalScore = 0.0;
        int serviceCount = 0;
        
        for (Map.Entry<ServiceType, Set<NodeId>> entry : placement.entrySet()) {
            ServiceType service = entry.getKey();
            Set<NodeId> nodes = entry.getValue();
            
            if (!nodes.isEmpty()) {
                double serviceScore = calculateServicePlacementScore(service, nodes, nodeConfigurations);
                totalScore += serviceScore;
                serviceCount++;
            }
        }
        
        return serviceCount > 0 ? totalScore / serviceCount : 0.0;
    }
    
    /**
     * Calculates placement score for a specific service.
     */
    private double calculateServicePlacementScore(ServiceType service, Set<NodeId> nodes,
                                                Map<NodeId, NodeConfiguration> nodeConfigurations) {
        double score = 0.0;
        
        for (NodeId nodeId : nodes) {
            NodeConfiguration config = nodeConfigurations.get(nodeId);
            if (config != null) {
                NodeMetrics metrics = config.getBaselineMetrics();
                
                // Score based on service requirements
                switch (service) {
                    case RPC_HANDLING:
                    case EVENT_ORDERING:
                        // Favor low latency
                        score += 1.0 - ((metrics.getLatency() - 8.0) / (22.0 - 8.0));
                        break;
                    case ANALYTICS:
                    case DISTRIBUTED_SHARED_MEMORY:
                        // Favor high throughput
                        score += (metrics.getThroughput() - 470.0) / (1250.0 - 470.0);
                        break;
                    case TRANSACTION_COMMIT:
                    case LOAD_BALANCING:
                        // Favor balanced resources
                        double cpuScore = 1.0 - ((metrics.getCpuUtilization() - 45.0) / (72.0 - 45.0));
                        double memoryScore = (metrics.getMemoryUsage() - 4.0) / (16.0 - 4.0);
                        score += (cpuScore + memoryScore) / 2.0;
                        break;
                    default:
                        // General performance score
                        score += 0.5;
                        break;
                }
            }
        }
        
        return nodes.isEmpty() ? 0.0 : score / nodes.size();
    }
}