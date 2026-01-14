package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Dynamic adaptation engine for responding to changing traffic and transaction patterns.
 * Implements real-time adaptation algorithms to optimize system behavior.
 */
public class DynamicAdaptationEngine {
    
    private final Map<NodeId, TrafficHistory> trafficHistory;
    private final Map<NodeId, TransactionHistory> transactionHistory;
    private final AdaptationPolicyManager policyManager;
    
    private static final int HISTORY_WINDOW_MINUTES = 30;
    private static final double ADAPTATION_THRESHOLD = 0.15; // 15% change threshold
    
    public DynamicAdaptationEngine() {
        this.trafficHistory = new ConcurrentHashMap<>();
        this.transactionHistory = new ConcurrentHashMap<>();
        this.policyManager = new AdaptationPolicyManager();
    }
    
    /**
     * Adapts system behavior to current traffic and transaction patterns.
     * @param trafficPattern Current traffic pattern
     * @param transactionPattern Current transaction pattern
     * @param currentMetrics Current node metrics
     * @param nodeConfigurations Node configurations
     * @return Adaptation result
     */
    public AdaptationResult adaptToPatterns(TrafficPattern trafficPattern,
                                          TransactionPattern transactionPattern,
                                          Map<NodeId, NodeMetrics> currentMetrics,
                                          Map<NodeId, NodeConfiguration> nodeConfigurations) {
        
        // Update pattern history
        updatePatternHistory(trafficPattern, transactionPattern);
        
        // Detect pattern changes
        List<PatternChange> patternChanges = detectPatternChanges(trafficPattern, transactionPattern);
        
        // Generate adaptation strategies
        List<AdaptationStrategy> strategies = generateAdaptationStrategies(
            patternChanges, currentMetrics, nodeConfigurations);
        
        // Apply adaptations
        List<AdaptationAction> appliedActions = applyAdaptations(strategies);
        
        // Calculate adaptation effectiveness
        double effectivenessScore = calculateAdaptationEffectiveness(appliedActions, currentMetrics);
        
        return new AdaptationResult(
            patternChanges,
            strategies,
            appliedActions,
            effectivenessScore,
            LocalDateTime.now()
        );
    }
    
    /**
     * Updates historical pattern data for trend analysis.
     * @param trafficPattern Current traffic pattern
     * @param transactionPattern Current transaction pattern
     */
    private void updatePatternHistory(TrafficPattern trafficPattern, 
                                    TransactionPattern transactionPattern) {
        LocalDateTime now = LocalDateTime.now();
        
        // Update traffic history for each node
        for (Map.Entry<NodeId, Double> entry : trafficPattern.getNodeTrafficRates().entrySet()) {
            NodeId nodeId = entry.getKey();
            double trafficRate = entry.getValue();
            
            trafficHistory.computeIfAbsent(nodeId, k -> new TrafficHistory())
                         .addDataPoint(now, trafficRate);
        }
        
        // Update transaction history for each node
        for (Map.Entry<NodeId, Integer> entry : transactionPattern.getNodeTransactionRates().entrySet()) {
            NodeId nodeId = entry.getKey();
            int transactionRate = entry.getValue();
            
            transactionHistory.computeIfAbsent(nodeId, k -> new TransactionHistory())
                             .addDataPoint(now, transactionRate);
        }
        
        // Clean old history data
        cleanOldHistoryData(now);
    }
    
    /**
     * Detects significant changes in traffic and transaction patterns.
     * @param trafficPattern Current traffic pattern
     * @param transactionPattern Current transaction pattern
     * @return List of detected pattern changes
     */
    private List<PatternChange> detectPatternChanges(TrafficPattern trafficPattern,
                                                   TransactionPattern transactionPattern) {
        List<PatternChange> changes = new ArrayList<>();
        
        // Detect traffic pattern changes
        for (Map.Entry<NodeId, Double> entry : trafficPattern.getNodeTrafficRates().entrySet()) {
            NodeId nodeId = entry.getKey();
            double currentRate = entry.getValue();
            
            TrafficHistory history = trafficHistory.get(nodeId);
            if (history != null) {
                double historicalAverage = history.getAverageRate(HISTORY_WINDOW_MINUTES);
                double changeRatio = Math.abs(currentRate - historicalAverage) / historicalAverage;
                
                if (changeRatio > ADAPTATION_THRESHOLD) {
                    changes.add(new PatternChange(
                        nodeId,
                        PatternChangeType.TRAFFIC_SPIKE,
                        currentRate > historicalAverage ? "Traffic increase" : "Traffic decrease",
                        changeRatio,
                        LocalDateTime.now()
                    ));
                }
            }
        }
        
        // Detect transaction pattern changes
        for (Map.Entry<NodeId, Integer> entry : transactionPattern.getNodeTransactionRates().entrySet()) {
            NodeId nodeId = entry.getKey();
            int currentRate = entry.getValue();
            
            TransactionHistory history = transactionHistory.get(nodeId);
            if (history != null) {
                double historicalAverage = history.getAverageRate(HISTORY_WINDOW_MINUTES);
                double changeRatio = Math.abs(currentRate - historicalAverage) / historicalAverage;
                
                if (changeRatio > ADAPTATION_THRESHOLD) {
                    changes.add(new PatternChange(
                        nodeId,
                        PatternChangeType.TRANSACTION_BURST,
                        currentRate > historicalAverage ? "Transaction increase" : "Transaction decrease",
                        changeRatio,
                        LocalDateTime.now()
                    ));
                }
            }
        }
        
        return changes;
    }
    
