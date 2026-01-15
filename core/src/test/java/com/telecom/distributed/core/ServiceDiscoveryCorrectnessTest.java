package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pholser.junit.quickcheck.generator.InRange;
import com.telecom.distributed.core.model.*;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for service discovery correctness.
 * **Validates: Requirements 9.3**
 */
@RunWith(JUnitQuickcheck.class)
public class ServiceDiscoveryCorrectnessTest {

    private ReplicationManager replicationManager;
    private PerformanceAnalyzer mockPerformanceAnalyzer;
    private TransactionManager mockTransactionManager;
    private CommunicationManager mockCommunicationManager;

    @Before
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
    
    // Helper method to get a valid NodeId from a number
    private NodeId getNodeId(int number) {
        NodeId[] nodes = {NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1};
        return nodes[number % 5];
    }

    /**
     * Property 22: Service Discovery Correctness
     * For any service lookup request, the naming service should return the correct current location of the service.
     */
    @Property(trials = 100)
    public void serviceDiscoveryReturnsCorrectLocation(
            @InRange(min = "1", max = "10") int serviceNumber,
            @InRange(min = "1", max = "5") int nodeNumber) {
        
        // Create test data
        ServiceId serviceId = new ServiceId("discovery-service-" + serviceNumber);
        NodeId nodeId = getNodeId(nodeNumber);
        String expectedEndpoint = "http://" + nodeId.getId() + ":8080/services/discovery-service-" + serviceNumber;
        
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
     * Property test for service discovery after location updates.
     * Service discovery should always return the most recent location.
     */
    @Property(trials = 100)
    public void serviceDiscoveryReturnsLatestLocationAfterUpdates(
            @InRange(min = "1", max = "5") int serviceNumber,
            @InRange(min = "1", max = "3") int initialNodeNumber,
            @InRange(min = "1", max = "3") int updatedNodeNumber) {
        
        // Ensure we have different nodes for the update
        if (initialNodeNumber == updatedNodeNumber) {
            updatedNodeNumber = (updatedNodeNumber % 3) + 1;
        }
        
        ServiceId serviceId = new ServiceId("update-service-" + serviceNumber);
        NodeId initialNode = getNodeId(initialNodeNumber);
        NodeId updatedNode = getNodeId(updatedNodeNumber);
        
        String initialEndpoint = "http://" + initialNode.getId() + ":8080/services/update-service-" + serviceNumber;
        String updatedEndpoint = "http://updated-node-" + updatedNodeNumber + ":8080/services/update-service-" + serviceNumber;
        
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
     * Property test for service discovery consistency across multiple lookups.
     * Multiple lookups of the same service should return consistent results.
     */
    @Property(trials = 50)
    public void multipleLookupsReturnConsistentResults(
            @InRange(min = "1", max = "5") int serviceNumber,
            @InRange(min = "1", max = "3") int nodeNumber) {
        
        ServiceId serviceId = new ServiceId("consistent-service-" + serviceNumber);
        NodeId nodeId = getNodeId(nodeNumber);
        String endpoint = "http://" + nodeId.getId() + ":8080/services/consistent-service-" + serviceNumber;
        
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
     * Property test for service discovery of non-existent services.
     * Lookup of non-existent services should return null consistently.
     */
    @Property(trials = 50)
    public void lookupOfNonExistentServiceReturnsNull(
            @InRange(min = "100", max = "999") int nonExistentServiceNumber) {
        
        ServiceId nonExistentService = new ServiceId("non-existent-" + nonExistentServiceNumber);
        
        // Lookup non-existent service
        ServiceLocation result = replicationManager.lookupService(nonExistentService);
        
        // Should return null for non-existent service
        assertNull(result, "Lookup of non-existent service should return null");
        
        // Multiple lookups should consistently return null
        ServiceLocation secondResult = replicationManager.lookupService(nonExistentService);
        assertNull(secondResult, "Second lookup of non-existent service should also return null");
    }

    /**
     * Property test for service discovery after service registration in replication groups.
     * Services registered through replication groups should be discoverable.
     */
    @Property(trials = 50)
    public void servicesInReplicationGroupsAreDiscoverable(
            @InRange(min = "1", max = "5") int groupNumber,
            @InRange(min = "1", max = "3") int primaryNodeNumber) {
        
        ServiceId serviceId = new ServiceId("replicated-service-" + groupNumber);
        GroupId groupId = new GroupId("replication-group-" + groupNumber);
        NodeId primaryNode = getNodeId(primaryNodeNumber);
        NodeId replicaNode = getNodeId((primaryNodeNumber + 1) % 5);
        
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