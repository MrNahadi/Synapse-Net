package com.telecom.distributed.core.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Metadata for distributed files in the system.
 * Tracks file location, replication status, and access control.
 */
public class FileMetadata {
    private final String fileId;
    private final String fileName;
    private final long fileSize;
    private final Instant createdAt;
    private final Instant lastModified;
    private final NodeId primaryLocation;
    private final Set<NodeId> replicaLocations;
    private final String checksum;
    private final int version;
    private final AccessControlPolicy accessPolicy;

    public FileMetadata(String fileId, String fileName, long fileSize,
                       Instant createdAt, Instant lastModified,
                       NodeId primaryLocation, Set<NodeId> replicaLocations,
                       String checksum, int version, AccessControlPolicy accessPolicy) {
        this.fileId = Objects.requireNonNull(fileId, "File ID cannot be null");
        this.fileName = Objects.requireNonNull(fileName, "File name cannot be null");
        this.fileSize = validateFileSize(fileSize);
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        this.lastModified = Objects.requireNonNull(lastModified, "Last modified timestamp cannot be null");
        this.primaryLocation = Objects.requireNonNull(primaryLocation, "Primary location cannot be null");
        this.replicaLocations = Objects.requireNonNull(replicaLocations, "Replica locations cannot be null");
        this.checksum = Objects.requireNonNull(checksum, "Checksum cannot be null");
        this.version = validateVersion(version);
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "Access policy cannot be null");
    }

    private long validateFileSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("File size cannot be negative: " + size);
        }
        return size;
    }

    private int validateVersion(int version) {
        if (version < 1) {
            throw new IllegalArgumentException("Version must be at least 1: " + version);
        }
        return version;
    }

    public FileMetadata withNewVersion(int newVersion, Instant modifiedTime, String newChecksum) {
        return new FileMetadata(fileId, fileName, fileSize, createdAt, modifiedTime,
                              primaryLocation, replicaLocations, newChecksum, newVersion, accessPolicy);
    }

    public FileMetadata withNewReplicas(Set<NodeId> newReplicas) {
        return new FileMetadata(fileId, fileName, fileSize, createdAt, lastModified,
                              primaryLocation, newReplicas, checksum, version, accessPolicy);
    }

    public FileMetadata withNewPrimary(NodeId newPrimary) {
        return new FileMetadata(fileId, fileName, fileSize, createdAt, lastModified,
                              newPrimary, replicaLocations, checksum, version, accessPolicy);
    }

    public boolean isReplicatedOn(NodeId nodeId) {
        return primaryLocation.equals(nodeId) || replicaLocations.contains(nodeId);
    }

    public int getTotalReplicas() {
        return 1 + replicaLocations.size(); // primary + replicas
    }

    // Getters
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    public NodeId getPrimaryLocation() { return primaryLocation; }
    public Set<NodeId> getReplicaLocations() { return replicaLocations; }
    public String getChecksum() { return checksum; }
    public int getVersion() { return version; }
    public AccessControlPolicy getAccessPolicy() { return accessPolicy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileMetadata that = (FileMetadata) o;
        return Objects.equals(fileId, that.fileId) && version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, version);
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
               "fileId='" + fileId + '\'' +
               ", fileName='" + fileName + '\'' +
               ", fileSize=" + fileSize +
               ", version=" + version +
               ", primaryLocation=" + primaryLocation +
               ", replicas=" + replicaLocations.size() +
               '}';
    }
}
