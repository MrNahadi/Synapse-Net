package com.telecom.distributed.core.factory;

import com.telecom.distributed.core.model.*;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Factory for creating predefined nodes with dataset characteristics.
 * Creates the five nodes: Edge1, Edge2, Core1, Core2, Cloud1 with their specific properties.
 */
public class NodeFactory {

    /**
     * Creates Edge1 node with dataset characteristics.
     * - Latency: 12ms, Throughput: 500Mbps, CPU: 45%, Memory: 4.0GB
     * - Crash failure mode, RPC handling and replication services
     */
    public static NodeConfiguration createEdge1() {
        NodeMetrics metrics = new NodeMetrics(12.0, 500.0, 1.0, 45.0, 4.0, 150, 8.0);
        
        Set<ServiceType> services = Set.of(
            ServiceType.RPC_HANDLING,
            ServiceType.DATA_REPLICATION,
            ServiceType.EVENT_ORDERING
        );

        FailureModel failureModel = new FailureModel(FailureType.CRASH, 0.05, 5000, 10000);
        ResourceLimits resourceLimits = new ResourceLimits(0.72, 4.0, 150, 500.0);
        
        // Edge1 connects to Core1 and Core2
        Set<NodeId> connectedNodes = Set.of(NodeId.CORE1, NodeId.CORE2);
        Map<NodeId, Double> latencies = Map.of(
            NodeId.CORE1, 4.0,  // 12ms - 8ms = 4ms connection latency
            NodeId.CORE2, 2.0   // 12ms - 10ms = 2ms connection latency
        );
        Map<NodeId, Double> bandwidths = Map.of(
            NodeId.CORE1, 500.0,
            NodeId.CORE2, 500.0
        );
        NetworkTopology topology = new NetworkTopology(connectedNodes, latencies, bandwidths);

        return new NodeConfiguration(NodeId.EDGE1, NodeLayer.EDGE, metrics, services, 
                                   failureModel, resourceLimits, topology);
    }

    /**
     * Creates Edge2 node with dataset characteristics.
     * - Latency: 15ms, Throughput: 470Mbps, CPU: 50%, Memory: 4.5GB
     * - Omission failure mode, migration and recovery services
     */
    public static NodeConfiguration createEdge2() {
        NodeMetrics metrics = new NodeMetrics(15.0, 470.0, 1.5, 50.0, 4.5, 120, 9.0);
        
        Set<ServiceType> services = Set.of(
            ServiceType.MIGRATION_SERVICES,
            ServiceType.RECOVERY_OPERATIONS
        );

        FailureModel failureModel = new FailureModel(FailureType.OMISSION, 0.08, 3000, 15000);
        ResourceLimits resourceLimits = new ResourceLimits(0.65, 4.5, 120, 470.0);
        
        // Edge2 connects to Core1 and Core2
        Set<NodeId> connectedNodes = Set.of(NodeId.CORE1, NodeId.CORE2);
        Map<NodeId, Double> latencies = Map.of(
            NodeId.CORE1, 7.0,  // 15ms - 8ms = 7ms connection latency
            NodeId.CORE2, 5.0   // 15ms - 10ms = 5ms connection latency
        );
        Map<NodeId, Double> bandwidths = Map.of(
            NodeId.CORE1, 470.0,
            NodeId.CORE2, 470.0
        );
        NetworkTopology topology = new NetworkTopology(connectedNodes, latencies, bandwidths);

        return new NodeConfiguration(NodeId.EDGE2, NodeLayer.EDGE, metrics, services, 
                                   failureModel, resourceLimits, topology);
    }

    /**
     * Creates Core1 node with dataset characteristics.
     * - Latency: 8ms, Throughput: 1000Mbps, CPU: 60%, Memory: 8.0GB
     * - Byzantine failure mode, transaction commit services
     */
    public static NodeConfiguration createCore1() {
        NodeMetrics metrics = new NodeMetrics(8.0, 1000.0, 0.5, 60.0, 8.0, 250, 12.0);
        
        Set<ServiceType> services = Set.of(
            ServiceType.TRANSACTION_COMMIT,
            ServiceType.RPC_HANDLING
        );

        FailureModel failureModel = new FailureModel(FailureType.BYZANTINE, 0.03, 30000, 60000);
        ResourceLimits resourceLimits = new ResourceLimits(0.70, 8.0, 250, 1000.0);
        
        // Core1 connects to Edge1, Edge2, Core2, and Cloud1
        Set<NodeId> connectedNodes = Set.of(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE2, NodeId.CLOUD1);
        Map<NodeId, Double> latencies = Map.of(
            NodeId.EDGE1, 4.0,
            NodeId.EDGE2, 7.0,
            NodeId.CORE2, 2.0,   // 10ms - 8ms = 2ms
            NodeId.CLOUD1, 14.0  // 22ms - 8ms = 14ms
        );
        Map<NodeId, Double> bandwidths = Map.of(
            NodeId.EDGE1, 500.0,
            NodeId.EDGE2, 470.0,
            NodeId.CORE2, 950.0,
            NodeId.CLOUD1, 1000.0
        );
        NetworkTopology topology = new NetworkTopology(connectedNodes, latencies, bandwidths);

        return new NodeConfiguration(NodeId.CORE1, NodeLayer.CORE, metrics, services, 
                                   failureModel, resourceLimits, topology);
    }

