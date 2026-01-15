package com.telecom.distributed.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a comprehensive multi-dimensional trade-off analysis across
 * reliability, latency, throughput, resource utilization, scalability, and maintainability.
 */
public class MultiDimensionalTradeOffAnalysis {
    private final Map<String, Double> dimensionScores;
    private final Map<String, String> dimensionAnalysis;
    private final SystemConfiguration recommendedConfiguration;
    private final double overallScore;
    private final String criticalAnalysis;
    private final SimulationValidationResult simulationValidation;
    
    public MultiDimensionalTradeOffAnalysis(
            Map<String, Double> dimensionScores,
            Map<String, String> dimensionAnalysis,
            SystemConfiguration recommendedConfiguration,
            double overallScore,
            String criticalAnalysis,
            SimulationValidationResult simulationValidation) {
        this.dimensionScores = Objects.requireNonNull(dimensionScores, "Dimension scores cannot be null");
        this.dimensionAnalysis = Objects.requireNonNull(dimensionAnalysis, "Dimension analysis cannot be null");
        this.recommendedConfiguration = Objects.requireNonNull(recommendedConfiguration, "Recommended configuration cannot be null");
        this.overallScore = overallScore;
        this.criticalAnalysis = Objects.requireNonNull(criticalAnalysis, "Critical analysis cannot be null");
        this.simulationValidation = Objects.requireNonNull(simulationValidation, "Simulation validation cannot be null");
    }
    
    public Map<String, Double> getDimensionScores() {
        return dimensionScores;
    }
    
    public Map<String, String> getDimensionAnalysis() {
        return dimensionAnalysis;
    }
    
    public SystemConfiguration getRecommendedConfiguration() {
        return recommendedConfiguration;
    }
    
    public double getOverallScore() {
        return overallScore;
    }
    
    public String getCriticalAnalysis() {
        return criticalAnalysis;
    }
    
    public SimulationValidationResult getSimulationValidation() {
        return simulationValidation;
    }
    
    public double getReliabilityScore() {
        return dimensionScores.getOrDefault("reliability", 0.0);
    }
    
    public double getLatencyScore() {
        return dimensionScores.getOrDefault("latency", 0.0);
    }
    
    public double getThroughputScore() {
        return dimensionScores.getOrDefault("throughput", 0.0);
    }
    
    public double getResourceUtilizationScore() {
        return dimensionScores.getOrDefault("resource_utilization", 0.0);
    }
    
    public double getScalabilityScore() {
        return dimensionScores.getOrDefault("scalability", 0.0);
    }
    
    public double getMaintainabilityScore() {
        return dimensionScores.getOrDefault("maintainability", 0.0);
    }
    
    @Override
    public String toString() {
        return String.format("MultiDimensionalTradeOffAnalysis{" +
                "reliability=%.2f, latency=%.2f, throughput=%.2f, " +
                "resourceUtilization=%.2f, scalability=%.2f, maintainability=%.2f, " +
                "overallScore=%.2f, simulationValid=%s}",
                getReliabilityScore(), getLatencyScore(), getThroughputScore(),
                getResourceUtilizationScore(), getScalabilityScore(), getMaintainabilityScore(),
                overallScore, simulationValidation.isValid());
    }
}
