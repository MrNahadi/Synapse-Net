package com.telecom.distributed.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Overall system health assessment across all nodes.
 * Provides comprehensive view of system reliability and performance.
 */
public class SystemHealthAssessment {
    private final SystemHealthStatus overallStatus;
    private final Map<NodeId, HealthStatus> nodeHealthMap;
    private final Set<NodeId> failedNodes;
    private final Set<NodeId> degradedNodes;
    private final double systemReliabilityScore; // 0.0 to 1.0
    private final Instant assessmentTime;
    private final Set<String> activeAlerts;
    private final CascadeRiskLevel cascadeRisk;

    public SystemHealthAssessment(SystemHealthStatus overallStatus, Map<NodeId, HealthStatus> nodeHealthMap,
                                Set<NodeId> failedNodes, Set<NodeId> degradedNodes, double systemReliabilityScore,
                                Instant assessmentTime, Set<String> activeAlerts, CascadeRiskLevel cascadeRisk) {
        this.overallStatus = Objects.requireNonNull(overallStatus, "Overall status cannot be null");
        this.nodeHealthMap = Objects.requireNonNull(nodeHealthMap, "Node health map cannot be null");
        this.failedNodes = Objects.requireNonNull(failedNodes, "Failed nodes cannot be null");
        this.degradedNodes = Objects.requireNonNull(degradedNodes, "Degraded nodes cannot be null");
        this.systemReliabilityScore = validateReliabilityScore(systemReliabilityScore);
        this.assessmentTime = Objects.requireNonNull(assessmentTime, "Assessment time cannot be null");
        this.activeAlerts = Objects.requireNonNull(activeAlerts, "Active alerts cannot be null");
        this.cascadeRisk = Objects.requireNonNull(cascadeRisk, "Cascade risk cannot be null");
    }

    private double validateReliabilityScore(double score) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Reliability score must be between 0.0 and 1.0, got: " + score);
        }
        return score;
    }

    public enum SystemHealthStatus {
        HEALTHY("All nodes operational, no issues detected"),
        DEGRADED("Some nodes degraded but system functional"),
        CRITICAL("Multiple failures, system at risk"),
        FAILED("System cannot provide required services");

        private final String description;

        SystemHealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum CascadeRiskLevel {
        LOW("Minimal risk of cascading failures"),
        MEDIUM("Moderate risk, monitoring required"),
        HIGH("High risk, preventive action needed"),
        CRITICAL("Imminent cascade risk, immediate action required");

        private final String description;

        CascadeRiskLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getters
    public SystemHealthStatus getOverallStatus() { return overallStatus; }
    public Map<NodeId, HealthStatus> getNodeHealthMap() { return nodeHealthMap; }
    public Set<NodeId> getFailedNodes() { return failedNodes; }
    public Set<NodeId> getDegradedNodes() { return degradedNodes; }
    public double getSystemReliabilityScore() { return systemReliabilityScore; }
    public Instant getAssessmentTime() { return assessmentTime; }
    public Set<String> getActiveAlerts() { return activeAlerts; }
    public CascadeRiskLevel getCascadeRisk() { return cascadeRisk; }

    public int getHealthyNodeCount() {
        return (int) nodeHealthMap.values().stream()
                .filter(HealthStatus::isHealthy)
                .count();
    }

    public int getOperationalNodeCount() {
        return (int) nodeHealthMap.values().stream()
                .filter(HealthStatus::isOperational)
                .count();
    }

    public boolean isSystemOperational() {
        return overallStatus == SystemHealthStatus.HEALTHY || overallStatus == SystemHealthStatus.DEGRADED;
    }

    public boolean requiresImmediateAction() {
        return overallStatus == SystemHealthStatus.CRITICAL || 
               overallStatus == SystemHealthStatus.FAILED ||
               cascadeRisk == CascadeRiskLevel.CRITICAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemHealthAssessment that = (SystemHealthAssessment) o;
        return Double.compare(that.systemReliabilityScore, systemReliabilityScore) == 0 &&
               overallStatus == that.overallStatus &&
               Objects.equals(nodeHealthMap, that.nodeHealthMap) &&
               Objects.equals(failedNodes, that.failedNodes) &&
               Objects.equals(degradedNodes, that.degradedNodes) &&
               Objects.equals(assessmentTime, that.assessmentTime) &&
               Objects.equals(activeAlerts, that.activeAlerts) &&
               cascadeRisk == that.cascadeRisk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(overallStatus, nodeHealthMap, failedNodes, degradedNodes, 
                          systemReliabilityScore, assessmentTime, activeAlerts, cascadeRisk);
    }

    @Override
    public String toString() {
        return "SystemHealthAssessment{" +
               "overallStatus=" + overallStatus +
               ", healthyNodes=" + getHealthyNodeCount() +
               ", failedNodes=" + failedNodes.size() +
               ", degradedNodes=" + degradedNodes.size() +
               ", reliabilityScore=" + systemReliabilityScore +
               ", cascadeRisk=" + cascadeRisk +
               ", activeAlerts=" + activeAlerts.size() +
               '}';
    }
}