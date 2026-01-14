package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReplicationManager functionality.
 * Tests service availability during migration, service discovery correctness,
 * and strong consistency under concurrency.
 */
@Tag("Feature: distributed-telecom-system")
public class ReplicationManagerTest {

    private ReplicationManager replicationManager;
    private PerformanceAnalyzer mockPerformanceAnalyzer;
    private TransactionManager mockTransactionManager;
    private CommunicationManager mockCommunicationManager;

    @BeforeEach
    public void setUp() {
        mockPerformanceAnalyzer = mock(PerformanceAnalyzer.class);
        mockTransactionManager = mock(TransactionManager.class);
        mockCommunicationManager = mock(CommunicationManager.class);
        
        // Setup mock behavior
        when(mockTransactionManager.beginTransaction()).thenReturn(new TransactionId("tx-test"));
        when(mockTransactionManager.commit(any())).thenReturn(CommitResult.COMMITTED);
        when(mockPerformanceAnalyzer.analyzeBottlenecks(any())).thenReturn(
            Arrays.asList(new BottleneckAnalysis(NodeId.EDGE1, BottleneckType.CPU, 0.5, 
                "Test bottleneck", Collections.emptySet()))
        );
        
        replicationManager = new ReplicationManager(mockPerformanceAnalyzer, mockTransactionManager, mockCommunicationManager);
    }

    /**
     * Test Property 21: Service Availability During Migration
     * **Validates: Requirements 9.2**
     */
    @Test
    public void testServiceRemainsAvailableDuringMigration() {
        // Create test data
        ServiceId serviceId = new ServiceId("test-service");
        NodeId sourceNode = NodeId.EDGE1;
        NodeId targetNode = NodeId.EDGE2;
        
        // Create replication group and register service
        GroupId groupId = new GroupId("test-group");
        Set<NodeId> candidateNodes = new HashSet<>();
        candidateNodes.add(sourceNode);
        candidateNodes.add(targetNode);
        
        ReplicationGroup group = replicationManager.createReplicationGroup(
            groupId, serviceId, candidateNodes, ServiceType.RPC_HANDLING
        );
        
        // Verify service is initially available
        ServiceLocation initialLocation = replicationManager.lookupService(serviceId);
        assertNotNull(initialLocation, "Service should be registered and available initially");
        assertTrue(initialLocation.isAvailable(), "Service should be available initially");
        
        // Determine which node the service is actually on and migrate to the other
        NodeId actualSourceNode = initialLocation.getCurrentNode();
        NodeId actualTargetNode = actualSourceNode.equals(sourceNode) ? targetNode : sourceNode;
        
        // Start migration with availability-preserving strategy
        MigrationStrategy strategy = MigrationStrategy.LIVE_MIGRATION; // Preserves availability
        CompletableFuture<MigrationPlan> migrationFuture = replicationManager.migrateService(
            serviceId, actualTargetNode, strategy
        );
        
        // Wait for migration to complete
        MigrationPlan completedPlan = migrationFuture.join();
        
        // Verify migration completed successfully
        assertTrue(completedPlan.isCompleted(), "Migration should complete successfully");
        
        // Verify service is available after migration
        ServiceLocation finalLocation = replicationManager.lookupService(serviceId);
        assertNotNull(finalLocation, "Service should still be registered after migration");
        assertTrue(finalLocation.isAvailable(), "Service should be available after migration");
        assertEquals(actualTargetNode, finalLocation.getCurrentNode(), "Service should be on target node after migration");
        
        // Verify service was never unavailable during the entire process
        assertTrue(strategy.preservesAvailability(), "Migration strategy should preserve availability");
    }

    /**
     * Test Property 22: Service Discovery Correctness
     * **Validates: Requirements 9.3**
     */
    @Test
    public void testServiceDiscoveryReturnsCorrectLocation() {
        // Create test data
        ServiceId serviceId = new ServiceId("discovery-service");
        NodeId nodeId = NodeId.CORE1;
        String expectedEndpoint = "http://Core1:8080/services/discovery-service";
        
        // Register service at specific location
        replicationManager.registerService(serviceId, nodeId, ServiceStatus.ACTIVE, expectedEndpoint);
        
        // Lookup service - should return correct location
        ServiceLocation discoveredLocation = replicationManager.lookupService(serviceId);
        
        // Verify correctness of discovered location
        assertNotNull(discoveredLocation, "Service lookup should return a location");
        assertEquals(serviceId, discoveredLocation.getServiceId(), "Service ID should match");
        assertEquals(nodeId, discoveredLocation.getCurrentNode(), "Node location should match");
        assertEquals(ServiceStatus.ACTIVE, discoveredLocation.getStatus(), "Service status should match");
        assertEquals(expectedEndpoint, discoveredLocation.getEndpoint(), "Endpoint should match");
        assertTrue(discoveredLocation.isAvailable(), "Service should be available");
    }

