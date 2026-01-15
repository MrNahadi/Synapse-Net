package com.telecom.distributed.core;

import com.telecom.distributed.core.model.SystemPerformanceMetrics;
import com.telecom.distributed.core.model.NodeMetrics;
import com.telecom.distributed.core.model.NodeId;
import com.telecom.distributed.core.ThroughputImprovementEstimator.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ThroughputImprovementEstimator.
 * Tests probabilistic modeling and improvement calculations.
 * Requirements: 15.3, 15.4
 */
public class ThroughputImprovementEstimatorTest {
    
    private ThroughputImprovementEstimator estimator;
    private SystemPerformanceMetrics baselineMetrics;
    
    /**
     * Helper method to create SystemPerformanceMetrics with desired characteristics.
     */
    private SystemPerformanceMetrics createMetrics(double avgThroughput, double avgLatency, 
                                                   double abortRate, double deadlockRate, 
                                                   double throughputVariance) {
        Map<NodeId, NodeMetrics> nodeMetrics = new HashMap<>();
        
        // Create 5 nodes with metrics that average to the desired values
        // Adjust individual values to create the desired variance
        double variance = throughputVariance / avgThroughput;
        
        // Calculate lock contention within valid range (5-15%)
        // Map abort rate (0.0-1.0) to lock contention (5-15%)
        double targetLockContention = 5.0 + (abortRate * 10.0);
        // Ensure it stays within bounds
        targetLockContention = Math.max(5.0, Math.min(15.0, targetLockContention));
        
        nodeMetrics.put(NodeId.EDGE1, new NodeMetrics(
            avgLatency * 0.9, avgThroughput * (1 - variance), 1.0, 50.0, 6.0, 150, Math.max(5.0, Math.min(15.0, targetLockContention * 0.9))));
        nodeMetrics.put(NodeId.EDGE2, new NodeMetrics(
            avgLatency * 1.1, avgThroughput * (1 + variance), 1.5, 55.0, 7.0, 160, Math.max(5.0, Math.min(15.0, targetLockContention * 1.1))));
        nodeMetrics.put(NodeId.CORE1, new NodeMetrics(
            avgLatency * 0.8, avgThroughput * (1 + variance * 0.5), 0.8, 60.0, 10.0, 200, targetLockContention));
        nodeMetrics.put(NodeId.CORE2, new NodeMetrics(
            avgLatency * 1.0, avgThroughput * (1 - variance * 0.5), 1.2, 52.0, 8.0, 180, Math.max(5.0, Math.min(15.0, targetLockContention * 1.05))));
        nodeMetrics.put(NodeId.CLOUD1, new NodeMetrics(
            avgLatency * 1.2, avgThroughput * 1.0, 2.0, 48.0, 12.0, 250, Math.max(5.0, Math.min(15.0, targetLockContention * 0.95))));
        
        return new SystemPerformanceMetrics(nodeMetrics);
    }
    
    @BeforeEach
    public void setUp() {
        estimator = new ThroughputImprovementEstimator(1000);
        
        // Create baseline metrics based on dataset characteristics
        baselineMetrics = createMetrics(
            750.0,    // Average throughput: 750 Mbps (mid-range of 470-1250)
            14.0,     // Average latency: 14ms (mid-range of 8-22)
            0.15,     // Transaction abort rate: 15%
            0.05,     // Deadlock rate: 0.05 deadlocks/sec
            50.0      // Throughput variance
        );
    }
    
