package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Monte Carlo simulator for stochastic optimization analysis.
 */
public class MonteCarloSimulator {
    
    /**
     * Runs Monte Carlo simulation for optimization scenario.
     * @param scenario Optimization scenario
     * @param nodeConfigurations Node configurations
     * @param currentMetrics Current metrics
     * @param iterations Number of simulation iterations
     * @return Monte Carlo result
     */
    public MonteCarloResult simulate(OptimizationScenario scenario,
                                   Map<NodeId, NodeConfiguration> nodeConfigurations,
                                   Map<NodeId, NodeMetrics> currentMetrics,
                                   int iterations) {
        
        double[] throughputSamples = new double[iterations];
        double[] latencySamples = new double[iterations];
        
        for (int i = 0; i < iterations; i++) {
            // Simulate with random variations
            throughputSamples[i] = simulateThroughput(scenario, currentMetrics);
            latencySamples[i] = simulateLatency(scenario, currentMetrics);
        }
        
        // Calculate statistics
        double avgThroughput = calculateMean(throughputSamples);
        double avgLatency = calculateMean(latencySamples);
        
        ConfidenceInterval throughputCI = calculateConfidenceInterval(throughputSamples, 0.95);
        ConfidenceInterval latencyCI = calculateConfidenceInterval(latencySamples, 0.95);
        
        return new MonteCarloResult(avgThroughput, avgLatency, throughputCI, latencyCI);
    }
    
    private double simulateThroughput(OptimizationScenario scenario, Map<NodeId, NodeMetrics> currentMetrics) {
        double baseThroughput = currentMetrics.values().stream()
            .mapToDouble(NodeMetrics::getThroughput)
            .sum();
        
        // Add random variation (±10%)
        double variation = ThreadLocalRandom.current().nextGaussian() * 0.1;
        double improvementFactor = 1.0 + scenario.getExpectedThroughputImprovement() + variation;
        
        return Math.max(0, baseThroughput * improvementFactor);
    }
    
    private double simulateLatency(OptimizationScenario scenario, Map<NodeId, NodeMetrics> currentMetrics) {
        double averageLatency = currentMetrics.values().stream()
            .mapToDouble(NodeMetrics::getLatency)
            .average()
            .orElse(15.0);
        
        // Add random variation (±15%)
        double variation = ThreadLocalRandom.current().nextGaussian() * 0.15;
        double improvementFactor = 1.0 - scenario.getExpectedLatencyImprovement() + variation;
        
        return Math.max(1.0, averageLatency * improvementFactor);
    }
    
    private double calculateMean(double[] samples) {
        double sum = 0.0;
        for (double sample : samples) {
            sum += sample;
        }
        return sum / samples.length;
    }
    
    private ConfidenceInterval calculateConfidenceInterval(double[] samples, double confidence) {
        java.util.Arrays.sort(samples);
        double alpha = 1.0 - confidence;
        int lowerIndex = (int) (samples.length * alpha / 2);
        int upperIndex = (int) (samples.length * (1.0 - alpha / 2));
        
        double lowerBound = samples[lowerIndex];
        double upperBound = samples[upperIndex];
        
        return new ConfidenceInterval(lowerBound, upperBound);
    }
}