package com.telecom.distributed.core.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of what-if analysis comparing multiple optimization scenarios.
 */
public class WhatIfAnalysisResult {
    private final OptimizationScenario baseScenario;
    private final List<OptimizationScenario> variations;
    private final Map<OptimizationScenario, SimulationResult> results;
    private final List<ScenarioComparison> comparisons;
    private final OptimizationScenario recommendedScenario;
    
    public WhatIfAnalysisResult(OptimizationScenario baseScenario,
                              List<OptimizationScenario> variations,
                              Map<OptimizationScenario, SimulationResult> results,
                              List<ScenarioComparison> comparisons,
                              OptimizationScenario recommendedScenario) {
        this.baseScenario = Objects.requireNonNull(baseScenario);
        this.variations = Objects.requireNonNull(variations);
        this.results = Objects.requireNonNull(results);
        this.comparisons = Objects.requireNonNull(comparisons);
        this.recommendedScenario = recommendedScenario;
    }
    
    public OptimizationScenario getBaseScenario() { return baseScenario; }
    public List<OptimizationScenario> getVariations() { return variations; }
    public Map<OptimizationScenario, SimulationResult> getResults() { return results; }
    public List<ScenarioComparison> getComparisons() { return comparisons; }
    public OptimizationScenario getRecommendedScenario() { return recommendedScenario; }
    
    @Override
    public String toString() {
        return "WhatIfAnalysisResult{" +
               "baseScenario=" + baseScenario.getName() +
               ", variations=" + variations.size() +
               ", recommendedScenario=" + (recommendedScenario != null ? recommendedScenario.getName() : "none") +
               '}';
    }
}