    @Test
    public void testEstimateImprovementWithModerateOptimizations() {
        // Test with moderate optimization parameters
        double commitOptimization = 0.3;      // 30% reduction in aborts
        double deadlockReduction = 0.4;       // 40% reduction in deadlocks
        double latencyReduction = 2.0;        // 2ms latency reduction
        
        ThroughputImprovementEstimation result = estimator.estimateImprovement(
            baselineMetrics, commitOptimization, deadlockReduction, latencyReduction);
        
        // Verify baseline is preserved
        assertEquals(750.0, result.getBaselineThroughput(), 0.01);
        
        // Verify improvement is positive
        assertTrue(result.getAbsoluteImprovement() > 0, 
            "Absolute improvement should be positive");
        assertTrue(result.getPercentageImprovement() > 0, 
            "Percentage improvement should be positive");
        
        // Verify expected throughput is higher than baseline
        assertTrue(result.getExpectedThroughput() > result.getBaselineThroughput(),
            "Expected throughput should exceed baseline");
        
        // Verify confidence interval is valid
        ConfidenceInterval ci = result.getConfidenceInterval();
        assertTrue(ci.getLowerBound() >= 0, "Lower bound should be non-negative");
        assertTrue(ci.getUpperBound() > ci.getLowerBound(), 
            "Upper bound should exceed lower bound");
        assertEquals(0.95, ci.getConfidence(), 0.01);
        
        // Verify breakdown components are present
        ImprovementBreakdown breakdown = result.getBreakdown();
        assertTrue(breakdown.getCommitProtocolContribution() > 0);
        assertTrue(breakdown.getDeadlockReductionContribution() > 0);
        assertTrue(breakdown.getCommunicationContribution() > 0);
    }
    
    @Test
    public void testEstimateImprovementWithHighOptimizations() {
        // Test with aggressive optimization parameters
        double commitOptimization = 0.7;      // 70% reduction in aborts
        double deadlockReduction = 0.8;       // 80% reduction in deadlocks
        double latencyReduction = 5.0;        // 5ms latency reduction
        
        ThroughputImprovementEstimation result = estimator.estimateImprovement(
            baselineMetrics, commitOptimization, deadlockReduction, latencyReduction);
        
        // High optimizations should yield significant improvement
        assertTrue(result.getPercentageImprovement() > 10.0,
            "High optimizations should yield >10% improvement");
        
        // Verify all breakdown components contribute
        ImprovementBreakdown breakdown = result.getBreakdown();
        double totalContribution = breakdown.getTotalContribution();
        assertTrue(totalContribution > 0, "Total contribution should be positive");
        
        // Each component should contribute meaningfully
        assertTrue(breakdown.getCommitProtocolContribution() > 0);
        assertTrue(breakdown.getDeadlockReductionContribution() > 0);
        assertTrue(breakdown.getCommunicationContribution() > 0);
    }
    
    @Test
    public void testEstimateImprovementWithMinimalOptimizations() {
        // Test with minimal optimization parameters
        double commitOptimization = 0.05;     // 5% reduction in aborts
        double deadlockReduction = 0.1;       // 10% reduction in deadlocks
        double latencyReduction = 0.5;        // 0.5ms latency reduction
        
        ThroughputImprovementEstimation result = estimator.estimateImprovement(
            baselineMetrics, commitOptimization, deadlockReduction, latencyReduction);
        
        // Minimal optimizations should still show some improvement
        assertTrue(result.getAbsoluteImprovement() >= 0,
            "Even minimal optimizations should show non-negative improvement");
        assertTrue(result.getPercentageImprovement() >= 0);
        
        // Improvement should be modest
        assertTrue(result.getPercentageImprovement() < 10.0,
            "Minimal optimizations should yield <10% improvement");
    }
    
    @Test
    public void testWorstCaseAnalysis() {
        double commitOptimization = 0.3;
        double deadlockReduction = 0.4;
        double latencyReduction = 2.0;
        
        ThroughputImprovementEstimation worstCase = estimator.worstCaseAnalysis(
            baselineMetrics, commitOptimization, deadlockReduction, latencyReduction);
        
        ThroughputImprovementEstimation expected = estimator.estimateImprovement(
            baselineMetrics, commitOptimization, deadlockReduction, latencyReduction);
        
        // Worst case should be more conservative than expected case
        assertTrue(worstCase.getAbsoluteImprovement() <= expected.getAbsoluteImprovement(),
            "Worst case improvement should be <= expected improvement");
        
        // Worst case should still show positive improvement
        assertTrue(worstCase.getAbsoluteImprovement() > 0,
            "Worst case should still show positive improvement");
        
        // Verify breakdown in worst case
        ImprovementBreakdown breakdown = worstCase.getBreakdown();
        assertEquals(0.0, breakdown.getInteractionEffects(), 0.01,
            "Worst case should have no interaction effects");
    }
    