    /**
     * Generates adaptation strategies based on detected pattern changes.
     * @param patternChanges Detected pattern changes
     * @param currentMetrics Current node metrics
     * @param nodeConfigurations Node configurations
     * @return List of adaptation strategies
     */
    private List<AdaptationStrategy> generateAdaptationStrategies(
            List<PatternChange> patternChanges,
            Map<NodeId, NodeMetrics> currentMetrics,
            Map<NodeId, NodeConfiguration> nodeConfigurations) {
        
        List<AdaptationStrategy> strategies = new ArrayList<>();
        
        for (PatternChange change : patternChanges) {
            NodeId nodeId = change.getNodeId();
            NodeMetrics metrics = currentMetrics.get(nodeId);
            NodeConfiguration config = nodeConfigurations.get(nodeId);
            
            if (metrics != null && config != null) {
                AdaptationStrategy strategy = policyManager.generateStrategy(change, metrics, config);
                if (strategy != null) {
                    strategies.add(strategy);
                }
            }
        }
        
        // Prioritize strategies by impact and urgency
        strategies.sort((s1, s2) -> {
            double score1 = s1.getImpactScore() * s1.getUrgencyScore();
            double score2 = s2.getImpactScore() * s2.getUrgencyScore();
            return Double.compare(score2, score1);
        });
        
        return strategies;
    }
    
    /**
     * Applies adaptation strategies to the system.
     * @param strategies Adaptation strategies to apply
     * @return List of applied adaptation actions
     */
    private List<AdaptationAction> applyAdaptations(List<AdaptationStrategy> strategies) {
        List<AdaptationAction> appliedActions = new ArrayList<>();
        
        for (AdaptationStrategy strategy : strategies) {
            try {
                AdaptationAction action = strategy.execute();
                appliedActions.add(action);
            } catch (Exception e) {
                // Log adaptation failure and continue with other strategies
                System.err.println("Failed to apply adaptation strategy: " + strategy.getDescription());
            }
        }
        
        return appliedActions;
    }
    
    /**
     * Calculates the effectiveness of applied adaptations.
     * @param appliedActions Applied adaptation actions
     * @param currentMetrics Current node metrics
     * @return Effectiveness score (0.0 to 1.0)
     */
    private double calculateAdaptationEffectiveness(List<AdaptationAction> appliedActions,
                                                  Map<NodeId, NodeMetrics> currentMetrics) {
        if (appliedActions.isEmpty()) {
            return 0.0;
        }
        
        double totalEffectiveness = 0.0;
        int validActions = 0;
        
        for (AdaptationAction action : appliedActions) {
            if (action.isSuccessful()) {
                totalEffectiveness += action.getEffectivenessScore();
                validActions++;
            }
        }
        
        return validActions > 0 ? totalEffectiveness / validActions : 0.0;
    }
    
    /**
     * Cleans old historical data beyond the retention window.
     * @param currentTime Current timestamp
     */
    private void cleanOldHistoryData(LocalDateTime currentTime) {
        LocalDateTime cutoffTime = currentTime.minus(HISTORY_WINDOW_MINUTES * 2, ChronoUnit.MINUTES);
        
        trafficHistory.values().forEach(history -> history.cleanOldData(cutoffTime));
        transactionHistory.values().forEach(history -> history.cleanOldData(cutoffTime));
    }
    
    /**
     * Gets adaptation statistics for monitoring.
     * @return Adaptation statistics
     */
    public AdaptationStatistics getAdaptationStatistics() {
        int totalNodes = trafficHistory.size();
        int activeAdaptations = (int) trafficHistory.values().stream()
            .mapToLong(history -> history.getRecentDataPoints(5).size())
            .sum();
        
        double averageAdaptationRate = activeAdaptations / (double) Math.max(totalNodes, 1);
        
        return new AdaptationStatistics(
            totalNodes,
            activeAdaptations,
            averageAdaptationRate,
            LocalDateTime.now()
        );
    }
}