    /**
     * Creates Core2 node with dataset characteristics.
     * - Latency: 10ms, Throughput: 950Mbps, CPU: 55%, Memory: 6.0GB
     * - Crash failure mode, recovery and load balancing services
     */
    public static NodeConfiguration createCore2() {
        NodeMetrics metrics = new NodeMetrics(10.0, 950.0, 0.8, 55.0, 6.0, 200, 10.0);
        
        Set<ServiceType> services = Set.of(
            ServiceType.RECOVERY_OPERATIONS,
            ServiceType.LOAD_BALANCING,
            ServiceType.DEADLOCK_DETECTION
        );

        FailureModel failureModel = new FailureModel(FailureType.CRASH, 0.06, 5000, 12000);
        ResourceLimits resourceLimits = new ResourceLimits(0.68, 6.0, 200, 950.0);
        
        // Core2 connects to Edge1, Edge2, Core1, and Cloud1
        Set<NodeId> connectedNodes = Set.of(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CLOUD1);
        Map<NodeId, Double> latencies = Map.of(
            NodeId.EDGE1, 2.0,
            NodeId.EDGE2, 5.0,
            NodeId.CORE1, 2.0,
            NodeId.CLOUD1, 12.0  // 22ms - 10ms = 12ms
        );
        Map<NodeId, Double> bandwidths = Map.of(
            NodeId.EDGE1, 500.0,
            NodeId.EDGE2, 470.0,
            NodeId.CORE1, 1000.0,
            NodeId.CLOUD1, 950.0
        );
        NetworkTopology topology = new NetworkTopology(connectedNodes, latencies, bandwidths);

        return new NodeConfiguration(NodeId.CORE2, NodeLayer.CORE, metrics, services, 
                                   failureModel, resourceLimits, topology);
    }

    /**
     * Creates Cloud1 node with dataset characteristics.
     * - Latency: 22ms, Throughput: 1250Mbps, CPU: 72%, Memory: 16.0GB
     * - Omission failure mode, analytics and distributed shared memory services
     */
    public static NodeConfiguration createCloud1() {
        NodeMetrics metrics = new NodeMetrics(22.0, 1250.0, 2.0, 72.0, 16.0, 300, 15.0);
        
        Set<ServiceType> services = Set.of(
            ServiceType.ANALYTICS,
            ServiceType.DISTRIBUTED_SHARED_MEMORY,
            ServiceType.RPC_HANDLING
        );

        FailureModel failureModel = new FailureModel(FailureType.OMISSION, 0.10, 3000, 20000);
        ResourceLimits resourceLimits = new ResourceLimits(0.72, 16.0, 300, 1250.0);
        
        // Cloud1 connects to Core1 and Core2
        Set<NodeId> connectedNodes = Set.of(NodeId.CORE1, NodeId.CORE2);
        Map<NodeId, Double> latencies = Map.of(
            NodeId.CORE1, 14.0,
            NodeId.CORE2, 12.0
        );
        Map<NodeId, Double> bandwidths = Map.of(
            NodeId.CORE1, 1000.0,
            NodeId.CORE2, 950.0
        );
        NetworkTopology topology = new NetworkTopology(connectedNodes, latencies, bandwidths);

        return new NodeConfiguration(NodeId.CLOUD1, NodeLayer.CLOUD, metrics, services, 
                                   failureModel, resourceLimits, topology);
    }

    /**
     * Creates all five predefined nodes.
     */
    public static Set<NodeConfiguration> createAllNodes() {
        Set<NodeConfiguration> nodes = new HashSet<>();
        nodes.add(createEdge1());
        nodes.add(createEdge2());
        nodes.add(createCore1());
        nodes.add(createCore2());
        nodes.add(createCloud1());
        return nodes;
    }
}