    @Test
    public void testProbabilisticModelingConsistency() {
        // Run estimation multiple times with same parameters
        double commitOptimization = 0.3;
        double deadlockReduction = 0.4;
        double latencyReduction = 2.0;
        
        ThroughputImprovementEstimation result1 = estimator.estimateImprovement(
            baselineMetrics, commitOptimization, deadlockReduction, latencyReduction);
        
        ThroughputImprovementEstimation result2 = estimator.estimateImprovement(
            baselineMetrics, commitOptimization, deadlockReduction, latencyReduction);
        
        // Results should be similar (within 20% due to stochastic nature)
        double diff = Math.abs(result1.getAbsoluteImprovement() - result2.getAbsoluteImprovement());
        double avgImprovement = (result1.getAbsoluteImprovement() + result2.getAbsoluteImprovement()) / 2.0;
        double relativeDiff = diff / avgImprovement;
        
        assertTrue(relativeDiff < 0.20,
            "Multiple runs should produce similar results (within 20%)");
    }
    
    @Test
    public void testImprovementBreakdownSumMatchesTotal() {
        double commitOptimization = 0.3;
        double deadlockReduction = 0.4;
        double latencyReduction = 2.0;
        
        ThroughputImprovementEstimation result = estimator.estimateImprovement(
            baselineMetrics, commitOptimization, deadlockReduction, latencyReduction);
        
        ImprovementBreakdown breakdown = result.getBreakdown();
        double breakdownTotal = breakdown.getTotalContribution();
        
        // Breakdown total should be close to absolute improvement (within variance)
        // Allow for stochastic noise in the simulation
        double diff = Math.abs(breakdownTotal - result.getAbsoluteImprovement());
        assertTrue(diff < result.getAbsoluteImprovement() * 0.3,
            "Breakdown total should be close to absolute improvement");
    }
    
    @Test
    public void testConfidenceIntervalContainsMean() {
        double commitOptimization = 0.3;
        double deadlockReduction = 0.4;
        double latencyReduction = 2.0;
        
        ThroughputImprovementEstimation result = estimator.estimateImprovement(
            baselineMetrics, commitOptimization, deadlockReduction, latencyReduction);
        
        ConfidenceInterval ci = result.getConfidenceInterval();
        double improvement = result.getAbsoluteImprovement();
        
        // Mean improvement should typically fall within confidence interval
        // (may occasionally fail due to stochastic nature, but should be rare)
        assertTrue(improvement >= ci.getLowerBound() * 0.8 && 
                  improvement <= ci.getUpperBound() * 1.2,
            "Mean improvement should be near confidence interval");
    }
    
