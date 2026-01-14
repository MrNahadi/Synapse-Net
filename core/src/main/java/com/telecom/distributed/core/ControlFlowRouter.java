package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Control flow router with performance optimization for edge-core-cloud topology.
 * Implements intelligent routing algorithms to minimize latency and maximize throughput.
 */
public class ControlFlowRouter {
    
    private final Map<NodeId, NodeConfiguration> nodeConfigurations;
    private final RoutingTable routingTable;
    private final PerformanceOptimizer performanceOptimizer;
    
    public ControlFlowRouter() {
        this.nodeConfigurations = new HashMap<>();
        this.routingTable = new RoutingTable();
        this.performanceOptimizer = new PerformanceOptimizer();
        initializeRoutingTable();
    }
    
    /**
     * Updates node configurations for routing decisions.
     * @param configurations Node configurations
     */
    public void updateNodeConfigurations(Map<NodeId, NodeConfiguration> configurations) {
        this.nodeConfigurations.clear();
        this.nodeConfigurations.putAll(configurations);
        updateRoutingTable();
    }
    
    /**
     * Initializes the routing table with optimal paths.
     */
    private void initializeRoutingTable() {
        // Define optimal routing paths based on topology
        routingTable.addRoute(new NodeId("Edge1"), new NodeId("Core1"), 12.0);
        routingTable.addRoute(new NodeId("Edge1"), new NodeId("Core2"), 15.0);
        routingTable.addRoute(new NodeId("Edge2"), new NodeId("Core1"), 18.0);
        routingTable.addRoute(new NodeId("Edge2"), new NodeId("Core2"), 16.0);
        routingTable.addRoute(new NodeId("Core1"), new NodeId("Cloud1"), 20.0);
        routingTable.addRoute(new NodeId("Core2"), new NodeId("Cloud1"), 22.0);
        
        // Add reverse routes
        routingTable.addRoute(new NodeId("Core1"), new NodeId("Edge1"), 12.0);
        routingTable.addRoute(new NodeId("Core2"), new NodeId("Edge1"), 15.0);
        routingTable.addRoute(new NodeId("Core1"), new NodeId("Edge2"), 18.0);
        routingTable.addRoute(new NodeId("Core2"), new NodeId("Edge2"), 16.0);
        routingTable.addRoute(new NodeId("Cloud1"), new NodeId("Core1"), 20.0);
        routingTable.addRoute(new NodeId("Cloud1"), new NodeId("Core2"), 22.0);
    }
    
    /**
     * Updates routing table based on current node configurations.
     */
    private void updateRoutingTable() {
        // Recalculate routes based on current node performance
        for (Map.Entry<NodeId, NodeConfiguration> entry : nodeConfigurations.entrySet()) {
            NodeId nodeId = entry.getKey();
            NodeMetrics metrics = entry.getValue().getBaselineMetrics();
            
            // Update route costs based on current latency
            routingTable.updateNodeLatency(nodeId, metrics.getLatency());
        }
    }
    
    /**
     * Routes a request from source to destination with performance optimization.
     * @param source Source node
     * @param destination Destination node
     * @param request Request to route
     * @return Routing result with optimal path
     */
    public RoutingResult routeRequest(NodeId source, NodeId destination, Object request) {
        // Find optimal path
        List<NodeId> optimalPath = findOptimalPath(source, destination);
        
        if (optimalPath.isEmpty()) {
            return new RoutingResult(false, "No route found", Collections.emptyList(), 0.0);
        }
        
        // Calculate total latency
        double totalLatency = calculatePathLatency(optimalPath);
        
        // Apply performance optimizations
        optimalPath = performanceOptimizer.optimizePath(optimalPath, nodeConfigurations);
        
        return new RoutingResult(true, "Route found", optimalPath, totalLatency);
    }
    
    /**
     * Finds the optimal path between source and destination nodes.
     * Uses Dijkstra's algorithm with performance-weighted edges.
     */
    private List<NodeId> findOptimalPath(NodeId source, NodeId destination) {
        if (source.equals(destination)) {
            return Collections.singletonList(source);
        }
        
        // Use Dijkstra's algorithm
        Map<NodeId, Double> distances = new HashMap<>();
        Map<NodeId, NodeId> previous = new HashMap<>();
        Set<NodeId> unvisited = new HashSet<>();
        
        // Initialize distances
        for (NodeId nodeId : getAllNodes()) {
            distances.put(nodeId, Double.MAX_VALUE);
            unvisited.add(nodeId);
        }
        distances.put(source, 0.0);
        
        while (!unvisited.isEmpty()) {
            // Find node with minimum distance
            NodeId current = unvisited.stream()
                .min(Comparator.comparing(distances::get))
                .orElse(null);
            
            if (current == null || distances.get(current) == Double.MAX_VALUE) {
                break;
            }
            
            unvisited.remove(current);
            
            if (current.equals(destination)) {
                break;
            }
            
            // Update distances to neighbors
            for (NodeId neighbor : routingTable.getNeighbors(current)) {
                if (unvisited.contains(neighbor)) {
                    double edgeWeight = routingTable.getLatency(current, neighbor);
                    double alternativeDistance = distances.get(current) + edgeWeight;
                    
                    if (alternativeDistance < distances.get(neighbor)) {
                        distances.put(neighbor, alternativeDistance);
                        previous.put(neighbor, current);
                    }
                }
            }
        }
        
        // Reconstruct path
        return reconstructPath(previous, source, destination);
    }
    
