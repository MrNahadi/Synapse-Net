package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.Set;

/**
 * Represents the systemic failure risk assessment for a node.
 * Combines failure type, criticality, and dependencies to provide a comprehensive risk score.
 */
public class SystemicFailureRisk {
    private final NodeId nodeId;
    private final double riskScore;              // 0.0 to 1.0
    private final FailureType primaryFailureType;
    private final double criticalityScore;       // 0.0 to 1.0
    private final Set<NodeId> dependentNodes;
    private final double cascadeRiskScore;       // 0.0 to 1.0
    private final String riskDescription;
    private final Set<String> mitigationStrategies;

    public SystemicFailureRisk(NodeId nodeId, double riskScore, FailureType primaryFailureType,
                              double criticalityScore, Set<NodeId> dependentNodes,
                              double cascadeRiskScore, String riskDescription,
                              Set<String> mitigationStrategies) {
        this.nodeId = Objects.requireNonNull(nodeId, "NodeId cannot be null");
        this.riskScore = validateScore(riskScore, "Risk score");
        this.primaryFailureType = Objects.requireNonNull(primaryFailureType, "FailureType cannot be null");
        this.criticalityScore = validateScore(criticalityScore, "Criticality score");
        this.dependentNodes = Objects.requireNonNull(dependentNodes, "Dependent nodes cannot be null");
        this.cascadeRiskScore = validateScore(cascadeRiskScore, "Cascade risk score");
        this.riskDescription = Objects.requireNonNull(riskDescription, "Risk description cannot be null");
        this.mitigationStrategies = Objects.requireNonNull(mitigationStrategies, "Mitigation strategies cannot be null");
    }

    private double validateScore(double score, String scoreName) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(scoreName + " must be between 0.0 and 1.0, got: " + score);
        }
        return score;
    }

    // Getters
    public NodeId getNodeId() { return nodeId; }
    public double getRiskScore() { return riskScore; }
    public FailureType getPrimaryFailureType() { return primaryFailureType; }
    public double getCriticalityScore() { return criticalityScore; }
    public Set<NodeId> getDependentNodes() { return dependentNodes; }
    public double getCascadeRiskScore() { return cascadeRiskScore; }
    public String getRiskDescription() { return riskDescription; }
    public Set<String> getMitigationStrategies() { return mitigationStrategies; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemicFailureRisk that = (SystemicFailureRisk) o;
        return Double.compare(that.riskScore, riskScore) == 0 &&
               Double.compare(that.criticalityScore, criticalityScore) == 0 &&
               Double.compare(that.cascadeRiskScore, cascadeRiskScore) == 0 &&
               Objects.equals(nodeId, that.nodeId) &&
               primaryFailureType == that.primaryFailureType &&
               Objects.equals(dependentNodes, that.dependentNodes) &&
               Objects.equals(riskDescription, that.riskDescription) &&
               Objects.equals(mitigationStrategies, that.mitigationStrategies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, riskScore, primaryFailureType, criticalityScore,
                          dependentNodes, cascadeRiskScore, riskDescription, mitigationStrategies);
    }

    @Override
    public String toString() {
        return "SystemicFailureRisk{" +
               "nodeId=" + nodeId +
               ", riskScore=" + String.format("%.3f", riskScore) +
               ", primaryFailureType=" + primaryFailureType +
               ", criticalityScore=" + String.format("%.3f", criticalityScore) +
               ", dependentNodes=" + dependentNodes +
               ", cascadeRiskScore=" + String.format("%.3f", cascadeRiskScore) +
               ", riskDescription='" + riskDescription + '\'' +
               ", mitigationStrategies=" + mitigationStrategies +
               '}';
    }
}
