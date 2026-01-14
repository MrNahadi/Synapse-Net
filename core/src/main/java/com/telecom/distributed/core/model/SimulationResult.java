package com.telecom.distributed.core.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Result of optimization simulation analysis.
 */
public class SimulationResult {
    private final OptimizationScenario scenario;
    private final AnalyticalResult analyticalResult;
    private final MonteCarloResult monteCarloResult;
    private final ValidationResult validation;
    private final PerformancePrediction prediction;
    private final Duration simulationDuration;
    private final double confidenceLevel;
    
    public SimulationResult(OptimizationScenario scenario,
                          AnalyticalResult analyticalResult,
                          MonteCarloResult monteCarloResult,
                          ValidationResult validation,
                          PerformancePrediction prediction,
                          Duration simulationDuration,
                          double confidenceLevel) {
        this.scenario = Objects.requireNonNull(scenario);
        this.analyticalResult = Objects.requireNonNull(analyticalResult);
        this.monteCarloResult = Objects.requireNonNull(monteCarloResult);
        this.validation = Objects.requireNonNull(validation);
        this.prediction = Objects.requireNonNull(prediction);
        this.simulationDuration = Objects.requireNonNull(simulationDuration);
        this.confidenceLevel = confidenceLevel;
    }
    
    public OptimizationScenario getScenario() { return scenario; }
    public AnalyticalResult getAnalyticalResult() { return analyticalResult; }
    public MonteCarloResult getMonteCarloResult() { return monteCarloResult; }
    public ValidationResult getValidation() { return validation; }
    public PerformancePrediction getPrediction() { return prediction; }
    public Duration getSimulationDuration() { return simulationDuration; }
    public double getConfidenceLevel() { return confidenceLevel; }
    
    @Override
    public String toString() {
        return "SimulationResult{" +
               "scenario=" + scenario.getName() +
               ", validation=" + (validation.isValid() ? "VALID" : "INVALID") +
               ", simulationDuration=" + simulationDuration.toMillis() + "ms" +
               ", confidenceLevel=" + (confidenceLevel * 100) + "%" +
               '}';
    }
}