    /**
     * Reconstructs the path from source to destination.
     */
    private List<NodeId> reconstructPath(Map<NodeId, NodeId> previous, NodeId source, NodeId destination) {
        List<NodeId> path = new ArrayList<>();
        NodeId current = destination;
        
        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }
        
        return path.isEmpty() || !path.get(0).equals(source) ? Collections.emptyList() : path;
    }
    
    /**
     * Calculates total latency for a given path.
     */
    private double calculatePathLatency(List<NodeId> path) {
        double totalLatency = 0.0;
        
        for (int i = 0; i < path.size() - 1; i++) {
            NodeId from = path.get(i);
            NodeId to = path.get(i + 1);
            totalLatency += routingTable.getLatency(from, to);
        }
        
        return totalLatency;
    }
    
    /**
     * Gets all nodes in the system.
     */
    private Set<NodeId> getAllNodes() {
        Set<NodeId> allNodes = new HashSet<>();
        allNodes.add(new NodeId("Edge1"));
        allNodes.add(new NodeId("Edge2"));
        allNodes.add(new NodeId("Core1"));
        allNodes.add(new NodeId("Core2"));
        allNodes.add(new NodeId("Cloud1"));
        return allNodes;
    }
    
    /**
     * Gets the routing table.
     * @return Current routing table
     */
    public RoutingTable getRoutingTable() {
        return routingTable;
    }
    
    /**
     * Routing table implementation.
     */
    public static class RoutingTable {
        private final Map<NodeId, Map<NodeId, Double>> routes;
        
        public RoutingTable() {
            this.routes = new HashMap<>();
        }
        
        public void addRoute(NodeId from, NodeId to, double latency) {
            routes.computeIfAbsent(from, k -> new HashMap<>()).put(to, latency);
        }
        
        public double getLatency(NodeId from, NodeId to) {
            return routes.getOrDefault(from, Collections.emptyMap()).getOrDefault(to, Double.MAX_VALUE);
        }
        
        public Set<NodeId> getNeighbors(NodeId node) {
            return routes.getOrDefault(node, Collections.emptyMap()).keySet();
        }
        
        public void updateNodeLatency(NodeId node, double baseLatency) {
            // Update all routes involving this node with its current base latency
            for (Map.Entry<NodeId, Map<NodeId, Double>> entry : routes.entrySet()) {
                if (entry.getKey().equals(node)) {
                    // Update outgoing routes
                    for (Map.Entry<NodeId, Double> route : entry.getValue().entrySet()) {
                        double currentLatency = route.getValue();
                        // Adjust based on node's current performance
                        route.setValue(Math.max(currentLatency * 0.9, baseLatency));
                    }
                }
            }
        }
    }
    
    /**
     * Performance optimizer for routing paths.
     */
    public static class PerformanceOptimizer {
        
        public List<NodeId> optimizePath(List<NodeId> originalPath, 
                                       Map<NodeId, NodeConfiguration> nodeConfigurations) {
            if (originalPath.size() <= 2) {
                return originalPath; // No optimization needed for direct routes
            }
            
            // Check if any intermediate nodes are overloaded
            List<NodeId> optimizedPath = new ArrayList<>(originalPath);
            
            for (int i = 1; i < optimizedPath.size() - 1; i++) {
                NodeId intermediateNode = optimizedPath.get(i);
                NodeConfiguration config = nodeConfigurations.get(intermediateNode);
                
                if (config != null && isNodeOverloaded(config)) {
                    // Try to find alternative path bypassing this node
                    NodeId previous = optimizedPath.get(i - 1);
                    NodeId next = optimizedPath.get(i + 1);
                    
                    // For simplicity, keep original path but mark for monitoring
                    // In a real implementation, this would find alternative routes
                }
            }
            
            return optimizedPath;
        }
        
        private boolean isNodeOverloaded(NodeConfiguration config) {
            NodeMetrics metrics = config.getBaselineMetrics();
            // Consider node overloaded if CPU > 65% or memory > 80% of limit
            return metrics.getCpuUtilization() > 65.0 || 
                   metrics.getMemoryUsage() > (config.getResourceLimits().getMaxMemoryUsage() * 0.8);
        }
    }
    
    /**
     * Result of a routing operation.
     */
    public static class RoutingResult {
        private final boolean success;
        private final String message;
        private final List<NodeId> path;
        private final double totalLatency;
        
        public RoutingResult(boolean success, String message, List<NodeId> path, double totalLatency) {
            this.success = success;
            this.message = message;
            this.path = new ArrayList<>(path);
            this.totalLatency = totalLatency;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<NodeId> getPath() { return Collections.unmodifiableList(path); }
        public double getTotalLatency() { return totalLatency; }
        
        @Override
        public String toString() {
            return "RoutingResult{" +
                   "success=" + success +
                   ", message='" + message + '\'' +
                   ", path=" + path +
                   ", totalLatency=" + totalLatency + "ms" +
                   '}';
        }
    }
}