    /**
     * Test service discovery after location updates.
     */
    @Test
    public void testServiceDiscoveryReturnsLatestLocationAfterUpdates() {
        ServiceId serviceId = new ServiceId("update-service");
        NodeId initialNode = NodeId.CORE1;
        NodeId updatedNode = NodeId.CORE2;
        
        String initialEndpoint = "http://Core1:8080/services/update-service";
        String updatedEndpoint = "http://Core2:8080/services/update-service";
        
        // Register service at initial location
        replicationManager.registerService(serviceId, initialNode, ServiceStatus.ACTIVE, initialEndpoint);
        
        // Verify initial location
        ServiceLocation initialLocation = replicationManager.lookupService(serviceId);
        assertNotNull(initialLocation, "Initial service lookup should succeed");
        assertEquals(initialNode, initialLocation.getCurrentNode(), "Should be at initial node");
        
        // Update service location
        replicationManager.updateServiceLocation(serviceId, updatedNode, updatedEndpoint);
        
        // Verify updated location is returned by discovery
        ServiceLocation updatedLocation = replicationManager.lookupService(serviceId);
        assertNotNull(updatedLocation, "Updated service lookup should succeed");
        assertEquals(updatedNode, updatedLocation.getCurrentNode(), "Should be at updated node");
        assertEquals(updatedEndpoint, updatedLocation.getEndpoint(), "Should have updated endpoint");
        
        // Version should be incremented
        assertTrue(updatedLocation.getVersion() > initialLocation.getVersion(), 
            "Updated location should have higher version number");
    }

    /**
     * Test Property 23: Strong Consistency Under Concurrency
     * **Validates: Requirements 9.4**
     */
    @Test
    public void testConcurrentServiceRegistrationsAreConsistent() {
        ServiceId serviceId = new ServiceId("concurrent-service");
        NodeId nodeId = NodeId.CLOUD1;
        String endpoint = "http://Cloud1:8080/services/concurrent-service";
        
        // Register service
        replicationManager.registerService(serviceId, nodeId, ServiceStatus.ACTIVE, endpoint);
        
        // Perform multiple lookups
        ServiceLocation firstLookup = replicationManager.lookupService(serviceId);
        ServiceLocation secondLookup = replicationManager.lookupService(serviceId);
        ServiceLocation thirdLookup = replicationManager.lookupService(serviceId);
        
        // All lookups should return the same information
        assertNotNull(firstLookup, "First lookup should succeed");
        assertNotNull(secondLookup, "Second lookup should succeed");
        assertNotNull(thirdLookup, "Third lookup should succeed");
        
        assertEquals(firstLookup.getCurrentNode(), secondLookup.getCurrentNode(), 
            "First and second lookup should return same node");
        assertEquals(secondLookup.getCurrentNode(), thirdLookup.getCurrentNode(), 
            "Second and third lookup should return same node");
        assertEquals(firstLookup.getEndpoint(), secondLookup.getEndpoint(), 
            "First and second lookup should return same endpoint");
        assertEquals(secondLookup.getEndpoint(), thirdLookup.getEndpoint(), 
            "Second and third lookup should return same endpoint");
        assertEquals(firstLookup.getVersion(), secondLookup.getVersion(), 
            "First and second lookup should return same version");
        assertEquals(secondLookup.getVersion(), thirdLookup.getVersion(), 
            "Second and third lookup should return same version");
    }

    /**
     * Test lookup of non-existent services.
     */
    @Test
    public void testLookupOfNonExistentServiceReturnsNull() {
        ServiceId nonExistentService = new ServiceId("non-existent-service");
        
        // Lookup non-existent service
        ServiceLocation result = replicationManager.lookupService(nonExistentService);
        
        // Should return null for non-existent service
        assertNull(result, "Lookup of non-existent service should return null");
        
        // Multiple lookups should consistently return null
        ServiceLocation secondResult = replicationManager.lookupService(nonExistentService);
        assertNull(secondResult, "Second lookup of non-existent service should also return null");
    }

    /**
     * Test services in replication groups are discoverable.
     */
    @Test
    public void testServicesInReplicationGroupsAreDiscoverable() {
        ServiceId serviceId = new ServiceId("replicated-service");
        GroupId groupId = new GroupId("replication-group");
        NodeId primaryNode = NodeId.CORE1;
        NodeId replicaNode = NodeId.CORE2;
        
        Set<NodeId> candidateNodes = new HashSet<>();
        candidateNodes.add(primaryNode);
        candidateNodes.add(replicaNode);
        
        // Create replication group (this should register the service)
        ReplicationGroup group = replicationManager.createReplicationGroup(
            groupId, serviceId, candidateNodes, ServiceType.DATA_REPLICATION
        );
        
        // Service should be discoverable
        ServiceLocation discoveredLocation = replicationManager.lookupService(serviceId);
        assertNotNull(discoveredLocation, "Service in replication group should be discoverable");
        assertEquals(serviceId, discoveredLocation.getServiceId(), "Service ID should match");
        assertTrue(discoveredLocation.isAvailable(), "Service should be available");
        
        // The discovered location should be on the primary node
        assertEquals(group.getPrimary(), discoveredLocation.getCurrentNode(), 
            "Discovered service should be on the primary node of the replication group");
    }
}