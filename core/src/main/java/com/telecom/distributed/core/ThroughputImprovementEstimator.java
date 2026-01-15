package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Estimates expected throughput improvement after applying optimizations from
 * distributed commit protocols, deadlock detection, and communication management.
 * Uses probabilistic modeling and stochastic simulation to provide quantitative metrics.
 */
public class ThroughputImprovementEstimator {
    private static final Logger logger = LoggerFactory.getLogger(ThroughputImprovementEstimator.class);
    
    private final int simulationIterations;
    private final Random random;
    
    public ThroughputImprovementEstimator(int simulationIterations) {
        this.simulationIterations = simulationIterations;
        this.random = ThreadLocalRandom.current();
    }
    
    /**
     * Estimates throughput improvement using probabilistic modeling.
     * Integrates optimizations from commit protocols, deadlock detection, and communication management.
     * 
     * @param baselineMetrics Current system performance metrics
     * @param commitProtocolOptimization Expected improvement from commit protocol optimization
     * @param deadlockReductionFactor Expected reduction in deadlock occurrences (0.0 to 1.0)
     * @param communicationLatencyReduction Expected reduction in communication latency (milliseconds)
     * @return Throughput improvement estimation with detailed metrics
     */
    public ThroughputImprovementEstimation estimateImprovement(
            SystemPerformanceMetrics baselineMetrics,
            double commitProtocolOptimization,
            double deadlockReductionFactor,
            double communicationLatencyReduction) {
        
        logger.info("Starting throughput improvement estimation with {} iterations", simulationIterations);
        
        // Validate inputs
        validateInputs(commitProtocolOptimization, deadlockReductionFactor, communicationLatencyReduction);
        
        // Calculate baseline throughput
        double baselineThroughput = baselineMetrics.getAverageThroughput();
        
        // Probabilistic modeling of improvements
        ProbabilisticModel model = buildProbabilisticModel(
            baselineMetrics, commitProtocolOptimization, deadlockReductionFactor, communicationLatencyReduction);
        
        // Run stochastic simulation
        SimulationResults simulationResults = runStochasticSimulation(model, baselineThroughput);
        
        // Calculate improvement metrics
        double expectedImprovement = simulationResults.getMeanImprovement();
        double improvementPercentage = (expectedImprovement / baselineThroughput) * 100.0;
        
        // Calculate confidence intervals
        ConfidenceInterval confidenceInterval = calculateConfidenceInterval(
            simulationResults.getImprovements(), 0.95);
        
        logger.info("Estimated throughput improvement: {:.2f}% (baseline: {} Mbps, expected: {} Mbps)",
            improvementPercentage, baselineThroughput, baselineThroughput + expectedImprovement);
        
        return new ThroughputImprovementEstimation(
            baselineThroughput,
            baselineThroughput + expectedImprovement,
            expectedImprovement,
            improvementPercentage,
            confidenceInterval,
            simulationResults.getBreakdown()
        );
    }
    
    /**
     * Builds probabilistic model incorporating all optimization factors.
     */
    private ProbabilisticModel buildProbabilisticModel(
            SystemPerformanceMetrics baselineMetrics,
            double commitProtocolOptimization,
            double deadlockReductionFactor,
            double communicationLatencyReduction) {
        
        // Model commit protocol improvement
        // Reduced transaction abort rate leads to higher throughput
        double baselineAbortRate = baselineMetrics.getTransactionAbortRate();
        double improvedAbortRate = baselineAbortRate * (1.0 - commitProtocolOptimization);
        double commitProtocolGain = (baselineAbortRate - improvedAbortRate) * 
            baselineMetrics.getAverageThroughput();
        
        // Model deadlock reduction improvement
        // Fewer deadlocks mean less transaction rollback overhead
        double baselineDeadlockRate = baselineMetrics.getDeadlockRate();
        double improvedDeadlockRate = baselineDeadlockRate * (1.0 - deadlockReductionFactor);
        double deadlockReductionGain = (baselineDeadlockRate - improvedDeadlockRate) * 
            baselineMetrics.getAverageThroughput() * 1.5; // Deadlocks have higher overhead
        
        // Model communication latency improvement
        // Reduced latency allows more transactions per second
        double baselineLatency = baselineMetrics.getAverageLatency();
        double improvedLatency = Math.max(baselineLatency - communicationLatencyReduction, 1.0);
        double latencyImprovementFactor = baselineLatency / improvedLatency;
        double communicationGain = baselineMetrics.getAverageThroughput() * 
            (latencyImprovementFactor - 1.0);
        
        // Model interaction effects (non-linear)
        // Improvements compound when combined
        double interactionEffect = (commitProtocolGain + deadlockReductionGain + communicationGain) * 0.1;
        
        return new ProbabilisticModel(
            commitProtocolGain,
            deadlockReductionGain,
            communicationGain,
            interactionEffect,
            baselineMetrics.getThroughputVariance()
        );
    }
    
