package com.telecom.distributed.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the analysis of cascading failure effects in the distributed system.
 * Tracks dependency chains and potential cascade paths.
 */
public class CascadeAnalysis {
    private final NodeId originNode;
    private final List<List<NodeId>> cascadePaths;
    private final Set<NodeId> affectedNodes;
    private final double cascadeProbability;
    private final int maxCascadeDepth;
    private final String analysisDescription;

    public CascadeAnalysis(NodeId originNode, List<List<NodeId>> cascadePaths,
                          Set<NodeId> affectedNodes, double cascadeProbability,
                          int maxCascadeDepth, String analysisDescription) {
        this.originNode = Objects.requireNonNull(originNode, "Origin node cannot be null");
        this.cascadePaths = Objects.requireNonNull(cascadePaths, "Cascade paths cannot be null");
        this.affectedNodes = Objects.requireNonNull(affectedNodes, "Affected nodes cannot be null");
        this.cascadeProbability = validateProbability(cascadeProbability);
        this.maxCascadeDepth = maxCascadeDepth;
        this.analysisDescription = Objects.requireNonNull(analysisDescription, "Analysis description cannot be null");
    }

    private double validateProbability(double probability) {
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("Cascade probability must be between 0.0 and 1.0, got: " + probability);
        }
        return probability;
    }

    // Getters
    public NodeId getOriginNode() { return originNode; }
    public List<List<NodeId>> getCascadePaths() { return cascadePaths; }
    public Set<NodeId> getAffectedNodes() { return affectedNodes; }
    public double getCascadeProbability() { return cascadeProbability; }
    public int getMaxCascadeDepth() { return maxCascadeDepth; }
    public String getAnalysisDescription() { return analysisDescription; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CascadeAnalysis that = (CascadeAnalysis) o;
        return Double.compare(that.cascadeProbability, cascadeProbability) == 0 &&
               maxCascadeDepth == that.maxCascadeDepth &&
               Objects.equals(originNode, that.originNode) &&
               Objects.equals(cascadePaths, that.cascadePaths) &&
               Objects.equals(affectedNodes, that.affectedNodes) &&
               Objects.equals(analysisDescription, that.analysisDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originNode, cascadePaths, affectedNodes, cascadeProbability,
                          maxCascadeDepth, analysisDescription);
    }

    @Override
    public String toString() {
        return "CascadeAnalysis{" +
               "originNode=" + originNode +
               ", cascadePaths=" + cascadePaths +
               ", affectedNodes=" + affectedNodes +
               ", cascadeProbability=" + String.format("%.3f", cascadeProbability) +
               ", maxCascadeDepth=" + maxCascadeDepth +
               ", analysisDescription='" + analysisDescription + '\'' +
               '}';
    }
}
