package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DistributedFileStateManager.
 * Tests replication and access control under failure scenarios.
 * 
 * Requirements: 18.1, 18.2, 18.4
 */
class DistributedFileStateManagerTest {

    @Mock
    private ReplicationManager replicationManager;

    @Mock
    private FaultToleranceManager faultToleranceManager;

    @Mock
    private CommunicationManager communicationManager;

    private DistributedFileStateManager fileStateManager;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        fileStateManager = new DistributedFileStateManager(
            replicationManager, faultToleranceManager, communicationManager
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fileStateManager != null) {
            fileStateManager.shutdown();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testRegisterFile_CreatesFileWithReplicas() {
        // Arrange
        String fileName = "test-file.txt";
        long fileSize = 1024L;
        NodeId primaryNode = NodeId.CORE1;
        AccessControlPolicy policy = AccessControlPolicy.createPublicReadPolicy("policy-1");

        // Act
        FileMetadata metadata = fileStateManager.registerFile(fileName, fileSize, primaryNode, policy);

        // Assert
        assertNotNull(metadata);
        assertEquals(fileName, metadata.getFileName());
        assertEquals(fileSize, metadata.getFileSize());
        assertEquals(primaryNode, metadata.getPrimaryLocation());
        assertEquals(1, metadata.getVersion());
        assertTrue(metadata.getReplicaLocations().size() >= 2, "Should have at least 2 replicas");
        assertNotNull(metadata.getChecksum());
    }

    @Test
    void testGetFile_WithValidPermissions_ReturnsFile() {
        // Arrange
        String fileName = "test-file.txt";
        NodeId primaryNode = NodeId.CORE1;
        Set<NodeId> allowedNodes = new HashSet<>(Arrays.asList(NodeId.CORE1, NodeId.EDGE1));
        AccessControlPolicy policy = AccessControlPolicy.createRestrictedPolicy("policy-1", allowedNodes);
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);

        // Act
        FileMetadata retrieved = fileStateManager.getFile(registered.getFileId(), NodeId.CORE1);

        // Assert
        assertNotNull(retrieved);
        assertEquals(registered.getFileId(), retrieved.getFileId());
    }

