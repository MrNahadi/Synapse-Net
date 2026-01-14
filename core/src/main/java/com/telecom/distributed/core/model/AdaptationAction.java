package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Represents an adaptation action that has been executed.
 */
public class AdaptationAction {
    private final String description;
    private final boolean successful;
    private final double effectivenessScore;
    
    public AdaptationAction(String description, boolean successful, double effectivenessScore) {
        this.description = Objects.requireNonNull(description);
        this.successful = successful;
        this.effectivenessScore = effectivenessScore;
    }
    
    public String getDescription() { return description; }
    public boolean isSuccessful() { return successful; }
    public double getEffectivenessScore() { return effectivenessScore; }
    
    @Override
    public String toString() {
        return "AdaptationAction{" +
               "description='" + description + '\'' +
               ", successful=" + successful +
               ", effectivenessScore=" + effectivenessScore +
               '}';
    }
}