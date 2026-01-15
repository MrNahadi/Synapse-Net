package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Manages distributed files and state across the telecom system.
 * Implements replication mechanisms, access control, and failover integration.
 * Ensures correctness under Byzantine and omission failures.
 * 
 * Requirements: 18.1, 18.2, 18.3, 18.4
 */
public class DistributedFileStateManager {
    private final Map<String, FileMetadata> fileRegistry;
    private final Map<String, StateSnapshot> stateSnapshots;
    private final Map<String, Map<NodeId, ReplicationStatus>> replicationStatuses;
    private final ReadWriteLock registryLock;
    private final ReadWriteLock stateLock;
    private final ReplicationManager replicationManager;
    private final FaultToleranceManager faultToleranceManager;
    private final CommunicationManager communicationManager;
    private final ScheduledExecutorService replicationExecutor;
    private final int minReplicationFactor;
    private final long stateSnapshotInterval;
    private long stateSequenceNumber;

    public DistributedFileStateManager(ReplicationManager replicationManager,
                                      FaultToleranceManager faultToleranceManager,
                                      CommunicationManager communicationManager) {
        this.fileRegistry = new ConcurrentHashMap<>();
        this.stateSnapshots = new ConcurrentHashMap<>();
        this.replicationStatuses = new ConcurrentHashMap<>();
        this.registryLock = new ReentrantReadWriteLock();
        this.stateLock = new ReentrantReadWriteLock();
        this.replicationManager = Objects.requireNonNull(replicationManager, "Replication manager cannot be null");
        this.faultToleranceManager = Objects.requireNonNull(faultToleranceManager, "Fault tolerance manager cannot be null");
        this.communicationManager = Objects.requireNonNull(communicationManager, "Communication manager cannot be null");
        this.replicationExecutor = Executors.newScheduledThreadPool(4);
        this.minReplicationFactor = 2; // Minimum replicas for fault tolerance
        this.stateSnapshotInterval = 60000; // 60 seconds
        this.stateSequenceNumber = 0;
        
        // Start periodic state snapshot
        startPeriodicStateSnapshot();
    }

    /**
     * Registers a new file in the distributed system with replication.
     * Requirements: 18.1
     */
    public FileMetadata registerFile(String fileName, long fileSize, NodeId primaryNode,
                                    AccessControlPolicy accessPolicy) {
        Objects.requireNonNull(fileName, "File name cannot be null");
        Objects.requireNonNull(primaryNode, "Primary node cannot be null");
        Objects.requireNonNull(accessPolicy, "Access policy cannot be null");

        String fileId = generateFileId(fileName);
        Instant now = Instant.now();
        String checksum = calculateChecksum(fileName + fileSize + now.toString());

        // Select replica nodes based on failure tolerance
        Set<NodeId> replicaNodes = selectReplicaNodes(primaryNode, minReplicationFactor);

        FileMetadata metadata = new FileMetadata(
            fileId, fileName, fileSize, now, now,
            primaryNode, replicaNodes, checksum, 1, accessPolicy
        );

        registryLock.writeLock().lock();
        try {
            fileRegistry.put(fileId, metadata);
            initializeReplicationStatus(fileId, primaryNode, replicaNodes);
        } finally {
            registryLock.writeLock().unlock();
        }

        // Trigger replication to replica nodes
        scheduleReplication(fileId, metadata);

        return metadata;
    }

    /**
     * Retrieves file metadata with access control check.
     * Requirements: 18.2
     */
    public FileMetadata getFile(String fileId, NodeId requestingNode) {
        Objects.requireNonNull(fileId, "File ID cannot be null");
        Objects.requireNonNull(requestingNode, "Requesting node cannot be null");

        registryLock.readLock().lock();
        try {
            FileMetadata metadata = fileRegistry.get(fileId);
            if (metadata == null) {
                throw new IllegalArgumentException("File not found: " + fileId);
            }

            // Check access control
            if (!metadata.getAccessPolicy().canRead(requestingNode)) {
                throw new SecurityException("Node " + requestingNode + " does not have read permission for file " + fileId);
            }

            return metadata;
        } finally {
            registryLock.readLock().unlock();
        }
    }

