package com.telecom.distributed.core.monitoring;

import com.telecom.distributed.core.model.NodeId;
import com.telecom.distributed.core.model.NodeMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Collects and manages performance metrics for the distributed telecom system.
 */
public class MetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollector.class);
    private static final Logger metricsLogger = LoggerFactory.getLogger("com.telecom.distributed.metrics");

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<NodeId, AtomicReference<NodeMetrics>> nodeMetrics;

    // Counters
    private final Counter rpcRequestCounter;
    private final Counter transactionCounter;
    private final Counter failureCounter;

    // Timers
    private final Timer rpcLatencyTimer;
    private final Timer transactionLatencyTimer;

    public MetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.nodeMetrics = new ConcurrentHashMap<>();

        // Initialize counters
        this.rpcRequestCounter = Counter.builder("rpc.requests.total")
                .description("Total number of RPC requests")
                .register(meterRegistry);

        this.transactionCounter = Counter.builder("transactions.total")
                .description("Total number of transactions")
                .register(meterRegistry);

        this.failureCounter = Counter.builder("failures.total")
                .description("Total number of failures")
                .register(meterRegistry);

        // Initialize timers
        this.rpcLatencyTimer = Timer.builder("rpc.latency")
                .description("RPC request latency")
                .register(meterRegistry);

        this.transactionLatencyTimer = Timer.builder("transaction.latency")
                .description("Transaction completion latency")
                .register(meterRegistry);
    }

    /**
     * Updates metrics for a specific node.
     */
    public void updateNodeMetrics(NodeId nodeId, NodeMetrics metrics) {
        nodeMetrics.computeIfAbsent(nodeId, k -> new AtomicReference<>()).set(metrics);
        
        // Register gauges for this node if not already registered
        registerNodeGauges(nodeId);
        
        // Log metrics
        metricsLogger.info("Node {} metrics: {}", nodeId.getId(), metrics);
        logger.debug("Updated metrics for node {}: {}", nodeId, metrics);
    }

    /**
     * Gets current metrics for a node.
     */
    public NodeMetrics getNodeMetrics(NodeId nodeId) {
        AtomicReference<NodeMetrics> metricsRef = nodeMetrics.get(nodeId);
        return metricsRef != null ? metricsRef.get() : null;
    }

    /**
     * Records an RPC request.
     */
    public void recordRpcRequest(NodeId source, NodeId target, long latencyMs) {
        rpcRequestCounter.increment();
        rpcLatencyTimer.record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        logger.debug("Recorded RPC request from {} to {} with latency {}ms", source, target, latencyMs);
    }

    /**
     * Records a transaction.
     */
    public void recordTransaction(String transactionId, long durationMs, boolean successful) {
        transactionCounter.increment();
        transactionLatencyTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        logger.debug("Recorded transaction {} duration {}ms, successful: {}", transactionId, durationMs, successful);
    }

    /**
     * Records a failure event.
     */
    public void recordFailure(NodeId nodeId, String failureType) {
        failureCounter.increment();
        logger.warn("Recorded failure on node {}: {}", nodeId, failureType);
    }

    private void registerNodeGauges(NodeId nodeId) {
        String nodeIdStr = nodeId.getId();
        
        // Register gauges for node metrics
        Gauge.builder("node.latency", this, collector -> {
                    NodeMetrics metrics = collector.getNodeMetrics(nodeId);
                    return metrics != null ? metrics.getLatency() : 0.0;
                })
                .description("Node latency in milliseconds")
                .tag("node", nodeIdStr)
                .register(meterRegistry);

        Gauge.builder("node.throughput", this, collector -> {
                    NodeMetrics metrics = collector.getNodeMetrics(nodeId);
                    return metrics != null ? metrics.getThroughput() : 0.0;
                })
                .description("Node throughput in Mbps")
                .tag("node", nodeIdStr)
                .register(meterRegistry);

        Gauge.builder("node.cpu.utilization", this, collector -> {
                    NodeMetrics metrics = collector.getNodeMetrics(nodeId);
                    return metrics != null ? metrics.getCpuUtilization() : 0.0;
                })
                .description("Node CPU utilization percentage")
                .tag("node", nodeIdStr)
                .register(meterRegistry);

        Gauge.builder("node.memory.usage", this, collector -> {
                    NodeMetrics metrics = collector.getNodeMetrics(nodeId);
                    return metrics != null ? metrics.getMemoryUsage() : 0.0;
                })
                .description("Node memory usage in GB")
                .tag("node", nodeIdStr)
                .register(meterRegistry);
    }
}