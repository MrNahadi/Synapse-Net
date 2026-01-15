package com.telecom.distributed.core.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Network topology information for nodes in the distributed telecom system.
 */
public class NetworkTopology {
    private final Set<NodeId> connectedNodes;
    private final Map<NodeId, Double> connectionLatencies; // milliseconds
    private final Map<NodeId, Double> connectionBandwidths; // Mbps

    public NetworkTopology(Set<NodeId> connectedNodes,
                          Map<NodeId, Double> connectionLatencies,
                          Map<NodeId, Double> connectionBandwidths) {
        this.connectedNodes = Objects.requireNonNull(connectedNodes, "Connected nodes cannot be null");
        this.connectionLatencies = Objects.requireNonNull(connectionLatencies, "Connection latencies cannot be null");
        this.connectionBandwidths = Objects.requireNonNull(connectionBandwidths, "Connection bandwidths cannot be null");
        validateTopology();
    }

    private void validateTopology() {
        // Ensure all connected nodes have latency and bandwidth information
        for (NodeId nodeId : connectedNodes) {
            if (!connectionLatencies.containsKey(nodeId)) {
                throw new IllegalArgumentException("Missing latency information for connected node: " + nodeId);
            }
            if (!connectionBandwidths.containsKey(nodeId)) {
                throw new IllegalArgumentException("Missing bandwidth information for connected node: " + nodeId);
            }
        }
    }

    // Getters
    public Set<NodeId> getConnectedNodes() { return connectedNodes; }
    public Map<NodeId, Double> getConnectionLatencies() { return connectionLatencies; }
    public Map<NodeId, Double> getConnectionBandwidths() { return connectionBandwidths; }

    public boolean isConnectedTo(NodeId nodeId) {
        return connectedNodes.contains(nodeId);
    }
    
    public boolean isConnected(NodeId from, NodeId to) {
        // Check if 'from' node (this topology's owner) is connected to 'to' node
        return connectedNodes.contains(to);
    }

    public double getLatencyTo(NodeId nodeId) {
        return connectionLatencies.getOrDefault(nodeId, Double.MAX_VALUE);
    }

    public double getBandwidthTo(NodeId nodeId) {
        return connectionBandwidths.getOrDefault(nodeId, 0.0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkTopology that = (NetworkTopology) o;
        return Objects.equals(connectedNodes, that.connectedNodes) &&
               Objects.equals(connectionLatencies, that.connectionLatencies) &&
               Objects.equals(connectionBandwidths, that.connectionBandwidths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectedNodes, connectionLatencies, connectionBandwidths);
    }

    @Override
    public String toString() {
        return "NetworkTopology{" +
               "connectedNodes=" + connectedNodes +
               ", connectionLatencies=" + connectionLatencies +
               ", connectionBandwidths=" + connectionBandwidths +
               '}';
    }
}