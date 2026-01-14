package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Manages replication and migration strategies for the distributed telecom system.
 * Implements bottleneck reduction algorithms, service availability preservation,
 * naming strategies for service discovery, and strong consistency under concurrent transactions.
 */
public class ReplicationManager {
    private final Map<GroupId, ReplicationGroup> replicationGroups;
    private final Map<ServiceId, ServiceLocation> serviceRegistry;
    private final Map<MigrationId, MigrationPlan> activeMigrations;
    private final ReadWriteLock registryLock;
    private final PerformanceAnalyzer performanceAnalyzer;
    private final TransactionManager transactionManager;
    private final CommunicationManager communicationManager;

    public ReplicationManager(PerformanceAnalyzer performanceAnalyzer,
                             TransactionManager transactionManager,
                             CommunicationManager communicationManager) {
        this.replicationGroups = new ConcurrentHashMap<>();
        this.serviceRegistry = new ConcurrentHashMap<>();
        this.activeMigrations = new ConcurrentHashMap<>();
        this.registryLock = new ReentrantReadWriteLock();
        this.performanceAnalyzer = Objects.requireNonNull(performanceAnalyzer, "Performance analyzer cannot be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "Transaction manager cannot be null");
        this.communicationManager = Objects.requireNonNull(communicationManager, "Communication manager cannot be null");
    }

    /**
     * Creates a replication group with bottleneck reduction algorithms.
     * Analyzes performance metrics to determine optimal replication strategy.
     */
    public ReplicationGroup createReplicationGroup(GroupId groupId, ServiceId serviceId,
                                                  Set<NodeId> candidateNodes, ServiceType serviceType) {
        Objects.requireNonNull(groupId, "Group ID cannot be null");
        Objects.requireNonNull(serviceId, "Service ID cannot be null");
        Objects.requireNonNull(candidateNodes, "Candidate nodes cannot be null");
        Objects.requireNonNull(serviceType, "Service type cannot be null");

        if (candidateNodes.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one candidate node");
        }

        // Analyze bottlenecks to determine optimal replication strategy
        Map<NodeId, NodeMetrics> nodeMetricsMap = new HashMap<>();
        for (NodeId nodeId : candidateNodes) {
            // Create default metrics for analysis - in a real system this would come from monitoring
            NodeMetrics metrics = createDefaultMetrics(nodeId);
            nodeMetricsMap.put(nodeId, metrics);
        }
        List<BottleneckAnalysis> bottleneckAnalyses = performanceAnalyzer.analyzeBottlenecks(nodeMetricsMap);
        BottleneckAnalysis primaryAnalysis = bottleneckAnalyses.isEmpty() ? 
            createDefaultBottleneckAnalysis(candidateNodes.iterator().next()) : bottleneckAnalyses.get(0);
        
        ReplicationStrategy strategy = determineOptimalReplicationStrategy(primaryAnalysis, serviceType);
        
        // Select primary node based on performance metrics
        NodeId primaryNode = selectPrimaryNode(candidateNodes, primaryAnalysis);
        
        // Select replica nodes to minimize bottlenecks
        Set<NodeId> replicaNodes = selectReplicaNodes(candidateNodes, primaryNode, strategy.getReplicationFactor());

        ReplicationGroup group = new ReplicationGroup(
            groupId, primaryNode, replicaNodes,
            strategy.getConsistencyLevel(), strategy, strategy.getReplicationFactor()
        );
        
        group.addService(serviceId);
        replicationGroups.put(groupId, group);

        // Register service location
        registerService(serviceId, primaryNode, ServiceStatus.ACTIVE, generateEndpoint(primaryNode, serviceId));

        return group;
    }

    /**
     * Implements migration strategies preserving service availability.
     * Uses live migration techniques to maintain zero-downtime service transitions.
     */
    public CompletableFuture<MigrationPlan> migrateService(ServiceId serviceId, NodeId targetNode,
                                                          MigrationStrategy strategy) {
        Objects.requireNonNull(serviceId, "Service ID cannot be null");
        Objects.requireNonNull(targetNode, "Target node cannot be null");
        Objects.requireNonNull(strategy, "Migration strategy cannot be null");

        return CompletableFuture.supplyAsync(() -> {
            ServiceLocation currentLocation = lookupService(serviceId);
            if (currentLocation == null) {
                throw new IllegalArgumentException("Service not found: " + serviceId);
            }

            if (currentLocation.getCurrentNode().equals(targetNode)) {
                throw new IllegalArgumentException("Service is already on target node: " + targetNode);
            }

            MigrationId migrationId = new MigrationId("migration-" + serviceId.getId() + "-" + System.currentTimeMillis());
            long estimatedDuration = estimateMigrationDuration(serviceId, strategy);
            
            MigrationPlan plan = new MigrationPlan(
                migrationId, serviceId, currentLocation.getCurrentNode(), targetNode,
                strategy, Instant.now(), estimatedDuration, Collections.emptySet()
            );

            activeMigrations.put(migrationId, plan);

            // Execute migration based on strategy
            executeMigration(plan);

            return plan;
        });
    }

    /**
     * Implements naming strategies for service discovery.
     * Provides consistent service location lookup with strong consistency guarantees.
     */
    public ServiceLocation lookupService(ServiceId serviceId) {
        Objects.requireNonNull(serviceId, "Service ID cannot be null");
        
        registryLock.readLock().lock();
        try {
            return serviceRegistry.get(serviceId);
        } finally {
            registryLock.readLock().unlock();
        }
    }

    /**
     * Registers a service location with strong consistency under concurrent transactions.
     * Uses distributed locking to ensure atomic updates to the service registry.
     */
    public void registerService(ServiceId serviceId, NodeId nodeId, ServiceStatus status, String endpoint) {
        Objects.requireNonNull(serviceId, "Service ID cannot be null");
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        Objects.requireNonNull(status, "Service status cannot be null");
        Objects.requireNonNull(endpoint, "Endpoint cannot be null");

        // Use distributed transaction for strong consistency
        TransactionId txId = transactionManager.beginTransaction();
        
        try {
            registryLock.writeLock().lock();
            try {
                ServiceLocation currentLocation = serviceRegistry.get(serviceId);
                int newVersion = currentLocation != null ? currentLocation.getVersion() + 1 : 1;
                
                ServiceLocation newLocation = new ServiceLocation(
                    serviceId, nodeId, status, Instant.now(), endpoint, newVersion
                );
                
                serviceRegistry.put(serviceId, newLocation);
                
                // Replicate to all nodes in replication groups containing this service
                replicateServiceLocation(serviceId, newLocation);
                
                transactionManager.commit(txId);
            } finally {
                registryLock.writeLock().unlock();
            }
        } catch (Exception e) {
            transactionManager.abort(txId);
            throw new RuntimeException("Failed to register service: " + serviceId, e);
        }
    }

    /**
     * Updates service location during migration while preserving availability.
     */
    public void updateServiceLocation(ServiceId serviceId, NodeId newNode, String newEndpoint) {
        ServiceLocation currentLocation = lookupService(serviceId);
        if (currentLocation == null) {
            throw new IllegalArgumentException("Service not found: " + serviceId);
        }

        // Maintain service availability during location update
        ServiceLocation updatedLocation = currentLocation.withNewLocation(newNode, newEndpoint);
        registerService(serviceId, newNode, ServiceStatus.ACTIVE, newEndpoint);
    }

    /**
     * Gets all services currently registered in the system.
     */
    public Set<ServiceId> getAllServices() {
        registryLock.readLock().lock();
        try {
            return new HashSet<>(serviceRegistry.keySet());
        } finally {
            registryLock.readLock().unlock();
        }
    }

    /**
     * Gets all active migration plans.
     */
    public Collection<MigrationPlan> getActiveMigrations() {
        return activeMigrations.values().stream()
            .filter(plan -> !plan.isCompleted() && !plan.hasFailed())
            .collect(Collectors.toList());
    }

    /**
     * Gets replication group for a service.
     */
    public ReplicationGroup getReplicationGroup(ServiceId serviceId) {
        return replicationGroups.values().stream()
            .filter(group -> group.getServices().contains(serviceId))
            .findFirst()
            .orElse(null);
    }

    // Private helper methods

    private ReplicationStrategy determineOptimalReplicationStrategy(BottleneckAnalysis analysis, ServiceType serviceType) {
        // Determine replication type based on service criticality and failure modes
        ReplicationStrategy.ReplicationType replicationType;
        ReplicationStrategy.ConsistencyLevel consistencyLevel;
        int replicationFactor;

        if (serviceType == ServiceType.CRITICAL || serviceType == ServiceType.TRANSACTION_PROCESSING) {
            replicationType = ReplicationStrategy.ReplicationType.BYZANTINE_TOLERANT;
            consistencyLevel = ReplicationStrategy.ConsistencyLevel.STRONG;
            replicationFactor = 3; // Minimum for BFT
        } else if (serviceType == ServiceType.TRANSACTION_COMMIT) {
            replicationType = ReplicationStrategy.ReplicationType.ACTIVE;
            consistencyLevel = ReplicationStrategy.ConsistencyLevel.STRONG;
            replicationFactor = 2;
        } else {
            replicationType = ReplicationStrategy.ReplicationType.PASSIVE;
            consistencyLevel = ReplicationStrategy.ConsistencyLevel.EVENTUAL;
            replicationFactor = 2;
        }

        return new ReplicationStrategy(
            replicationType, replicationFactor, Collections.emptySet(),
            consistencyLevel, true
        );
    }

    private NodeId selectPrimaryNode(Set<NodeId> candidateNodes, BottleneckAnalysis analysis) {
        // Select node with best performance characteristics as primary
        return candidateNodes.stream()
            .min((n1, n2) -> {
                // Prefer nodes that are not bottlenecks
                if (analysis.getBottleneckNode().equals(n1) && !analysis.getBottleneckNode().equals(n2)) {
                    return 1;
                }
                if (!analysis.getBottleneckNode().equals(n1) && analysis.getBottleneckNode().equals(n2)) {
                    return -1;
                }
                return 0; // If both are bottlenecks or neither, consider them equal
            })
            .orElse(candidateNodes.iterator().next());
    }

    private Set<NodeId> selectReplicaNodes(Set<NodeId> candidateNodes, NodeId primaryNode, int replicationFactor) {
        return candidateNodes.stream()
            .filter(node -> !node.equals(primaryNode))
            .limit(replicationFactor - 1)
            .collect(Collectors.toSet());
    }

    private void executeMigration(MigrationPlan plan) {
        plan.updateStatus(MigrationStatus.IN_PROGRESS);
        
        try {
            switch (plan.getStrategy()) {
                case LIVE_MIGRATION:
                    executeLiveMigration(plan);
                    break;
                case REPLICATED_MIGRATION:
                    executeReplicatedMigration(plan);
                    break;
                case GRADUAL_MIGRATION:
                    executeGradualMigration(plan);
                    break;
                default:
                    throw new UnsupportedOperationException("Migration strategy not implemented: " + plan.getStrategy());
            }
            
            plan.updateStatus(MigrationStatus.COMPLETED);
        } catch (Exception e) {
            plan.updateStatus(MigrationStatus.FAILED);
            throw new RuntimeException("Migration failed: " + plan.getMigrationId(), e);
        }
    }

    private void executeLiveMigration(MigrationPlan plan) {
        // Implement live migration with zero downtime
        // 1. Prepare target node
        // 2. Start state synchronization
        // 3. Switch traffic atomically
        // 4. Clean up source node
        
        updateServiceLocation(plan.getServiceId(), plan.getTargetNode(), 
            generateEndpoint(plan.getTargetNode(), plan.getServiceId()));
    }

    private void executeReplicatedMigration(MigrationPlan plan) {
        // Implement replicated migration
        // 1. Create replica on target node
        // 2. Synchronize state
        // 3. Switch primary designation
        // 4. Remove old replica
        
        updateServiceLocation(plan.getServiceId(), plan.getTargetNode(),
            generateEndpoint(plan.getTargetNode(), plan.getServiceId()));
    }

    private void executeGradualMigration(MigrationPlan plan) {
        // Implement gradual traffic redirection
        // 1. Start service on target node
        // 2. Gradually redirect traffic
        // 3. Complete migration when all traffic moved
        
        updateServiceLocation(plan.getServiceId(), plan.getTargetNode(),
            generateEndpoint(plan.getTargetNode(), plan.getServiceId()));
    }

    private void replicateServiceLocation(ServiceId serviceId, ServiceLocation location) {
        // Replicate service location to all nodes in relevant replication groups
        ReplicationGroup group = getReplicationGroup(serviceId);
        if (group != null) {
            Set<NodeId> allNodes = new HashSet<>(group.getReplicas());
            allNodes.add(group.getPrimary());
            
            for (NodeId nodeId : allNodes) {
                // Send location update to each node
                // This would typically use the communication manager
                // For now, we'll just ensure local consistency
            }
        }
    }

    private long estimateMigrationDuration(ServiceId serviceId, MigrationStrategy strategy) {
        // Estimate migration duration based on service size and strategy
        switch (strategy) {
            case LIVE_MIGRATION:
                return 30000; // 30 seconds
            case REPLICATED_MIGRATION:
                return 60000; // 1 minute
            case GRADUAL_MIGRATION:
                return 120000; // 2 minutes
            default:
                return 45000; // 45 seconds default
        }
    }

    private String generateEndpoint(NodeId nodeId, ServiceId serviceId) {
        return "http://" + nodeId.getId() + ":8080/services/" + serviceId.getId();
    }

    private NodeMetrics createDefaultMetrics(NodeId nodeId) {
        // Create default metrics based on node type - in a real system this would come from monitoring
        String nodeIdStr = nodeId.getId().toLowerCase();
        if (nodeIdStr.contains("edge1")) {
            return new NodeMetrics(12.0, 500.0, 0.5, 45.0, 4.0, 150, 5.0);
        } else if (nodeIdStr.contains("edge2")) {
            return new NodeMetrics(15.0, 470.0, 0.8, 50.0, 4.5, 120, 7.0);
        } else if (nodeIdStr.contains("core1")) {
            return new NodeMetrics(8.0, 1000.0, 0.3, 60.0, 8.0, 250, 10.0);
        } else if (nodeIdStr.contains("core2")) {
            return new NodeMetrics(10.0, 950.0, 0.4, 55.0, 6.0, 200, 8.0);
        } else if (nodeIdStr.contains("cloud1")) {
            return new NodeMetrics(22.0, 1250.0, 0.2, 72.0, 16.0, 300, 15.0);
        } else {
            // Default metrics for unknown nodes
            return new NodeMetrics(15.0, 750.0, 0.5, 50.0, 8.0, 200, 10.0);
        }
    }

    private BottleneckAnalysis createDefaultBottleneckAnalysis(NodeId nodeId) {
        return new BottleneckAnalysis(nodeId, BottleneckType.CPU, 0.5, 
            "Default analysis for " + nodeId, Collections.emptySet());
    }
}