    /**
     * Runs stochastic simulation to estimate throughput improvement distribution.
     */
    private SimulationResults runStochasticSimulation(ProbabilisticModel model, double baselineThroughput) {
        List<Double> improvements = new ArrayList<>(simulationIterations);
        
        for (int i = 0; i < simulationIterations; i++) {
            // Sample from probability distributions for each optimization factor
            double commitGain = sampleNormal(model.getCommitProtocolGain(), 
                model.getCommitProtocolGain() * 0.15);
            double deadlockGain = sampleNormal(model.getDeadlockReductionGain(), 
                model.getDeadlockReductionGain() * 0.20);
            double communicationGain = sampleNormal(model.getCommunicationGain(), 
                model.getCommunicationGain() * 0.10);
            double interactionGain = sampleNormal(model.getInteractionEffect(), 
                model.getInteractionEffect() * 0.25);
            
            // Add stochastic noise based on baseline variance
            double noise = sampleNormal(0, Math.sqrt(model.getBaselineVariance()));
            
            // Calculate total improvement for this iteration
            double totalImprovement = commitGain + deadlockGain + communicationGain + 
                interactionGain + noise;
            
            // Ensure non-negative improvement
            totalImprovement = Math.max(0, totalImprovement);
            
            improvements.add(totalImprovement);
        }
        
        // Calculate breakdown of improvement sources
        ImprovementBreakdown breakdown = new ImprovementBreakdown(
            model.getCommitProtocolGain(),
            model.getDeadlockReductionGain(),
            model.getCommunicationGain(),
            model.getInteractionEffect()
        );
        
        return new SimulationResults(improvements, breakdown);
    }
    
    /**
     * Samples from normal distribution using Box-Muller transform.
     */
    private double sampleNormal(double mean, double stdDev) {
        double u1 = random.nextDouble();
        double u2 = random.nextDouble();
        double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        return mean + z * stdDev;
    }
    
    /**
     * Calculates confidence interval for improvement estimates.
     */
    private ConfidenceInterval calculateConfidenceInterval(List<Double> improvements, double confidence) {
        Collections.sort(improvements);
        
        int n = improvements.size();
        double alpha = 1.0 - confidence;
        int lowerIndex = (int) Math.floor(n * alpha / 2.0);
        int upperIndex = (int) Math.ceil(n * (1.0 - alpha / 2.0)) - 1;
        
        double lowerBound = improvements.get(Math.max(0, lowerIndex));
        double upperBound = improvements.get(Math.min(n - 1, upperIndex));
        
        return new ConfidenceInterval(lowerBound, upperBound, confidence);
    }
    
    /**
     * Validates input parameters.
     */
    private void validateInputs(double commitProtocolOptimization, 
                                double deadlockReductionFactor, 
                                double communicationLatencyReduction) {
        if (commitProtocolOptimization < 0.0 || commitProtocolOptimization > 1.0) {
            throw new IllegalArgumentException(
                "Commit protocol optimization must be between 0.0 and 1.0");
        }
        if (deadlockReductionFactor < 0.0 || deadlockReductionFactor > 1.0) {
            throw new IllegalArgumentException(
                "Deadlock reduction factor must be between 0.0 and 1.0");
        }
        if (communicationLatencyReduction < 0.0) {
            throw new IllegalArgumentException(
                "Communication latency reduction must be non-negative");
        }
    }
    
    /**
     * Performs worst-case analysis for throughput improvement.
     * Provides conservative estimate assuming minimal optimization benefits.
     */
    public ThroughputImprovementEstimation worstCaseAnalysis(
            SystemPerformanceMetrics baselineMetrics,
            double commitProtocolOptimization,
            double deadlockReductionFactor,
            double communicationLatencyReduction) {
        
        logger.info("Performing worst-case throughput improvement analysis");
        
        double baselineThroughput = baselineMetrics.getAverageThroughput();
        
        // Use conservative estimates (50% of expected improvement)
        double conservativeFactor = 0.5;
        
        ProbabilisticModel model = buildProbabilisticModel(
            baselineMetrics, 
            commitProtocolOptimization * conservativeFactor, 
            deadlockReductionFactor * conservativeFactor, 
            communicationLatencyReduction * conservativeFactor);
        
        // Calculate worst-case improvement (no interaction effects, no positive variance)
        double worstCaseImprovement = model.getCommitProtocolGain() + 
            model.getDeadlockReductionGain() + 
            model.getCommunicationGain();
        
        double improvementPercentage = (worstCaseImprovement / baselineThroughput) * 100.0;
        
        // Narrow confidence interval for worst-case
        ConfidenceInterval confidenceInterval = new ConfidenceInterval(
            worstCaseImprovement * 0.8, 
            worstCaseImprovement * 1.2, 
            0.95);
        
        ImprovementBreakdown breakdown = new ImprovementBreakdown(
            model.getCommitProtocolGain(),
            model.getDeadlockReductionGain(),
            model.getCommunicationGain(),
            0.0 // No interaction effects in worst case
        );
        
        return new ThroughputImprovementEstimation(
            baselineThroughput,
            baselineThroughput + worstCaseImprovement,
            worstCaseImprovement,
            improvementPercentage,
            confidenceInterval,
            breakdown
        );
    }
    
