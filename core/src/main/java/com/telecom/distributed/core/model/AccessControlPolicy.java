package com.telecom.distributed.core.model;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Access control policy for distributed files and state.
 * Defines which nodes and operations are permitted.
 */
public class AccessControlPolicy {
    private final String policyId;
    private final Set<NodeId> readPermissions;
    private final Set<NodeId> writePermissions;
    private final Set<NodeId> deletePermissions;
    private final boolean publicRead;
    private final boolean requiresAuthentication;

    public AccessControlPolicy(String policyId, Set<NodeId> readPermissions,
                             Set<NodeId> writePermissions, Set<NodeId> deletePermissions,
                             boolean publicRead, boolean requiresAuthentication) {
        this.policyId = Objects.requireNonNull(policyId, "Policy ID cannot be null");
        this.readPermissions = Objects.requireNonNull(readPermissions, "Read permissions cannot be null");
        this.writePermissions = Objects.requireNonNull(writePermissions, "Write permissions cannot be null");
        this.deletePermissions = Objects.requireNonNull(deletePermissions, "Delete permissions cannot be null");
        this.publicRead = publicRead;
        this.requiresAuthentication = requiresAuthentication;
    }

    public static AccessControlPolicy createPublicReadPolicy(String policyId) {
        return new AccessControlPolicy(policyId, Collections.emptySet(),
                                     Collections.emptySet(), Collections.emptySet(),
                                     true, false);
    }

    public static AccessControlPolicy createRestrictedPolicy(String policyId, Set<NodeId> allowedNodes) {
        return new AccessControlPolicy(policyId, allowedNodes, allowedNodes, allowedNodes,
                                     false, true);
    }

    public boolean canRead(NodeId nodeId) {
        return publicRead || readPermissions.contains(nodeId);
    }

    public boolean canWrite(NodeId nodeId) {
        return writePermissions.contains(nodeId);
    }

    public boolean canDelete(NodeId nodeId) {
        return deletePermissions.contains(nodeId);
    }

    public boolean hasAnyPermission(NodeId nodeId) {
        return canRead(nodeId) || canWrite(nodeId) || canDelete(nodeId);
    }

    // Getters
    public String getPolicyId() { return policyId; }
    public Set<NodeId> getReadPermissions() { return readPermissions; }
    public Set<NodeId> getWritePermissions() { return writePermissions; }
    public Set<NodeId> getDeletePermissions() { return deletePermissions; }
    public boolean isPublicRead() { return publicRead; }
    public boolean requiresAuthentication() { return requiresAuthentication; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessControlPolicy that = (AccessControlPolicy) o;
        return Objects.equals(policyId, that.policyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyId);
    }

    @Override
    public String toString() {
        return "AccessControlPolicy{" +
               "policyId='" + policyId + '\'' +
               ", publicRead=" + publicRead +
               ", requiresAuth=" + requiresAuthentication +
               ", readNodes=" + readPermissions.size() +
               ", writeNodes=" + writePermissions.size() +
               ", deleteNodes=" + deletePermissions.size() +
               '}';
    }
}
