package com.telecom.distributed.core.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Result of dynamic adaptation operations.
 */
public class AdaptationResult {
    private final List<PatternChange> patternChanges;
    private final List<AdaptationStrategy> strategies;
    private final List<AdaptationAction> appliedActions;
    private final double effectivenessScore;
    private final LocalDateTime timestamp;
    
    public AdaptationResult(List<PatternChange> patternChanges,
                          List<AdaptationStrategy> strategies,
                          List<AdaptationAction> appliedActions,
                          double effectivenessScore,
                          LocalDateTime timestamp) {
        this.patternChanges = Objects.requireNonNull(patternChanges);
        this.strategies = Objects.requireNonNull(strategies);
        this.appliedActions = Objects.requireNonNull(appliedActions);
        this.effectivenessScore = effectivenessScore;
        this.timestamp = Objects.requireNonNull(timestamp);
    }
    
    public List<PatternChange> getPatternChanges() { return patternChanges; }
    public List<AdaptationStrategy> getStrategies() { return strategies; }
    public List<AdaptationAction> getAppliedActions() { return appliedActions; }
    public double getEffectivenessScore() { return effectivenessScore; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return "AdaptationResult{" +
               "patternChanges=" + patternChanges.size() +
               ", strategies=" + strategies.size() +
               ", appliedActions=" + appliedActions.size() +
               ", effectivenessScore=" + (effectivenessScore * 100) + "%" +
               ", timestamp=" + timestamp +
               '}';
    }
}