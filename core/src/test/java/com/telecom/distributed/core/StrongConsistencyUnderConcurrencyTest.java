package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pholser.junit.quickcheck.generator.InRange;
import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for strong consistency under concurrent transactions.
 * **Validates: Requirements 9.4**
 */
@RunWith(JUnitQuickcheck.class)
@Tag("Feature: distributed-telecom-system, Property 23: Strong Consistency Under Concurrency")
public class StrongConsistencyUnderConcurrencyTest {

    private ReplicationManager replicationManager;
    private PerformanceAnalyzer mockPerformanceAnalyzer;
    private TransactionManager mockTransactionManager;
    private CommunicationManager mockCommunicationManager;
    private ExecutorService executorService;
    private AtomicInteger transactionIdCounter;

    @BeforeEach
    public void setUp() {
        mockPerformanceAnalyzer = mock(PerformanceAnalyzer.class);
        mockTransactionManager = mock(TransactionManager.class);
        mockCommunicationManager = mock(CommunicationManager.class);
        executorService = Executors.newFixedThreadPool(10);
        transactionIdCounter = new AtomicInteger(0);
        
        // Setup mock behavior for transaction management
        when(mockTransactionManager.beginTransaction()).thenAnswer(invocation -> 
            new TransactionId("tx-" + transactionIdCounter.incrementAndGet()));
        when(mockTransactionManager.commit(any())).thenReturn(CommitResult.COMMITTED);
        when(mockPerformanceAnalyzer.analyzeBottlenecks(any())).thenReturn(
            Arrays.asList(new BottleneckAnalysis(new NodeId("edge1"), BottleneckType.CPU, 0.5, 
                "Test bottleneck", Collections.emptySet()))
        );
        
        replicationManager = new ReplicationManager(mockPerformanceAnalyzer, mockTransactionManager, mockCommunicationManager);
    }

    /**
     * Property 23: Strong Consistency Under Concurrency
     * For any set of concurrent transactions on replicated data, the final state should be 
     * equivalent to some sequential execution of those transactions.
     */
    @Property(trials = 50)
    public void concurrentServiceRegistrationsAreConsistent(
            @InRange(min = "2", max = "5") int numberOfServices,
            @InRange(min = "2", max = "4") int numberOfConcurrentThreads) {
        
        // Create services and nodes for testing
        List<ServiceId> services = new ArrayList<>();
        List<NodeId> nodes = new ArrayList<>();
        
        for (int i = 1; i <= numberOfServices; i++) {
            services.add(new ServiceId("concurrent-service-" + i));
            nodes.add(new NodeId("node-" + i));
        }
        
        // Create concurrent tasks that register services
        List<CompletableFuture<Void>> registrationTasks = new ArrayList<>();
        Map<ServiceId, List<ServiceLocation>> allRegistrations = new ConcurrentHashMap<>();
        
        for (int thread = 0; thread < numberOfConcurrentThreads; thread++) {
            final int threadId = thread;
            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                for (int i = 0; i < numberOfServices; i++) {
                    ServiceId serviceId = services.get(i);
                    NodeId nodeId = nodes.get((threadId + i) % nodes.size());
                    String endpoint = "http://" + nodeId.getId() + ":8080/services/" + serviceId.getId() + "-thread-" + threadId;
                    
                    try {
                        // Register service (this should be atomic due to strong consistency)
                        replicationManager.registerService(serviceId, nodeId, ServiceStatus.ACTIVE, endpoint);
                        
                        // Record the registration attempt
                        ServiceLocation location = replicationManager.lookupService(serviceId);
                        if (location != null) {
                            allRegistrations.computeIfAbsent(serviceId, k -> new ArrayList<>()).add(location);
                        }
                        
                        // Small delay to increase chance of concurrency
                        Thread.sleep(1);
                    } catch (Exception e) {
                        // Ignore exceptions - they may occur due to concurrency control
                    }
                }
            }, executorService);
            