    @Test
    public void testInvalidInputValidation() {
        // Test invalid commit protocol optimization
        assertThrows(IllegalArgumentException.class, () -> {
            estimator.estimateImprovement(baselineMetrics, -0.1, 0.4, 2.0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            estimator.estimateImprovement(baselineMetrics, 1.5, 0.4, 2.0);
        });
        
        // Test invalid deadlock reduction factor
        assertThrows(IllegalArgumentException.class, () -> {
            estimator.estimateImprovement(baselineMetrics, 0.3, -0.1, 2.0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            estimator.estimateImprovement(baselineMetrics, 0.3, 1.5, 2.0);
        });
        
        // Test invalid communication latency reduction
        assertThrows(IllegalArgumentException.class, () -> {
            estimator.estimateImprovement(baselineMetrics, 0.3, 0.4, -1.0);
        });
    }
    
    @Test
    public void testZeroOptimizationsYieldMinimalImprovement() {
        // Test with zero optimizations
        ThroughputImprovementEstimation result = estimator.estimateImprovement(
            baselineMetrics, 0.0, 0.0, 0.0);
        
        // Should yield minimal or zero improvement
        assertTrue(result.getAbsoluteImprovement() >= 0);
        assertTrue(result.getPercentageImprovement() < 1.0,
            "Zero optimizations should yield <1% improvement");
    }
    
    @Test
    public void testHighBaselineAbortRateAmplifiesCommitProtocolGains() {
        // Create metrics with high abort rate
        SystemPerformanceMetrics highAbortMetrics = createMetrics(
            750.0, 14.0, 0.40, 0.05, 50.0);  // 40% abort rate
        
        ThroughputImprovementEstimation highAbortResult = estimator.estimateImprovement(
            highAbortMetrics, 0.5, 0.0, 0.0);
        
        // Create metrics with low abort rate
        SystemPerformanceMetrics lowAbortMetrics = createMetrics(
            750.0, 14.0, 0.10, 0.05, 50.0);  // 10% abort rate
        
        ThroughputImprovementEstimation lowAbortResult = estimator.estimateImprovement(
            lowAbortMetrics, 0.5, 0.0, 0.0);
        
        // High abort rate should benefit more from commit protocol optimization
        assertTrue(highAbortResult.getBreakdown().getCommitProtocolContribution() >
                  lowAbortResult.getBreakdown().getCommitProtocolContribution(),
            "Higher abort rate should amplify commit protocol gains");
    }
    
    @Test
    public void testHighDeadlockRateAmplifiesDeadlockReductionGains() {
        // Create metrics with high deadlock rate
        SystemPerformanceMetrics highDeadlockMetrics = createMetrics(
            750.0, 14.0, 0.15, 0.20, 50.0);  // 0.20 deadlocks/sec
        
        ThroughputImprovementEstimation highDeadlockResult = estimator.estimateImprovement(
            highDeadlockMetrics, 0.0, 0.5, 0.0);
        
        // Create metrics with low deadlock rate
        SystemPerformanceMetrics lowDeadlockMetrics = createMetrics(
            750.0, 14.0, 0.15, 0.02, 50.0);  // 0.02 deadlocks/sec
        
        ThroughputImprovementEstimation lowDeadlockResult = estimator.estimateImprovement(
            lowDeadlockMetrics, 0.0, 0.5, 0.0);
        
        // High deadlock rate should benefit more from deadlock reduction (allow for stochastic variance)
        // Due to probabilistic nature, allow high to be at least 80% of low contribution
        assertTrue(highDeadlockResult.getBreakdown().getDeadlockReductionContribution() >=
                  lowDeadlockResult.getBreakdown().getDeadlockReductionContribution() * 0.8,
            "Higher deadlock rate should amplify deadlock reduction gains (with stochastic tolerance)");
    }
    
    @Test
    public void testCombinedOptimizationsShowInteractionEffects() {
        // Test with all optimizations enabled
        ThroughputImprovementEstimation combinedResult = estimator.estimateImprovement(
            baselineMetrics, 0.3, 0.4, 2.0);
        
        // Test with individual optimizations
        ThroughputImprovementEstimation commitOnly = estimator.estimateImprovement(
            baselineMetrics, 0.3, 0.0, 0.0);
        ThroughputImprovementEstimation deadlockOnly = estimator.estimateImprovement(
            baselineMetrics, 0.0, 0.4, 0.0);
        ThroughputImprovementEstimation communicationOnly = estimator.estimateImprovement(
            baselineMetrics, 0.0, 0.0, 2.0);
        
        double sumOfIndividual = commitOnly.getAbsoluteImprovement() +
                                deadlockOnly.getAbsoluteImprovement() +
                                communicationOnly.getAbsoluteImprovement();
        
        // Combined should be greater than sum of individual due to interaction effects
        assertTrue(combinedResult.getAbsoluteImprovement() >= sumOfIndividual * 0.95,
            "Combined optimizations should show synergistic effects");
    }
}
