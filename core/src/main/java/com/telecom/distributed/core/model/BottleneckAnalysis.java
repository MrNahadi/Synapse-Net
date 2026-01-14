package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.Set;

/**
 * Analysis result for performance bottlenecks in the distributed telecom system.
 */
public class BottleneckAnalysis {
    private final NodeId bottleneckNode;
    private final BottleneckType type;
    private final double severityScore;      // 0.0 to 1.0
    private final String description;
    private final Set<OptimizationSuggestion> suggestions;

    public BottleneckAnalysis(NodeId bottleneckNode, BottleneckType type, double severityScore,
                            String description, Set<OptimizationSuggestion> suggestions) {
        this.bottleneckNode = Objects.requireNonNull(bottleneckNode, "Bottleneck node cannot be null");
        this.type = Objects.requireNonNull(type, "Bottleneck type cannot be null");
        this.severityScore = validateSeverityScore(severityScore);
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.suggestions = Objects.requireNonNull(suggestions, "Suggestions cannot be null");
    }

    private double validateSeverityScore(double score) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Severity score must be between 0.0 and 1.0, got: " + score);
        }
        return score;
    }

    // Getters
    public NodeId getBottleneckNode() { return bottleneckNode; }
    public BottleneckType getType() { return type; }
    public double getSeverityScore() { return severityScore; }
    public String getDescription() { return description; }
    public Set<OptimizationSuggestion> getSuggestions() { return suggestions; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BottleneckAnalysis that = (BottleneckAnalysis) o;
        return Double.compare(that.severityScore, severityScore) == 0 &&
               Objects.equals(bottleneckNode, that.bottleneckNode) &&
               type == that.type &&
               Objects.equals(description, that.description) &&
               Objects.equals(suggestions, that.suggestions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bottleneckNode, type, severityScore, description, suggestions);
    }

    @Override
    public String toString() {
        return "BottleneckAnalysis{" +
               "bottleneckNode=" + bottleneckNode +
               ", type=" + type +
               ", severityScore=" + severityScore +
               ", description='" + description + '\'' +
               ", suggestions=" + suggestions +
               '}';
    }
}