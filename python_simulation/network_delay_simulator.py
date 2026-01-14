"""
Network delay simulation for realistic communication latency modeling.
Simulates variable delays, jitter, and packet loss between nodes.
"""

import random
import time
import logging
from typing import Dict, List, Tuple
from dataclasses import dataclass
from collections import defaultdict

from models import NodeId, NetworkDelay


@dataclass
class NetworkCondition:
    """Current network condition affecting delays"""
    congestion_level: float  # 0.0-1.0
    packet_loss_rate: float  # 0.0-1.0
    jitter_multiplier: float  # 1.0 = normal jitter
    start_time: float
    duration: float


class NetworkDelaySimulator:
    """
    Network delay simulator that models realistic communication delays
    between nodes in the distributed system.
    """
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        
        # Initialize baseline delays between nodes based on topology
        self.baseline_delays = self._initialize_baseline_delays()
        
        # Current network conditions
        self.current_conditions: List[NetworkCondition] = []
        
        # Delay history for analysis
        self.delay_history = defaultdict(list)
        
        # Configuration
        self.base_jitter_range = 0.002  # ±2ms base jitter
        self.congestion_delay_multiplier = 2.0  # Max delay multiplier under congestion
        self.packet_loss_retry_delay = 0.050  # 50ms retry delay for lost packets
        
        self.logger.info("NetworkDelaySimulator initialized with %d node pairs", 
                        len(self.baseline_delays))
    
    def _initialize_baseline_delays(self) -> Dict[Tuple[NodeId, NodeId], NetworkDelay]:
        """Initialize baseline network delays between all node pairs"""
        delays = {}
        
        # Define baseline delays based on network topology and node characteristics
        node_latencies = {
            NodeId.EDGE1: 12.0,   # ms
            NodeId.EDGE2: 15.0,   # ms
            NodeId.CORE1: 8.0,    # ms
            NodeId.CORE2: 10.0,   # ms
            NodeId.CLOUD1: 22.0   # ms
        }
        
        # Calculate delays between all node pairs
        for source in NodeId:
            for destination in NodeId:
                if source == destination:
                    # Self-communication (local)
                    delay = NetworkDelay(
                        source=source,
                        destination=destination,
                        base_delay=0.1,  # 0.1ms local delay
                        jitter=0.05,     # 0.05ms jitter
                        packet_loss_rate=0.0001  # Very low local packet loss
                    )
                else:
                    # Inter-node communication
                    source_latency = node_latencies[source]
                    dest_latency = node_latencies[destination]
                    
                    # Base delay is sum of node latencies plus network propagation
                    base_delay = (source_latency + dest_latency) / 2.0
                    
                    # Add topology-based adjustments
                    topology_adjustment = self._get_topology_adjustment(source, destination)
                    base_delay *= topology_adjustment
                    
                    # Jitter based on distance and network quality
                    jitter = base_delay * 0.1  # 10% of base delay as jitter
                    
                    # Packet loss based on network path
                    packet_loss = self._get_baseline_packet_loss(source, destination)
                    
                    delay = NetworkDelay(
                        source=source,
                        destination=destination,
                        base_delay=base_delay,
                        jitter=jitter,
                        packet_loss_rate=packet_loss
                    )
                
                delays[(source, destination)] = delay
        
        return delays
    
    def _get_topology_adjustment(self, source: NodeId, destination: NodeId) -> float:
        """Get topology-based delay adjustment factor"""
        # Define network layers
        edge_nodes = {NodeId.EDGE1, NodeId.EDGE2}
        core_nodes = {NodeId.CORE1, NodeId.CORE2}
        cloud_nodes = {NodeId.CLOUD1}
        
        source_layer = self._get_node_layer(source)
        dest_layer = self._get_node_layer(destination)
        
        # Same layer communication is faster
        if source_layer == dest_layer:
            return 0.8  # 20% faster
        
        # Edge to Core communication
        if (source in edge_nodes and destination in core_nodes) or \
           (source in core_nodes and destination in edge_nodes):
            return 1.0  # Normal delay
        
        # Core to Cloud communication
        if (source in core_nodes and destination in cloud_nodes) or \
           (source in cloud_nodes and destination in core_nodes):
            return 1.2  # 20% slower
        
        # Edge to Cloud communication (through core)
        if (source in edge_nodes and destination in cloud_nodes) or \
           (source in cloud_nodes and destination in edge_nodes):
            return 1.5  # 50% slower (multi-hop)
        
        return 1.0
    
    def _get_node_layer(self, node: NodeId) -> str:
        """Get the network layer of a node"""
        if node in {NodeId.EDGE1, NodeId.EDGE2}:
            return "edge"
        elif node in {NodeId.CORE1, NodeId.CORE2}:
            return "core"
        else:  # CLOUD1
            return "cloud"
    
    def _get_baseline_packet_loss(self, source: NodeId, destination: NodeId) -> float:
        """Get baseline packet loss rate between nodes"""
        # Higher packet loss for longer paths and less reliable nodes
        node_reliability = {
            NodeId.EDGE1: 0.99,   # 99% reliability (crash failures)
            NodeId.EDGE2: 0.97,   # 97% reliability (omission failures)
            NodeId.CORE1: 0.95,   # 95% reliability (Byzantine failures)
            NodeId.CORE2: 0.98,   # 98% reliability (crash failures)
            NodeId.CLOUD1: 0.96   # 96% reliability (omission failures)
        }
        
        source_reliability = node_reliability[source]
        dest_reliability = node_reliability[destination]
        
        # Combined reliability affects packet loss
        combined_reliability = source_reliability * dest_reliability
        base_packet_loss = (1.0 - combined_reliability) * 0.1  # Scale to reasonable range
        
        # Add topology-based packet loss
        topology_factor = self._get_topology_adjustment(source, destination)
        adjusted_packet_loss = base_packet_loss * topology_factor
        
        return min(0.05, adjusted_packet_loss)  # Cap at 5% packet loss
    
    def get_delay_to_node(self, destination: NodeId, source: NodeId = None) -> float:
        """
        Get current network delay to a destination node.
        
        Args:
            destination: Target node
            source: Source node (defaults to a random node if not specified)
            
        Returns:
            Network delay in seconds
        """
        if source is None:
            # Default to EDGE1 as source for client requests
            source = NodeId.EDGE1
        
        # Get baseline delay configuration
        delay_config = self.baseline_delays.get((source, destination))
        if not delay_config:
            self.logger.warning("No delay configuration for %s -> %s", source, destination)
            return 0.010  # Default 10ms delay
        
        # Calculate base delay with jitter
        base_delay = delay_config.base_delay / 1000.0  # Convert ms to seconds
        jitter = random.uniform(-delay_config.jitter, delay_config.jitter) / 1000.0
        current_delay = max(0.001, base_delay + jitter)  # Minimum 1ms delay
        
        # Apply current network conditions
        current_delay = self._apply_network_conditions(current_delay, delay_config)
        
        # Simulate packet loss with retries
        if random.random() < delay_config.packet_loss_rate:
            # Packet lost, add retry delay
            retry_delay = self.packet_loss_retry_delay
            current_delay += retry_delay
            self.logger.debug("Packet loss on %s -> %s, adding retry delay", source, destination)
        
        # Record delay for analysis
        self.delay_history[(source, destination)].append(current_delay)
        
        return current_delay
    
    def _apply_network_conditions(self, base_delay: float, delay_config: NetworkDelay) -> float:
        """Apply current network conditions to base delay"""
        current_time = time.time()
        adjusted_delay = base_delay
        
        for condition in self.current_conditions:
            if condition.start_time <= current_time <= condition.start_time + condition.duration:
                # Apply congestion delay
                congestion_multiplier = 1.0 + (condition.congestion_level * (self.congestion_delay_multiplier - 1.0))
                adjusted_delay *= congestion_multiplier
                
                # Apply additional jitter
                additional_jitter = random.uniform(-self.base_jitter_range, self.base_jitter_range) * condition.jitter_multiplier
                adjusted_delay += additional_jitter
        
        return max(0.001, adjusted_delay)  # Minimum 1ms delay
    
    def inject_network_condition(self, congestion_level: float, duration: float, 
                                packet_loss_rate: float = None, jitter_multiplier: float = 1.0):
        """
        Inject a network condition that affects all communications.
        
        Args:
            congestion_level: Network congestion level (0.0-1.0)
            duration: Duration of the condition in seconds
            packet_loss_rate: Additional packet loss rate (optional)
            jitter_multiplier: Jitter multiplier (1.0 = normal)
        """
        condition = NetworkCondition(
            congestion_level=congestion_level,
            packet_loss_rate=packet_loss_rate or 0.0,
            jitter_multiplier=jitter_multiplier,
            start_time=time.time(),
            duration=duration
        )
        
        self.current_conditions.append(condition)
        
        self.logger.info("Injected network condition: congestion=%.2f, duration=%.1fs", 
                        congestion_level, duration)
    
    def simulate_network_partition(self, partitioned_nodes: List[NodeId], duration: float):
        """
        Simulate network partition by dramatically increasing delays to partitioned nodes.
        
        Args:
            partitioned_nodes: Nodes that are partitioned
            duration: Duration of partition in seconds
        """
        # Temporarily modify delays for partitioned nodes
        partition_start = time.time()
        
        for source in NodeId:
            for destination in partitioned_nodes:
                if source != destination and source not in partitioned_nodes:
                    # Increase delay dramatically for partition simulation
                    key = (source, destination)
                    if key in self.baseline_delays:
                        original_delay = self.baseline_delays[key].base_delay
                        self.baseline_delays[key].base_delay = original_delay * 100  # 100x delay
                        self.baseline_delays[key].packet_loss_rate = 0.9  # 90% packet loss
        
        # Schedule restoration
        def restore_partition():
            for source in NodeId:
                for destination in partitioned_nodes:
                    if source != destination and source not in partitioned_nodes:
                        key = (source, destination)
                        if key in self.baseline_delays:
                            # Restore original delay
                            self.baseline_delays[key].base_delay /= 100
                            self.baseline_delays[key].packet_loss_rate = self._get_baseline_packet_loss(source, destination)
        
        # Note: In a real implementation, you'd use a timer or scheduler
        # For simulation purposes, this would be handled by the main simulation loop
        
        self.logger.warning("Network partition simulated for nodes: %s (duration: %.1fs)", 
                          [node.value for node in partitioned_nodes], duration)
    
    def get_delay_statistics(self) -> Dict:
        """Get network delay statistics"""
        if not self.delay_history:
            return {}
        
        stats = {
            'total_measurements': sum(len(delays) for delays in self.delay_history.values()),
            'node_pair_stats': {}
        }
        
        for (source, destination), delays in self.delay_history.items():
            if delays:
                pair_key = f"{source.value}->{destination.value}"
                stats['node_pair_stats'][pair_key] = {
                    'count': len(delays),
                    'average_delay': sum(delays) / len(delays),
                    'min_delay': min(delays),
                    'max_delay': max(delays),
                    'jitter': max(delays) - min(delays)
                }
        
        return stats
    
    def get_current_network_conditions(self) -> List[NetworkCondition]:
        """Get currently active network conditions"""
        current_time = time.time()
        active_conditions = []
        
        for condition in self.current_conditions:
            if condition.start_time <= current_time <= condition.start_time + condition.duration:
                active_conditions.append(condition)
        
        return active_conditions
    
    def cleanup_expired_conditions(self):
        """Remove expired network conditions"""
        current_time = time.time()
        self.current_conditions = [
            condition for condition in self.current_conditions
            if current_time <= condition.start_time + condition.duration
        ]
    
    def reset(self):
        """Reset network delay simulator state"""
        self.current_conditions.clear()
        self.delay_history.clear()
        self.baseline_delays = self._initialize_baseline_delays()
        self.logger.info("NetworkDelaySimulator state reset")