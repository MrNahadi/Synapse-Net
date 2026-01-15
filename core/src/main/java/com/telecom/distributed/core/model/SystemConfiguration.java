package com.telecom.distributed.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a complete system configuration for the distributed telecom system.
 * Contains all parameters that affect throughput-latency trade-offs.
 */
public class SystemConfiguration {
    private final Map<NodeId, NodeConfiguration> nodeConfigurations;
    private final Map<ServiceId, ReplicationStrategy> replicationStrategies;
    private final String loadBalancingStrategy;
    private final int maxConcurrentTransactions;
    private final double networkBufferSize;
    private final int connectionPoolSize;
    private final long transactionTimeout;
    private final double configurationScore;

    public SystemConfiguration(Map<NodeId, NodeConfiguration> nodeConfigurations,
                              Map<ServiceId, ReplicationStrategy> replicationStrategies,
                              String loadBalancingStrategy,
                              int maxConcurrentTransactions,
                              double networkBufferSize,
                              int connectionPoolSize,
                              long transactionTimeout,
                              double configurationScore) {
        this.nodeConfigurations = Objects.requireNonNull(nodeConfigurations, "Node configurations cannot be null");
        this.replicationStrategies = Objects.requireNonNull(replicationStrategies, "Replication strategies cannot be null");
        this.loadBalancingStrategy = Objects.requireNonNull(loadBalancingStrategy, "Load balancing strategy cannot be null");
        this.maxConcurrentTransactions = maxConcurrentTransactions;
        this.networkBufferSize = networkBufferSize;
        this.connectionPoolSize = connectionPoolSize;
        this.transactionTimeout = transactionTimeout;
        this.configurationScore = configurationScore;
    }

    public Map<NodeId, NodeConfiguration> getNodeConfigurations() {
        return nodeConfigurations;
    }

    public Map<ServiceId, ReplicationStrategy> getReplicationStrategies() {
        return replicationStrategies;
    }

    public String getLoadBalancingStrategy() {
        return loadBalancingStrategy;
    }

    public int getMaxConcurrentTransactions() {
        return maxConcurrentTransactions;
    }

    public double getNetworkBufferSize() {
        return networkBufferSize;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public long getTransactionTimeout() {
        return transactionTimeout;
    }

    public double getConfigurationScore() {
        return configurationScore;
    }

    /**
     * Creates a new configuration with modified replication strategies.
     */
    public SystemConfiguration withReplicationStrategies(Map<ServiceId, ReplicationStrategy> newStrategies) {
        return new SystemConfiguration(
            nodeConfigurations, newStrategies, loadBalancingStrategy,
            maxConcurrentTransactions, networkBufferSize, connectionPoolSize,
            transactionTimeout, configurationScore
        );
    }

    /**
     * Creates a new configuration with modified load balancing strategy.
     */
    public SystemConfiguration withLoadBalancingStrategy(String newStrategy) {
        return new SystemConfiguration(
            nodeConfigurations, replicationStrategies, newStrategy,
            maxConcurrentTransactions, networkBufferSize, connectionPoolSize,
            transactionTimeout, configurationScore
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemConfiguration that = (SystemConfiguration) o;
        return maxConcurrentTransactions == that.maxConcurrentTransactions &&
               Double.compare(that.networkBufferSize, networkBufferSize) == 0 &&
               connectionPoolSize == that.connectionPoolSize &&
               transactionTimeout == that.transactionTimeout &&
               Double.compare(that.configurationScore, configurationScore) == 0 &&
               Objects.equals(nodeConfigurations, nodeConfigurations) &&
               Objects.equals(replicationStrategies, replicationStrategies) &&
               Objects.equals(loadBalancingStrategy, loadBalancingStrategy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeConfigurations, replicationStrategies, loadBalancingStrategy,
                          maxConcurrentTransactions, networkBufferSize, connectionPoolSize,
                          transactionTimeout, configurationScore);
    }

    @Override
    public String toString() {
        return String.format("SystemConfiguration{nodes=%d, services=%d, strategy=%s, " +
                           "maxTx=%d, bufferSize=%.1f, poolSize=%d, timeout=%d, score=%.3f}",
                           nodeConfigurations.size(), replicationStrategies.size(),
                           loadBalancingStrategy, maxConcurrentTransactions,
                           networkBufferSize, connectionPoolSize, transactionTimeout, configurationScore);
    }
}