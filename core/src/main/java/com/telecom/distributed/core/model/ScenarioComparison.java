package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Comparison between two optimization scenarios.
 */
public class ScenarioComparison {
    private final OptimizationScenario scenario1;
    private final OptimizationScenario scenario2;
    private final double throughputDifference;
    private final double latencyDifference;
    private final OptimizationScenario betterScenario;
    private final double scoreDifference;
    
    public ScenarioComparison(OptimizationScenario scenario1, OptimizationScenario scenario2,
                            double throughputDifference, double latencyDifference,
                            OptimizationScenario betterScenario, double scoreDifference) {
        this.scenario1 = Objects.requireNonNull(scenario1);
        this.scenario2 = Objects.requireNonNull(scenario2);
        this.throughputDifference = throughputDifference;
        this.latencyDifference = latencyDifference;
        this.betterScenario = Objects.requireNonNull(betterScenario);
        this.scoreDifference = scoreDifference;
    }
    
    public OptimizationScenario getScenario1() { return scenario1; }
    public OptimizationScenario getScenario2() { return scenario2; }
    public double getThroughputDifference() { return throughputDifference; }
    public double getLatencyDifference() { return latencyDifference; }
    public OptimizationScenario getBetterScenario() { return betterScenario; }
    public double getScoreDifference() { return scoreDifference; }
    
    @Override
    public String toString() {
        return "ScenarioComparison{" +
               "scenario1=" + scenario1.getName() +
               ", scenario2=" + scenario2.getName() +
               ", betterScenario=" + betterScenario.getName() +
               ", scoreDifference=" + scoreDifference +
               '}';
    }
}