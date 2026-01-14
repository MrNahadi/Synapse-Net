package com.telecom.distributed.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Result of resource constraint enforcement for a node.
 */
public class ResourceConstraintResult {
    private final NodeId nodeId;
    private final boolean compliant;
    private final List<ConstraintViolation> violations;
    private final List<ConstraintAction> recommendedActions;
    
    public ResourceConstraintResult(NodeId nodeId, boolean compliant,
                                  List<ConstraintViolation> violations,
                                  List<ConstraintAction> recommendedActions) {
        this.nodeId = Objects.requireNonNull(nodeId);
        this.compliant = compliant;
        this.violations = Objects.requireNonNull(violations);
        this.recommendedActions = Objects.requireNonNull(recommendedActions);
    }
    
    public NodeId getNodeId() { return nodeId; }
    public boolean isCompliant() { return compliant; }
    public List<ConstraintViolation> getViolations() { return violations; }
    public List<ConstraintAction> getRecommendedActions() { return recommendedActions; }
    
    @Override
    public String toString() {
        return "ResourceConstraintResult{" +
               "nodeId=" + nodeId +
               ", compliant=" + compliant +
               ", violations=" + violations.size() +
               ", recommendedActions=" + recommendedActions.size() +
               '}';
    }
}