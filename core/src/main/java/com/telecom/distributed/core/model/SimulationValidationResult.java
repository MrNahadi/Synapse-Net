package com.telecom.distributed.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Results from simulation-based validation of multi-dimensional trade-off analysis.
 */
public class SimulationValidationResult {
    private final boolean isValid;
    private final double confidenceLevel;
    private final List<String> validationMetrics;
    private final String validationSummary;
    private final int simulationRuns;
    private final double averageError;
    
    public SimulationValidationResult(
            boolean isValid,
            double confidenceLevel,
            List<String> validationMetrics,
            String validationSummary,
            int simulationRuns,
            double averageError) {
        this.isValid = isValid;
        this.confidenceLevel = confidenceLevel;
        this.validationMetrics = Objects.requireNonNull(validationMetrics, "Validation metrics cannot be null");
        this.validationSummary = Objects.requireNonNull(validationSummary, "Validation summary cannot be null");
        this.simulationRuns = simulationRuns;
        this.averageError = averageError;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public double getConfidenceLevel() {
        return confidenceLevel;
    }
    
    public List<String> getValidationMetrics() {
        return validationMetrics;
    }
    
    public String getValidationSummary() {
        return validationSummary;
    }
    
    public int getSimulationRuns() {
        return simulationRuns;
    }
    
    public double getAverageError() {
        return averageError;
    }
    
    @Override
    public String toString() {
        return String.format("SimulationValidationResult{valid=%s, confidence=%.2f, runs=%d, avgError=%.3f}",
                isValid, confidenceLevel, simulationRuns, averageError);
    }
}