    @Test
    void testGetFile_WithoutPermissions_ThrowsSecurityException() {
        // Arrange
        String fileName = "restricted-file.txt";
        NodeId primaryNode = NodeId.CORE1;
        Set<NodeId> allowedNodes = new HashSet<>(Collections.singletonList(NodeId.CORE1));
        AccessControlPolicy policy = AccessControlPolicy.createRestrictedPolicy("policy-1", allowedNodes);
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);

        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            fileStateManager.getFile(registered.getFileId(), NodeId.EDGE2);
        });
    }

    @Test
    void testUpdateFile_WithValidPermissions_UpdatesVersion() {
        // Arrange
        String fileName = "test-file.txt";
        NodeId primaryNode = NodeId.CORE1;
        Set<NodeId> allowedNodes = new HashSet<>(Arrays.asList(NodeId.CORE1, NodeId.EDGE1));
        AccessControlPolicy policy = AccessControlPolicy.createRestrictedPolicy("policy-1", allowedNodes);
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);
        int originalVersion = registered.getVersion();

        // Act
        FileMetadata updated = fileStateManager.updateFile(registered.getFileId(), NodeId.CORE1, 2048L);

        // Assert
        assertNotNull(updated);
        assertEquals(originalVersion + 1, updated.getVersion());
        assertNotEquals(registered.getChecksum(), updated.getChecksum());
    }

    @Test
    void testUpdateFile_WithoutPermissions_ThrowsSecurityException() {
        // Arrange
        String fileName = "restricted-file.txt";
        NodeId primaryNode = NodeId.CORE1;
        Set<NodeId> allowedNodes = new HashSet<>(Collections.singletonList(NodeId.CORE1));
        AccessControlPolicy policy = AccessControlPolicy.createRestrictedPolicy("policy-1", allowedNodes);
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);

        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            fileStateManager.updateFile(registered.getFileId(), NodeId.EDGE2, 2048L);
        });
    }

    @Test
    void testDeleteFile_WithValidPermissions_DeletesFile() {
        // Arrange
        String fileName = "test-file.txt";
        NodeId primaryNode = NodeId.CORE1;
        Set<NodeId> allowedNodes = new HashSet<>(Arrays.asList(NodeId.CORE1, NodeId.EDGE1));
        AccessControlPolicy policy = AccessControlPolicy.createRestrictedPolicy("policy-1", allowedNodes);
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);

        // Act
        boolean deleted = fileStateManager.deleteFile(registered.getFileId(), NodeId.CORE1);

        // Assert
        assertTrue(deleted);
        assertThrows(IllegalArgumentException.class, () -> {
            fileStateManager.getFile(registered.getFileId(), NodeId.CORE1);
        });
    }

    @Test
    void testDeleteFile_WithoutPermissions_ThrowsSecurityException() {
        // Arrange
        String fileName = "restricted-file.txt";
        NodeId primaryNode = NodeId.CORE1;
        Set<NodeId> allowedNodes = new HashSet<>(Collections.singletonList(NodeId.CORE1));
        AccessControlPolicy policy = AccessControlPolicy.createRestrictedPolicy("policy-1", allowedNodes);
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);

        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            fileStateManager.deleteFile(registered.getFileId(), NodeId.EDGE2);
        });
    }

    @Test
    void testCreateStateSnapshot_CreatesSnapshotWithSequenceNumber() {
        // Arrange
        NodeId sourceNode = NodeId.CORE1;
        Map<String, Object> stateData = new HashMap<>();
        stateData.put("key1", "value1");
        stateData.put("key2", 42);

        // Act
        StateSnapshot snapshot = fileStateManager.createStateSnapshot(sourceNode, stateData);

        // Assert
        assertNotNull(snapshot);
        assertEquals(sourceNode, snapshot.getSourceNode());
        assertEquals(stateData, snapshot.getStateData());
        assertTrue(snapshot.getSequenceNumber() > 0);
        assertNotNull(snapshot.getChecksum());
    }

    @Test
    void testGetLatestStateSnapshot_ReturnsNewestSnapshot() {
        // Arrange
        NodeId sourceNode = NodeId.CORE1;
        Map<String, Object> stateData1 = new HashMap<>();
        stateData1.put("version", 1);
        Map<String, Object> stateData2 = new HashMap<>();
        stateData2.put("version", 2);

        // Act
        StateSnapshot snapshot1 = fileStateManager.createStateSnapshot(sourceNode, stateData1);
        StateSnapshot snapshot2 = fileStateManager.createStateSnapshot(sourceNode, stateData2);
        StateSnapshot latest = fileStateManager.getLatestStateSnapshot(sourceNode);

        // Assert
        assertNotNull(latest);
        assertEquals(snapshot2.getSnapshotId(), latest.getSnapshotId());
        assertTrue(latest.getSequenceNumber() > snapshot1.getSequenceNumber());
    }

    @Test
    void testHandleNodeFailure_CrashFailure_PromotesReplica() {
        // Arrange
        String fileName = "test-file.txt";
        NodeId primaryNode = NodeId.EDGE1; // Crash failure node
        AccessControlPolicy policy = AccessControlPolicy.createPublicReadPolicy("policy-1");
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);
        String fileId = registered.getFileId();

        // Act
        fileStateManager.handleNodeFailure(primaryNode, FailureType.CRASH);

        // Assert
        FileMetadata updated = fileStateManager.getFile(fileId, NodeId.CORE1);
        assertNotNull(updated);
        assertNotEquals(primaryNode, updated.getPrimaryLocation(), "Primary should have changed");
        assertTrue(updated.getReplicaLocations().size() >= 2, "Should maintain replication factor");
    }

    @Test
    void testHandleNodeFailure_OmissionFailure_ReplacesReplica() {
        // Arrange
        String fileName = "test-file.txt";
        NodeId primaryNode = NodeId.CORE1;
        AccessControlPolicy policy = AccessControlPolicy.createPublicReadPolicy("policy-1");
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);
        String fileId = registered.getFileId();
        
        // Simulate EDGE2 (omission failure) being a replica
        NodeId failedReplica = NodeId.EDGE2;

        // Act
        fileStateManager.handleNodeFailure(failedReplica, FailureType.OMISSION);

        // Assert
        FileMetadata updated = fileStateManager.getFile(fileId, NodeId.CORE1);
        assertNotNull(updated);
        // Should still have replicas
        assertTrue(updated.getTotalReplicas() >= 2);
    }

    @Test
    void testHandleNodeFailure_ByzantineFailure_MaintainsIntegrity() {
        // Arrange
        String fileName = "test-file.txt";
        NodeId primaryNode = NodeId.CORE2;
        AccessControlPolicy policy = AccessControlPolicy.createPublicReadPolicy("policy-1");
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);
        String fileId = registered.getFileId();
        
        // Simulate CORE1 (Byzantine failure) being involved
        NodeId byzantineNode = NodeId.CORE1;

        // Act
        fileStateManager.handleNodeFailure(byzantineNode, FailureType.BYZANTINE);

        // Assert - file should still be accessible
        FileMetadata updated = fileStateManager.getFile(fileId, NodeId.CORE2);
        assertNotNull(updated);
    }

    @Test
    void testVerifyFileIntegrity_WithSufficientReplicas_ReturnsTrue() {
        // Arrange
        String fileName = "test-file.txt";
        NodeId primaryNode = NodeId.CORE1;
        AccessControlPolicy policy = AccessControlPolicy.createPublicReadPolicy("policy-1");
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);
        
        // Wait a bit for replication to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        boolean isValid = fileStateManager.verifyFileIntegrity(registered.getFileId());

        // Assert
        assertTrue(isValid, "File integrity should be verified with sufficient replicas");
    }

    @Test
    void testGetReplicationStatus_ReturnsStatusForAllReplicas() {
        // Arrange
        String fileName = "test-file.txt";
        NodeId primaryNode = NodeId.CORE1;
        AccessControlPolicy policy = AccessControlPolicy.createPublicReadPolicy("policy-1");
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);

        // Act
        Map<NodeId, ReplicationStatus> statuses = fileStateManager.getReplicationStatus(registered.getFileId());

        // Assert
        assertNotNull(statuses);
        assertFalse(statuses.isEmpty());
        assertTrue(statuses.containsKey(primaryNode), "Should include primary node");
        
        // Primary should be synced
        ReplicationStatus primaryStatus = statuses.get(primaryNode);
        assertNotNull(primaryStatus);
        assertTrue(primaryStatus.isComplete());
    }

    @Test
    void testGetAllFiles_ReturnsAllRegisteredFiles() {
        // Arrange
        AccessControlPolicy policy = AccessControlPolicy.createPublicReadPolicy("policy-1");
        fileStateManager.registerFile("file1.txt", 1024L, NodeId.CORE1, policy);
        fileStateManager.registerFile("file2.txt", 2048L, NodeId.CORE2, policy);
        fileStateManager.registerFile("file3.txt", 512L, NodeId.EDGE1, policy);

        // Act
        Collection<FileMetadata> allFiles = fileStateManager.getAllFiles();

        // Assert
        assertNotNull(allFiles);
        assertEquals(3, allFiles.size());
    }

    @Test
    void testReplicationAcrossFailureTypes_SelectsDiverseNodes() {
        // Arrange
        String fileName = "test-file.txt";
        NodeId primaryNode = NodeId.CORE1; // Byzantine failure
        AccessControlPolicy policy = AccessControlPolicy.createPublicReadPolicy("policy-1");

        // Act
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);

        // Assert
        Set<NodeId> replicaNodes = registered.getReplicaLocations();
        assertNotNull(replicaNodes);
        assertTrue(replicaNodes.size() >= 2);
        
        // Should prefer nodes with different failure types
        // CORE1 is Byzantine, so replicas should prefer CRASH or OMISSION nodes
        boolean hasDifferentFailureType = replicaNodes.stream()
            .anyMatch(node -> node == NodeId.EDGE1 || node == NodeId.EDGE2 || 
                            node == NodeId.CORE2 || node == NodeId.CLOUD1);
        assertTrue(hasDifferentFailureType, "Should select replicas with different failure types");
    }

    @Test
    void testConcurrentFileOperations_MaintainsConsistency() throws InterruptedException {
        // Arrange
        String fileName = "concurrent-file.txt";
        NodeId primaryNode = NodeId.CORE1;
        Set<NodeId> allowedNodes = new HashSet<>(Arrays.asList(NodeId.CORE1, NodeId.CORE2, NodeId.EDGE1));
        AccessControlPolicy policy = AccessControlPolicy.createRestrictedPolicy("policy-1", allowedNodes);
        
        FileMetadata registered = fileStateManager.registerFile(fileName, 1024L, primaryNode, policy);
        String fileId = registered.getFileId();

        // Act - Concurrent updates from different nodes
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                fileStateManager.updateFile(fileId, NodeId.CORE1, 1024L + i);
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                fileStateManager.updateFile(fileId, NodeId.CORE2, 2048L + i);
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Assert
        FileMetadata finalMetadata = fileStateManager.getFile(fileId, NodeId.CORE1);
        assertNotNull(finalMetadata);
        assertTrue(finalMetadata.getVersion() > 1, "Version should have incremented");
        assertTrue(finalMetadata.getVersion() <= 21, "Version should not exceed total updates + 1");
    }

    @Test
    void testStateSnapshotSequencing_MaintainsOrder() {
        // Arrange
        NodeId sourceNode = NodeId.CORE1;
        List<StateSnapshot> snapshots = new ArrayList<>();

        // Act - Create multiple snapshots
        for (int i = 0; i < 5; i++) {
            Map<String, Object> stateData = new HashMap<>();
            stateData.put("iteration", i);
            StateSnapshot snapshot = fileStateManager.createStateSnapshot(sourceNode, stateData);
            snapshots.add(snapshot);
        }

        // Assert - Sequence numbers should be increasing
        for (int i = 1; i < snapshots.size(); i++) {
            assertTrue(snapshots.get(i).getSequenceNumber() > snapshots.get(i-1).getSequenceNumber(),
                      "Sequence numbers should be strictly increasing");
        }
    }
}
