package com.telecom.distributed.core.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Represents current transaction patterns across nodes.
 */
public class TransactionPattern {
    private final Map<NodeId, Integer> nodeTransactionRates; // transactions/sec
    private final LocalDateTime timestamp;
    private final String patternType;
    
    public TransactionPattern(Map<NodeId, Integer> nodeTransactionRates, String patternType) {
        this.nodeTransactionRates = Objects.requireNonNull(nodeTransactionRates);
        this.patternType = Objects.requireNonNull(patternType);
        this.timestamp = LocalDateTime.now();
    }
    
    public Map<NodeId, Integer> getNodeTransactionRates() { return nodeTransactionRates; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getPatternType() { return patternType; }
    
    public int getTotalTransactionRate() {
        return nodeTransactionRates.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    @Override
    public String toString() {
        return "TransactionPattern{" +
               "nodeTransactionRates=" + nodeTransactionRates.size() + " nodes" +
               ", timestamp=" + timestamp +
               ", patternType='" + patternType + '\'' +
               ", totalRate=" + getTotalTransactionRate() + " tx/sec" +
               '}';
    }
}