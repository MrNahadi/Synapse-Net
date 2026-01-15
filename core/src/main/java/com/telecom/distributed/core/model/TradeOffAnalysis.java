package com.telecom.distributed.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a quantitative trade-off analysis between throughput and latency.
 * Contains Pareto frontier analysis and optimal configuration recommendations.
 */
public class TradeOffAnalysis {
    private final List<ParetoPoint> paretoFrontier;
    private final SystemConfiguration optimalConfiguration;
    private final double tradeOffRatio;
    private final String analysisDescription;
    private final double confidenceScore;

    public TradeOffAnalysis(List<ParetoPoint> paretoFrontier,
                           SystemConfiguration optimalConfiguration,
                           double tradeOffRatio,
                           String analysisDescription,
                           double confidenceScore) {
        this.paretoFrontier = Objects.requireNonNull(paretoFrontier, "Pareto frontier cannot be null");
        this.optimalConfiguration = Objects.requireNonNull(optimalConfiguration, "Optimal configuration cannot be null");
        this.tradeOffRatio = tradeOffRatio;
        this.analysisDescription = Objects.requireNonNull(analysisDescription, "Analysis description cannot be null");
        this.confidenceScore = confidenceScore;
    }

    public List<ParetoPoint> getParetoFrontier() {
        return paretoFrontier;
    }

    public SystemConfiguration getOptimalConfiguration() {
        return optimalConfiguration;
    }

    public double getTradeOffRatio() {
        return tradeOffRatio;
    }

    public String getAnalysisDescription() {
        return analysisDescription;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeOffAnalysis that = (TradeOffAnalysis) o;
        return Double.compare(that.tradeOffRatio, tradeOffRatio) == 0 &&
               Double.compare(that.confidenceScore, confidenceScore) == 0 &&
               Objects.equals(paretoFrontier, paretoFrontier) &&
               Objects.equals(optimalConfiguration, optimalConfiguration) &&
               Objects.equals(analysisDescription, analysisDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paretoFrontier, optimalConfiguration, tradeOffRatio, analysisDescription, confidenceScore);
    }

    @Override
    public String toString() {
        return "TradeOffAnalysis{" +
               "paretoPoints=" + paretoFrontier.size() +
               ", optimalConfig=" + optimalConfiguration +
               ", tradeOffRatio=" + tradeOffRatio +
               ", confidence=" + confidenceScore +
               '}';
    }
}