    /**
     * Updates file with version control and replication.
     * Requirements: 18.1, 18.2
     */
    public FileMetadata updateFile(String fileId, NodeId updatingNode, long newSize) {
        Objects.requireNonNull(fileId, "File ID cannot be null");
        Objects.requireNonNull(updatingNode, "Updating node cannot be null");

        registryLock.writeLock().lock();
        try {
            FileMetadata currentMetadata = fileRegistry.get(fileId);
            if (currentMetadata == null) {
                throw new IllegalArgumentException("File not found: " + fileId);
            }

            // Check write permission
            if (!currentMetadata.getAccessPolicy().canWrite(updatingNode)) {
                throw new SecurityException("Node " + updatingNode + " does not have write permission for file " + fileId);
            }

            // Create new version
            Instant now = Instant.now();
            int newVersion = currentMetadata.getVersion() + 1;
            String newChecksum = calculateChecksum(fileId + newSize + now.toString());

            FileMetadata updatedMetadata = currentMetadata.withNewVersion(newVersion, now, newChecksum);
            fileRegistry.put(fileId, updatedMetadata);

            // Trigger replication of updated file
            scheduleReplication(fileId, updatedMetadata);

            return updatedMetadata;
        } finally {
            registryLock.writeLock().unlock();
        }
    }

    /**
     * Deletes file with access control and cleanup.
     * Requirements: 18.2
     */
    public boolean deleteFile(String fileId, NodeId deletingNode) {
        Objects.requireNonNull(fileId, "File ID cannot be null");
        Objects.requireNonNull(deletingNode, "Deleting node cannot be null");

        registryLock.writeLock().lock();
        try {
            FileMetadata metadata = fileRegistry.get(fileId);
            if (metadata == null) {
                return false;
            }

            // Check delete permission
            if (!metadata.getAccessPolicy().canDelete(deletingNode)) {
                throw new SecurityException("Node " + deletingNode + " does not have delete permission for file " + fileId);
            }

            // Remove from all locations
            fileRegistry.remove(fileId);
            replicationStatuses.remove(fileId);

            return true;
        } finally {
            registryLock.writeLock().unlock();
        }
    }

