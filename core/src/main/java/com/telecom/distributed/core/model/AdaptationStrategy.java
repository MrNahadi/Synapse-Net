package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Represents an adaptation strategy for responding to pattern changes.
 */
public class AdaptationStrategy {
    private final String description;
    private final double impactScore;
    private final double urgencyScore;
    
    public AdaptationStrategy(String description, double impactScore, double urgencyScore) {
        this.description = Objects.requireNonNull(description);
        this.impactScore = impactScore;
        this.urgencyScore = urgencyScore;
    }
    
    public String getDescription() { return description; }
    public double getImpactScore() { return impactScore; }
    public double getUrgencyScore() { return urgencyScore; }
    
    public AdaptationAction execute() {
        // Simplified execution - return a successful action
        return new AdaptationAction(description, true, 0.8);
    }
    
    @Override
    public String toString() {
        return "AdaptationStrategy{" +
               "description='" + description + '\'' +
               ", impactScore=" + impactScore +
               ", urgencyScore=" + urgencyScore +
               '}';
    }
}