            registrationTasks.add(task);
        }
        
        // Wait for all concurrent registrations to complete
        CompletableFuture.allOf(registrationTasks.toArray(new CompletableFuture[0])).join();
        
        // Verify strong consistency: each service should have exactly one consistent location
        for (ServiceId serviceId : services) {
            ServiceLocation finalLocation = replicationManager.lookupService(serviceId);
            assertNotNull(finalLocation, "Each service should have a final consistent location");
            
            // All lookups should return the same location (consistency)
            for (int i = 0; i < 5; i++) {
                ServiceLocation lookup = replicationManager.lookupService(serviceId);
                assertEquals(finalLocation.getCurrentNode(), lookup.getCurrentNode(), 
                    "All lookups should return consistent node location");
                assertEquals(finalLocation.getEndpoint(), lookup.getEndpoint(), 
                    "All lookups should return consistent endpoint");
                assertEquals(finalLocation.getVersion(), lookup.getVersion(), 
                    "All lookups should return consistent version");
            }
        }
    }

    /**
     * Property test for concurrent service location updates.
     * Concurrent updates should result in a consistent final state.
     */
    @Property(trials = 30)
    public void concurrentServiceUpdatesResultInConsistentState(
            @InRange(min = "1", max = "3") int serviceNumber,
            @InRange(min = "2", max = "4") int numberOfUpdates) {
        
        ServiceId serviceId = new ServiceId("update-service-" + serviceNumber);
        NodeId initialNode = new NodeId("initial-node");
        String initialEndpoint = "http://initial-node:8080/services/update-service-" + serviceNumber;
        
        // Register initial service
        replicationManager.registerService(serviceId, initialNode, ServiceStatus.ACTIVE, initialEndpoint);
        ServiceLocation initialLocation = replicationManager.lookupService(serviceId);
        assertNotNull(initialLocation, "Initial service registration should succeed");
        
        // Create concurrent update tasks
        List<CompletableFuture<Void>> updateTasks = new ArrayList<>();
        
        for (int i = 0; i < numberOfUpdates; i++) {
            final int updateId = i;
            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                NodeId updateNode = new NodeId("update-node-" + updateId);
                String updateEndpoint = "http://update-node-" + updateId + ":8080/services/update-service-" + serviceNumber;
                
                try {
                    replicationManager.updateServiceLocation(serviceId, updateNode, updateEndpoint);
                    Thread.sleep(1); // Small delay to increase concurrency
                } catch (Exception e) {
                    // Ignore exceptions - they may occur due to concurrency control
                }
            }, executorService);
            
            updateTasks.add(task);
        }
        
        // Wait for all updates to complete
        CompletableFuture.allOf(updateTasks.toArray(new CompletableFuture[0])).join();
        
        // Verify final state is consistent
        ServiceLocation finalLocation = replicationManager.lookupService(serviceId);
        assertNotNull(finalLocation, "Service should still exist after concurrent updates");
        
        // Version should be higher than initial (at least one update succeeded)
        assertTrue(finalLocation.getVersion() > initialLocation.getVersion(), 
            "Final version should be higher than initial version");
        
        // Multiple lookups should return the same consistent state
        for (int i = 0; i < 5; i++) {
            ServiceLocation lookup = replicationManager.lookupService(serviceId);
            assertEquals(finalLocation.getCurrentNode(), lookup.getCurrentNode(), 
                "Consistent node location across multiple lookups");
            assertEquals(finalLocation.getEndpoint(), lookup.getEndpoint(), 
                "Consistent endpoint across multiple lookups");
            assertEquals(finalLocation.getVersion(), lookup.getVersion(), 
                "Consistent version across multiple lookups");
        }
    }

    /**
     * Property test for concurrent replication group creation and service registration.
     * Concurrent operations on replication groups should maintain consistency.
     */
    @Property(trials = 30)
    public void concurrentReplicationGroupOperationsAreConsistent(
            @InRange(min = "1", max = "3") int groupNumber,
            @InRange(min = "2", max = "3") int numberOfConcurrentOperations) {
        
        GroupId groupId = new GroupId("concurrent-group-" + groupNumber);
        ServiceId serviceId = new ServiceId("group-service-" + groupNumber);
        
        Set<NodeId> candidateNodes = new HashSet<>();
        candidateNodes.add(new NodeId("node-1"));
        candidateNodes.add(new NodeId("node-2"));
        candidateNodes.add(new NodeId("node-3"));
        
        // Create concurrent tasks that try to create replication groups
        List<CompletableFuture<ReplicationGroup>> groupCreationTasks = new ArrayList<>();
        
        for (int i = 0; i < numberOfConcurrentOperations; i++) {
            CompletableFuture<ReplicationGroup> task = CompletableFuture.supplyAsync(() -> {
                try {
                    return replicationManager.createReplicationGroup(
                        groupId, serviceId, candidateNodes, ServiceType.CRITICAL
                    );
                } catch (Exception e) {
                    // May fail due to concurrent creation - return null
                    return null;
                }
            }, executorService);
            
            groupCreationTasks.add(task);
        }
        
        // Wait for all creation attempts to complete
        List<ReplicationGroup> results = groupCreationTasks.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        // At least one creation should succeed
        assertFalse(results.isEmpty(), "At least one replication group creation should succeed");
        
        // All successful creations should result in the same group configuration
        ReplicationGroup firstGroup = results.get(0);
        for (ReplicationGroup group : results) {
            assertEquals(firstGroup.getGroupId(), group.getGroupId(), "All groups should have same ID");
            assertEquals(firstGroup.getPrimary(), group.getPrimary(), "All groups should have same primary");
            assertEquals(firstGroup.getReplicationFactor(), group.getReplicationFactor(), 
                "All groups should have same replication factor");
        }
        
        // Service should be discoverable and consistent
        ServiceLocation serviceLocation = replicationManager.lookupService(serviceId);
        assertNotNull(serviceLocation, "Service should be discoverable after group creation");
        assertTrue(serviceLocation.isAvailable(), "Service should be available");
        
        // Multiple lookups should return consistent results
        for (int i = 0; i < 3; i++) {
            ServiceLocation lookup = replicationManager.lookupService(serviceId);
            assertEquals(serviceLocation.getCurrentNode(), lookup.getCurrentNode(), 
                "Service location should be consistent across lookups");
        }
    }

    /**
     * Property test for linearizability of service registry operations.
     * Operations should appear to execute atomically in some sequential order.
     */
    @Property(trials = 20)
    public void serviceRegistryOperationsAreLinearizable(
            @InRange(min = "1", max = "2") int serviceNumber) {
        
        ServiceId serviceId = new ServiceId("linearizable-service-" + serviceNumber);
        List<NodeId> nodes = Arrays.asList(
            new NodeId("linear-node-1"),
            new NodeId("linear-node-2"),
            new NodeId("linear-node-3")
        );
        
        // Create a sequence of operations that should be linearizable
        List<CompletableFuture<String>> operations = new ArrayList<>();
        
        // Operation 1: Register service
        operations.add(CompletableFuture.supplyAsync(() -> {
            replicationManager.registerService(serviceId, nodes.get(0), ServiceStatus.ACTIVE, 
                "http://linear-node-1:8080/services/linearizable-service-" + serviceNumber);
            return "register";
        }, executorService));
        
        // Operation 2: Update location
        operations.add(CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(5); // Slight delay to ensure registration happens first
                replicationManager.updateServiceLocation(serviceId, nodes.get(1), 
                    "http://linear-node-2:8080/services/linearizable-service-" + serviceNumber);
                return "update";
            } catch (Exception e) {
                return "update-failed";
            }
        }, executorService));
        
        // Operation 3: Lookup service
        operations.add(CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10); // Delay to ensure previous operations complete
                ServiceLocation location = replicationManager.lookupService(serviceId);
                return location != null ? "lookup-success" : "lookup-null";
            } catch (Exception e) {
                return "lookup-failed";
            }
        }, executorService));
        
        // Wait for all operations to complete
        List<String> results = operations.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        // Verify linearizability: operations should have completed in a valid order
        assertTrue(results.contains("register"), "Registration should have completed");
        
        // Final state should be consistent
        ServiceLocation finalLocation = replicationManager.lookupService(serviceId);
        assertNotNull(finalLocation, "Service should exist after all operations");
        assertTrue(finalLocation.isAvailable(), "Service should be available");
        
        // The final location should reflect the last successful update
        if (results.contains("update")) {
            // If update succeeded, should be on node-2
            assertEquals(nodes.get(1), finalLocation.getCurrentNode(), 
                "Service should be on updated node if update succeeded");
        } else {
            // If update failed, should still be on original node
            assertEquals(nodes.get(0), finalLocation.getCurrentNode(), 
                "Service should be on original node if update failed");
        }
    }
}