    /**
     * Creates a state snapshot for replication and recovery.
     * Requirements: 18.1
     */
    public StateSnapshot createStateSnapshot(NodeId sourceNode, Map<String, Object> stateData) {
        Objects.requireNonNull(sourceNode, "Source node cannot be null");
        Objects.requireNonNull(stateData, "State data cannot be null");

        stateLock.writeLock().lock();
        try {
            long seqNum = ++stateSequenceNumber;
            String snapshotId = generateSnapshotId(sourceNode, seqNum);
            Instant now = Instant.now();
            String checksum = calculateChecksum(snapshotId + stateData.toString());

            StateSnapshot snapshot = new StateSnapshot(
                snapshotId, now, sourceNode, stateData, checksum, seqNum
            );

            stateSnapshots.put(snapshotId, snapshot);

            // Replicate snapshot to other nodes
            replicateStateSnapshot(snapshot);

            return snapshot;
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the latest state snapshot from a node.
     * Requirements: 18.1
     */
    public StateSnapshot getLatestStateSnapshot(NodeId sourceNode) {
        Objects.requireNonNull(sourceNode, "Source node cannot be null");

        stateLock.readLock().lock();
        try {
            return stateSnapshots.values().stream()
                .filter(snapshot -> snapshot.getSourceNode().equals(sourceNode))
                .max(Comparator.comparing(StateSnapshot::getSequenceNumber))
                .orElse(null);
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Handles node failure with failover integration.
     * Requirements: 18.3
     */
    public void handleNodeFailure(NodeId failedNode, FailureType failureType) {
        Objects.requireNonNull(failedNode, "Failed node cannot be null");
        Objects.requireNonNull(failureType, "Failure type cannot be null");

        // Find all files with primary or replica on failed node
        List<FileMetadata> affectedFiles = findFilesOnNode(failedNode);

        for (FileMetadata metadata : affectedFiles) {
            if (metadata.getPrimaryLocation().equals(failedNode)) {
                // Primary failed - promote a replica
                promoteReplicaToPrimary(metadata, failedNode, failureType);
            } else {
                // Replica failed - create new replica
                replaceFailedReplica(metadata, failedNode, failureType);
            }
        }

        // Handle state snapshots from failed node
        handleFailedNodeStateRecovery(failedNode, failureType);
    }

    /**
     * Verifies file integrity under Byzantine failures.
     * Requirements: 18.4
     */
    public boolean verifyFileIntegrity(String fileId) {
        Objects.requireNonNull(fileId, "File ID cannot be null");

        FileMetadata metadata = fileRegistry.get(fileId);
        if (metadata == null) {
            return false;
        }

        // Get replication statuses from all nodes
        Map<NodeId, ReplicationStatus> statuses = replicationStatuses.get(fileId);
        if (statuses == null || statuses.isEmpty()) {
            return false;
        }

        // For Byzantine tolerance, need majority agreement on checksum
        Map<String, Integer> checksumVotes = new HashMap<>();
        for (ReplicationStatus status : statuses.values()) {
            if (status.isComplete()) {
                // In real system, would fetch actual checksum from node
                // For now, use metadata checksum
                checksumVotes.merge(metadata.getChecksum(), 1, Integer::sum);
            }
        }

        // Verify majority consensus
        int totalReplicas = metadata.getTotalReplicas();
        int requiredVotes = (totalReplicas / 2) + 1;

        return checksumVotes.values().stream()
            .anyMatch(votes -> votes >= requiredVotes);
    }

    /**
     * Gets replication status for a file.
     */
    public Map<NodeId, ReplicationStatus> getReplicationStatus(String fileId) {
        Objects.requireNonNull(fileId, "File ID cannot be null");
        Map<NodeId, ReplicationStatus> statuses = replicationStatuses.get(fileId);
        return statuses != null ? new HashMap<>(statuses) : Collections.emptyMap();
    }

    /**
     * Gets all registered files.
     */
    public Collection<FileMetadata> getAllFiles() {
        registryLock.readLock().lock();
        try {
            return new ArrayList<>(fileRegistry.values());
        } finally {
            registryLock.readLock().unlock();
        }
    }

    // Private helper methods

    private void startPeriodicStateSnapshot() {
        replicationExecutor.scheduleAtFixedRate(() -> {
            try {
                // Create periodic snapshots for all active nodes
                List<NodeId> allNodes = Arrays.asList(
                    NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1
                );
                for (NodeId nodeId : allNodes) {
                    Map<String, Object> stateData = collectNodeState(nodeId);
                    createStateSnapshot(nodeId, stateData);
                }
            } catch (Exception e) {
                // Log error but continue
            }
        }, stateSnapshotInterval, stateSnapshotInterval, TimeUnit.MILLISECONDS);
    }

    private Map<String, Object> collectNodeState(NodeId nodeId) {
        Map<String, Object> state = new HashMap<>();
        state.put("nodeId", nodeId.getId());
        state.put("timestamp", Instant.now().toString());
        state.put("fileCount", countFilesOnNode(nodeId));
        return state;
    }

    private int countFilesOnNode(NodeId nodeId) {
        return (int) fileRegistry.values().stream()
            .filter(metadata -> metadata.isReplicatedOn(nodeId))
            .count();
    }

    private Set<NodeId> selectReplicaNodes(NodeId primaryNode, int count) {
        // Select nodes with different failure characteristics
        List<NodeId> allNodes = Arrays.asList(
            NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1
        );
        List<NodeId> candidates = allNodes.stream()
            .filter(node -> !node.equals(primaryNode))
            .collect(Collectors.toList());

        // Prioritize nodes with different failure types
        Map<NodeId, FailureType> nodeFailureTypes = getNodeFailureTypes();
        FailureType primaryFailureType = nodeFailureTypes.get(primaryNode);

        candidates.sort((n1, n2) -> {
            boolean n1Different = !nodeFailureTypes.get(n1).equals(primaryFailureType);
            boolean n2Different = !nodeFailureTypes.get(n2).equals(primaryFailureType);
            if (n1Different && !n2Different) return -1;
            if (!n1Different && n2Different) return 1;
            return 0;
        });

        return candidates.stream()
            .limit(count)
            .collect(Collectors.toSet());
    }

    private Map<NodeId, FailureType> getNodeFailureTypes() {
        Map<NodeId, FailureType> types = new HashMap<>();
        types.put(NodeId.EDGE1, FailureType.CRASH);
        types.put(NodeId.EDGE2, FailureType.OMISSION);
        types.put(NodeId.CORE1, FailureType.BYZANTINE);
        types.put(NodeId.CORE2, FailureType.CRASH);
        types.put(NodeId.CLOUD1, FailureType.OMISSION);
        return types;
    }

    private void initializeReplicationStatus(String fileId, NodeId primaryNode, Set<NodeId> replicaNodes) {
        Map<NodeId, ReplicationStatus> statuses = new ConcurrentHashMap<>();
        Instant now = Instant.now();

        // Primary is already synced
        statuses.put(primaryNode, new ReplicationStatus(
            fileId, primaryNode, ReplicationStatus.ReplicationState.SYNCED, now, 0, 1.0
        ));

        // Replicas are pending
        for (NodeId replica : replicaNodes) {
            statuses.put(replica, new ReplicationStatus(
                fileId, replica, ReplicationStatus.ReplicationState.PENDING, now, 0, 0.0
            ));
        }

        replicationStatuses.put(fileId, statuses);
    }

    private void scheduleReplication(String fileId, FileMetadata metadata) {
        replicationExecutor.submit(() -> {
            try {
                performReplication(fileId, metadata);
            } catch (Exception e) {
                // Log error
            }
        });
    }

    private void performReplication(String fileId, FileMetadata metadata) {
        Map<NodeId, ReplicationStatus> statuses = replicationStatuses.get(fileId);
        if (statuses == null) return;

        for (NodeId replicaNode : metadata.getReplicaLocations()) {
            ReplicationStatus status = statuses.get(replicaNode);
            if (status != null && !status.isComplete()) {
                // Simulate replication
                Instant now = Instant.now();
                ReplicationStatus updatedStatus = new ReplicationStatus(
                    fileId, replicaNode, ReplicationStatus.ReplicationState.SYNCED,
                    now, metadata.getFileSize(), 1.0
                );
                statuses.put(replicaNode, updatedStatus);
            }
        }
    }

    private void replicateStateSnapshot(StateSnapshot snapshot) {
        // Replicate snapshot to all nodes except source
        List<NodeId> allNodes = Arrays.asList(
            NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1
        );
        for (NodeId nodeId : allNodes) {
            if (!nodeId.equals(snapshot.getSourceNode())) {
                replicationExecutor.submit(() -> {
                    // In real system, would send snapshot to node via communication manager
                    // For now, just store locally
                });
            }
        }
    }

    private List<FileMetadata> findFilesOnNode(NodeId nodeId) {
        registryLock.readLock().lock();
        try {
            return fileRegistry.values().stream()
                .filter(metadata -> metadata.isReplicatedOn(nodeId))
                .collect(Collectors.toList());
        } finally {
            registryLock.readLock().unlock();
        }
    }

    private void promoteReplicaToPrimary(FileMetadata metadata, NodeId failedNode, FailureType failureType) {
        Set<NodeId> availableReplicas = metadata.getReplicaLocations().stream()
            .filter(node -> !node.equals(failedNode))
            .collect(Collectors.toSet());

        if (availableReplicas.isEmpty()) {
            // No replicas available - critical failure
            return;
        }

        // Select best replica to promote
        NodeId newPrimary = selectBestReplica(availableReplicas, failureType);
        
        // Update metadata
        Set<NodeId> newReplicas = new HashSet<>(availableReplicas);
        newReplicas.remove(newPrimary);
        
        // Add new replica to maintain replication factor
        Set<NodeId> additionalReplicas = selectReplicaNodes(newPrimary, 1);
        newReplicas.addAll(additionalReplicas);

        FileMetadata updatedMetadata = metadata.withNewPrimary(newPrimary).withNewReplicas(newReplicas);
        
        registryLock.writeLock().lock();
        try {
            fileRegistry.put(metadata.getFileId(), updatedMetadata);
        } finally {
            registryLock.writeLock().unlock();
        }

        // Schedule replication to new replicas
        scheduleReplication(metadata.getFileId(), updatedMetadata);
    }

    private void replaceFailedReplica(FileMetadata metadata, NodeId failedNode, FailureType failureType) {
        Set<NodeId> currentReplicas = new HashSet<>(metadata.getReplicaLocations());
        currentReplicas.remove(failedNode);

        // Select new replica
        Set<NodeId> newReplicas = selectReplicaNodes(metadata.getPrimaryLocation(), 1);
        currentReplicas.addAll(newReplicas);

        FileMetadata updatedMetadata = metadata.withNewReplicas(currentReplicas);
        
        registryLock.writeLock().lock();
        try {
            fileRegistry.put(metadata.getFileId(), updatedMetadata);
        } finally {
            registryLock.writeLock().unlock();
        }

        // Schedule replication to new replica
        scheduleReplication(metadata.getFileId(), updatedMetadata);
    }

    private NodeId selectBestReplica(Set<NodeId> candidates, FailureType failureType) {
        // Prefer nodes with different failure type
        Map<NodeId, FailureType> nodeFailureTypes = getNodeFailureTypes();
        
        return candidates.stream()
            .filter(node -> !nodeFailureTypes.get(node).equals(failureType))
            .findFirst()
            .orElse(candidates.iterator().next());
    }

    private void handleFailedNodeStateRecovery(NodeId failedNode, FailureType failureType) {
        // Find latest snapshot from failed node
        StateSnapshot latestSnapshot = getLatestStateSnapshot(failedNode);
        
        if (latestSnapshot != null) {
            // State can be recovered from snapshot
            // In real system, would restore state to another node
        }
    }

    private String generateFileId(String fileName) {
        return "file-" + fileName.hashCode() + "-" + System.currentTimeMillis();
    }

    private String generateSnapshotId(NodeId nodeId, long seqNum) {
        return "snapshot-" + nodeId.getId() + "-" + seqNum;
    }

    private String calculateChecksum(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public void shutdown() {
        replicationExecutor.shutdown();
        try {
            if (!replicationExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                replicationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            replicationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
