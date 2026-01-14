package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.Set;

/**
 * Configuration model for nodes in the distributed telecom system.
 * Contains node characteristics based on the dataset specifications.
 */
public class NodeConfiguration {
    private final NodeId nodeId;
    private final NodeLayer layer;
    private final NodeMetrics baselineMetrics;
    private final Set<ServiceType> supportedServices;
    private final FailureModel failureModel;
    private final ResourceLimits resourceLimits;
    private final NetworkTopology networkTopology;

    public NodeConfiguration(NodeId nodeId, NodeLayer layer, NodeMetrics baselineMetrics,
                           Set<ServiceType> supportedServices, FailureModel failureModel,
                           ResourceLimits resourceLimits, NetworkTopology networkTopology) {
        this.nodeId = Objects.requireNonNull(nodeId, "NodeId cannot be null");
        this.layer = Objects.requireNonNull(layer, "NodeLayer cannot be null");
        this.baselineMetrics = Objects.requireNonNull(baselineMetrics, "BaselineMetrics cannot be null");
        this.supportedServices = Objects.requireNonNull(supportedServices, "SupportedServices cannot be null");
        this.failureModel = Objects.requireNonNull(failureModel, "FailureModel cannot be null");
        this.resourceLimits = Objects.requireNonNull(resourceLimits, "ResourceLimits cannot be null");
        this.networkTopology = Objects.requireNonNull(networkTopology, "NetworkTopology cannot be null");
    }

    // Getters
    public NodeId getNodeId() { return nodeId; }
    public NodeLayer getLayer() { return layer; }
    public NodeMetrics getBaselineMetrics() { return baselineMetrics; }
    public Set<ServiceType> getSupportedServices() { return supportedServices; }
    public FailureModel getFailureModel() { return failureModel; }
    public ResourceLimits getResourceLimits() { return resourceLimits; }
    public NetworkTopology getNetworkTopology() { return networkTopology; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeConfiguration that = (NodeConfiguration) o;
        return Objects.equals(nodeId, that.nodeId) &&
               layer == that.layer &&
               Objects.equals(baselineMetrics, that.baselineMetrics) &&
               Objects.equals(supportedServices, that.supportedServices) &&
               Objects.equals(failureModel, that.failureModel) &&
               Objects.equals(resourceLimits, that.resourceLimits) &&
               Objects.equals(networkTopology, that.networkTopology);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, layer, baselineMetrics, supportedServices, 
                          failureModel, resourceLimits, networkTopology);
    }

    @Override
    public String toString() {
        return "NodeConfiguration{" +
               "nodeId=" + nodeId +
               ", layer=" + layer +
               ", baselineMetrics=" + baselineMetrics +
               ", supportedServices=" + supportedServices +
               ", failureModel=" + failureModel +
               ", resourceLimits=" + resourceLimits +
               ", networkTopology=" + networkTopology +
               '}';
    }
}