    // Inner classes for data structures
    
    private static class ProbabilisticModel {
        private final double commitProtocolGain;
        private final double deadlockReductionGain;
        private final double communicationGain;
        private final double interactionEffect;
        private final double baselineVariance;
        
        public ProbabilisticModel(double commitProtocolGain, double deadlockReductionGain,
                                 double communicationGain, double interactionEffect,
                                 double baselineVariance) {
            this.commitProtocolGain = commitProtocolGain;
            this.deadlockReductionGain = deadlockReductionGain;
            this.communicationGain = communicationGain;
            this.interactionEffect = interactionEffect;
            this.baselineVariance = baselineVariance;
        }
        
        public double getCommitProtocolGain() { return commitProtocolGain; }
        public double getDeadlockReductionGain() { return deadlockReductionGain; }
        public double getCommunicationGain() { return communicationGain; }
        public double getInteractionEffect() { return interactionEffect; }
        public double getBaselineVariance() { return baselineVariance; }
    }
    
    private static class SimulationResults {
        private final List<Double> improvements;
        private final ImprovementBreakdown breakdown;
        
        public SimulationResults(List<Double> improvements, ImprovementBreakdown breakdown) {
            this.improvements = improvements;
            this.breakdown = breakdown;
        }
        
        public double getMeanImprovement() {
            return improvements.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        
        public List<Double> getImprovements() { return improvements; }
        public ImprovementBreakdown getBreakdown() { return breakdown; }
    }
    
    /**
     * Result of throughput improvement estimation.
     */
    public static class ThroughputImprovementEstimation {
        private final double baselineThroughput;
        private final double expectedThroughput;
        private final double absoluteImprovement;
        private final double percentageImprovement;
        private final ConfidenceInterval confidenceInterval;
        private final ImprovementBreakdown breakdown;
        
        public ThroughputImprovementEstimation(double baselineThroughput, double expectedThroughput,
                                              double absoluteImprovement, double percentageImprovement,
                                              ConfidenceInterval confidenceInterval,
                                              ImprovementBreakdown breakdown) {
            this.baselineThroughput = baselineThroughput;
            this.expectedThroughput = expectedThroughput;
            this.absoluteImprovement = absoluteImprovement;
            this.percentageImprovement = percentageImprovement;
            this.confidenceInterval = confidenceInterval;
            this.breakdown = breakdown;
        }
        
        public double getBaselineThroughput() { return baselineThroughput; }
        public double getExpectedThroughput() { return expectedThroughput; }
        public double getAbsoluteImprovement() { return absoluteImprovement; }
        public double getPercentageImprovement() { return percentageImprovement; }
        public ConfidenceInterval getConfidenceInterval() { return confidenceInterval; }
        public ImprovementBreakdown getBreakdown() { return breakdown; }
        
        @Override
        public String toString() {
            return String.format(
                "ThroughputImprovementEstimation{baseline=%.2f Mbps, expected=%.2f Mbps, " +
                "improvement=%.2f Mbps (%.2f%%), CI=[%.2f, %.2f]}",
                baselineThroughput, expectedThroughput, absoluteImprovement, percentageImprovement,
                confidenceInterval.getLowerBound(), confidenceInterval.getUpperBound());
        }
    }
    
    /**
     * Confidence interval for improvement estimates.
     */
    public static class ConfidenceInterval {
        private final double lowerBound;
        private final double upperBound;
        private final double confidence;
        
        public ConfidenceInterval(double lowerBound, double upperBound, double confidence) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.confidence = confidence;
        }
        
        public double getLowerBound() { return lowerBound; }
        public double getUpperBound() { return upperBound; }
        public double getConfidence() { return confidence; }
    }
    
    /**
     * Breakdown of improvement by source.
     */
    public static class ImprovementBreakdown {
        private final double commitProtocolContribution;
        private final double deadlockReductionContribution;
        private final double communicationContribution;
        private final double interactionEffects;
        
        public ImprovementBreakdown(double commitProtocolContribution,
                                   double deadlockReductionContribution,
                                   double communicationContribution,
                                   double interactionEffects) {
            this.commitProtocolContribution = commitProtocolContribution;
            this.deadlockReductionContribution = deadlockReductionContribution;
            this.communicationContribution = communicationContribution;
            this.interactionEffects = interactionEffects;
        }
        
        public double getCommitProtocolContribution() { return commitProtocolContribution; }
        public double getDeadlockReductionContribution() { return deadlockReductionContribution; }
        public double getCommunicationContribution() { return communicationContribution; }
        public double getInteractionEffects() { return interactionEffects; }
        
        public double getTotalContribution() {
            return commitProtocolContribution + deadlockReductionContribution + 
                   communicationContribution + interactionEffects;
        }
    }
}
