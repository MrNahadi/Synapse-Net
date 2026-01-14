package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Analysis result for the highest latency contributor in the system.
 */
public class LatencyContributorAnalysis {
    private final NodeId contributorNode;
    private final double latency;
    private final double conditionalFailureProbability;
    private final String justification;

    public LatencyContributorAnalysis(NodeId contributorNode, double latency,
                                    double conditionalFailureProbability, String justification) {
        this.contributorNode = Objects.requireNonNull(contributorNode, "Contributor node cannot be null");
        this.latency = validateLatency(latency);
        this.conditionalFailureProbability = validateProbability(conditionalFailureProbability);
        this.justification = Objects.requireNonNull(justification, "Justification cannot be null");
    }

    private double validateLatency(double latency) {
        if (latency < 0.0) {
            throw new IllegalArgumentException("Latency cannot be negative, got: " + latency);
        }
        return latency;
    }

    private double validateProbability(double probability) {
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("Probability must be between 0.0 and 1.0, got: " + probability);
        }
        return probability;
    }

    // Getters
    public NodeId getContributorNode() { return contributorNode; }
    public double getLatency() { return latency; }
    public double getConditionalFailureProbability() { return conditionalFailureProbability; }
    public String getJustification() { return justification; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatencyContributorAnalysis that = (LatencyContributorAnalysis) o;
        return Double.compare(that.latency, latency) == 0 &&
               Double.compare(that.conditionalFailureProbability, conditionalFailureProbability) == 0 &&
               Objects.equals(contributorNode, that.contributorNode) &&
               Objects.equals(justification, that.justification);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contributorNode, latency, conditionalFailureProbability, justification);
    }

    @Override
    public String toString() {
        return "LatencyContributorAnalysis{" +
               "contributorNode=" + contributorNode +
               ", latency=" + latency +
               ", conditionalFailureProbability=" + conditionalFailureProbability +
               ", justification='" + justification + '\'' +
               '}';
    }
}