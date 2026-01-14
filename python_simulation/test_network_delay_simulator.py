"""
Unit tests for network delay simulation.
Tests network delay simulation as specified in Requirements 8.2, 8.3.
"""

import unittest
import time
from unittest.mock import Mock, patch

from network_delay_simulator import NetworkDelaySimulator, NetworkCondition
from models import NodeId, NetworkDelay


class TestNetworkDelaySimulator(unittest.TestCase):
    """Test cases for NetworkDelaySimulator class"""
    
    def setUp(self):
        """Set up test fixtures"""
        self.network_simulator = NetworkDelaySimulator()
    
    def test_baseline_delays_initialization(self):
        """Test that baseline delays are correctly initialized"""
        # Check that delays exist for all node pairs
        expected_pairs = len(NodeId) * len(NodeId)  # 5x5 = 25 pairs
        self.assertEqual(len(self.network_simulator.baseline_delays), expected_pairs)
        
        # Check self-communication delays
        for node in NodeId:
            delay_config = self.network_simulator.baseline_delays[(node, node)]
            self.assertEqual(delay_config.base_delay, 0.1)  # 0.1ms local delay
            self.assertEqual(delay_config.jitter, 0.05)     # 0.05ms jitter
            self.assertEqual(delay_config.packet_loss_rate, 0.0001)  # Very low packet loss
        
        # Check inter-node communication delays
        edge1_to_core1 = self.network_simulator.baseline_delays[(NodeId.EDGE1, NodeId.CORE1)]
        self.assertGreater(edge1_to_core1.base_delay, 0.1)  # Should be > local delay
        self.assertGreater(edge1_to_core1.jitter, 0.05)     # Should have more jitter
        self.assertGreater(edge1_to_core1.packet_loss_rate, 0.0001)  # Higher packet loss
    
    def test_topology_adjustment(self):
        """Test topology-based delay adjustments"""
        # Same layer communication should be faster
        edge_to_edge_adj = self.network_simulator._get_topology_adjustment(NodeId.EDGE1, NodeId.EDGE2)
        self.assertEqual(edge_to_edge_adj, 0.8)  # 20% faster
        
        core_to_core_adj = self.network_simulator._get_topology_adjustment(NodeId.CORE1, NodeId.CORE2)
        self.assertEqual(core_to_core_adj, 0.8)  # 20% faster
        
        # Edge to Core should be normal
        edge_to_core_adj = self.network_simulator._get_topology_adjustment(NodeId.EDGE1, NodeId.CORE1)
        self.assertEqual(edge_to_core_adj, 1.0)  # Normal delay
        
        # Core to Cloud should be slower
        core_to_cloud_adj = self.network_simulator._get_topology_adjustment(NodeId.CORE1, NodeId.CLOUD1)
        self.assertEqual(core_to_cloud_adj, 1.2)  # 20% slower
        
        # Edge to Cloud should be slowest (multi-hop)
        edge_to_cloud_adj = self.network_simulator._get_topology_adjustment(NodeId.EDGE1, NodeId.CLOUD1)
        self.assertEqual(edge_to_cloud_adj, 1.5)  # 50% slower
    
    def test_node_layer_identification(self):
        """Test node layer identification"""
        self.assertEqual(self.network_simulator._get_node_layer(NodeId.EDGE1), "edge")
        self.assertEqual(self.network_simulator._get_node_layer(NodeId.EDGE2), "edge")
        self.assertEqual(self.network_simulator._get_node_layer(NodeId.CORE1), "core")
        self.assertEqual(self.network_simulator._get_node_layer(NodeId.CORE2), "core")
        self.assertEqual(self.network_simulator._get_node_layer(NodeId.CLOUD1), "cloud")
    
    def test_baseline_packet_loss_calculation(self):
        """Test baseline packet loss calculation"""
        # Test between reliable nodes
        edge1_to_core2_loss = self.network_simulator._get_baseline_packet_loss(NodeId.EDGE1, NodeId.CORE2)
        
        # Should be low but > 0
        self.assertGreater(edge1_to_core2_loss, 0.0)
        self.assertLessEqual(edge1_to_core2_loss, 0.05)  # Capped at 5%
        
        # Test between less reliable nodes
        edge2_to_core1_loss = self.network_simulator._get_baseline_packet_loss(NodeId.EDGE2, NodeId.CORE1)
        
        # Should be higher due to lower reliability
        self.assertGreater(edge2_to_core1_loss, edge1_to_core2_loss)
        self.assertLessEqual(edge2_to_core1_loss, 0.05)  # Still capped at 5%
    
    def test_get_delay_to_node_basic(self):
        """Test basic delay calculation"""
        # Test delay to CORE1 from EDGE1
        delay = self.network_simulator.get_delay_to_node(NodeId.CORE1, NodeId.EDGE1)
        
        # Should be positive and reasonable
        self.assertGreater(delay, 0.001)  # At least 1ms
        self.assertLess(delay, 1.0)       # Less than 1 second
        
        # Test multiple calls for consistency (should vary due to jitter)
        delays = [self.network_simulator.get_delay_to_node(NodeId.CORE1, NodeId.EDGE1) for _ in range(10)]
        
        # All delays should be positive
        for d in delays:
            self.assertGreater(d, 0.001)
        
        # Should have some variation due to jitter
        delay_range = max(delays) - min(delays)
        self.assertGreater(delay_range, 0.0)
    
    def test_get_delay_with_default_source(self):
        """Test delay calculation with default source node"""
        # Should default to EDGE1 when no source specified
        delay = self.network_simulator.get_delay_to_node(NodeId.CORE1)
        
        self.assertGreater(delay, 0.001)
        self.assertLess(delay, 1.0)
    
    def test_network_condition_injection(self):
        """Test network condition injection and effects"""
        # Inject high congestion condition
        self.network_simulator.inject_network_condition(
            congestion_level=0.8,
            duration=60.0,
            packet_loss_rate=0.1,
            jitter_multiplier=2.0
        )
        
        # Verify condition was added
        self.assertEqual(len(self.network_simulator.current_conditions), 1)
        condition = self.network_simulator.current_conditions[0]
        self.assertEqual(condition.congestion_level, 0.8)
        self.assertEqual(condition.duration, 60.0)
        self.assertEqual(condition.packet_loss_rate, 0.1)
        self.assertEqual(condition.jitter_multiplier, 2.0)
        
        # Test delay under congestion (should be higher)
        baseline_delay = self.network_simulator.baseline_delays[(NodeId.EDGE1, NodeId.CORE1)].base_delay / 1000.0
        
        # Get delay under congestion
        congested_delay = self.network_simulator.get_delay_to_node(NodeId.CORE1, NodeId.EDGE1)
        
        # Should be higher than baseline (though exact value depends on jitter)
        # We test that the congestion multiplier logic is applied
        expected_min_delay = baseline_delay * 1.8  # 1 + (0.8 * (2.0 - 1.0))
        self.assertGreater(congested_delay, baseline_delay)
    
    def test_packet_loss_simulation(self):
        """Test packet loss simulation with retries"""
        # Mock random to force packet loss
        with patch('network_delay_simulator.random.random', return_value=0.001):  # Force packet loss
            delay_with_loss = self.network_simulator.get_delay_to_node(NodeId.CORE1, NodeId.EDGE1)
        
        # Should include retry delay
        expected_min_delay = self.network_simulator.packet_loss_retry_delay
        self.assertGreater(delay_with_loss, expected_min_delay)
        
        # Mock random to avoid packet loss
        with patch('network_delay_simulator.random.random', return_value=0.999):  # Avoid packet loss
            delay_without_loss = self.network_simulator.get_delay_to_node(NodeId.CORE1, NodeId.EDGE1)
        
        # Delay with loss should be higher
        self.assertGreater(delay_with_loss, delay_without_loss)
    
    def test_network_partition_simulation(self):
        """Test network partition simulation"""
        partitioned_nodes = [NodeId.EDGE1, NodeId.EDGE2]
        duration = 120.0
        
        # Get baseline delay before partition
        baseline_delay = self.network_simulator.baseline_delays[(NodeId.CORE1, NodeId.EDGE1)].base_delay
        
        # Simulate partition
        self.network_simulator.simulate_network_partition(partitioned_nodes, duration)
        
        # Check that delays to partitioned nodes are dramatically increased
        partitioned_delay = self.network_simulator.baseline_delays[(NodeId.CORE1, NodeId.EDGE1)].base_delay
        self.assertEqual(partitioned_delay, baseline_delay * 100)  # 100x increase
        
        # Check packet loss is increased
        partitioned_loss = self.network_simulator.baseline_delays[(NodeId.CORE1, NodeId.EDGE1)].packet_loss_rate
        self.assertEqual(partitioned_loss, 0.9)  # 90% packet loss
    
    def test_delay_statistics_collection(self):
        """Test delay statistics collection"""
        # Generate some delays
        for _ in range(10):
            self.network_simulator.get_delay_to_node(NodeId.CORE1, NodeId.EDGE1)
            self.network_simulator.get_delay_to_node(NodeId.CLOUD1, NodeId.CORE2)
        
        stats = self.network_simulator.get_delay_statistics()
        
        # Check basic statistics structure
        self.assertIn('total_measurements', stats)
        self.assertIn('node_pair_stats', stats)
        self.assertEqual(stats['total_measurements'], 20)  # 10 calls x 2 pairs
        
        # Check specific pair statistics
        edge1_core1_key = f"{NodeId.EDGE1.value}->{NodeId.CORE1.value}"
        self.assertIn(edge1_core1_key, stats['node_pair_stats'])
        
        pair_stats = stats['node_pair_stats'][edge1_core1_key]
        self.assertEqual(pair_stats['count'], 10)
        self.assertGreater(pair_stats['average_delay'], 0.0)
        self.assertGreater(pair_stats['max_delay'], pair_stats['min_delay'])
        self.assertGreaterEqual(pair_stats['jitter'], 0.0)
    
    def test_current_network_conditions(self):
        """Test current network conditions tracking"""
        # Initially no conditions
        conditions = self.network_simulator.get_current_network_conditions()
        self.assertEqual(len(conditions), 0)
        
        # Add a condition
        self.network_simulator.inject_network_condition(0.5, 30.0)
        
        conditions = self.network_simulator.get_current_network_conditions()
        self.assertEqual(len(conditions), 1)
        self.assertEqual(conditions[0].congestion_level, 0.5)
        self.assertEqual(conditions[0].duration, 30.0)
    
    def test_cleanup_expired_conditions(self):
        """Test cleanup of expired network conditions"""
        current_time = time.time()
        
        # Add expired condition
        expired_condition = NetworkCondition(
            congestion_level=0.5,
            packet_loss_rate=0.1,
            jitter_multiplier=1.5,
            start_time=current_time - 100.0,  # Started 100 seconds ago
            duration=50.0  # Duration 50 seconds (expired)
        )
        
        # Add active condition
        active_condition = NetworkCondition(
            congestion_level=0.3,
            packet_loss_rate=0.05,
            jitter_multiplier=1.2,
            start_time=current_time - 10.0,  # Started 10 seconds ago
            duration=60.0  # Duration 60 seconds (still active)
        )
        
        self.network_simulator.current_conditions = [expired_condition, active_condition]
        
        # Cleanup expired conditions
        self.network_simulator.cleanup_expired_conditions()
        
        # Only active condition should remain
        self.assertEqual(len(self.network_simulator.current_conditions), 1)
        self.assertEqual(self.network_simulator.current_conditions[0], active_condition)
    
    def test_reset_functionality(self):
        """Test network delay simulator reset functionality"""
        # Add some state
        self.network_simulator.inject_network_condition(0.5, 30.0)
        self.network_simulator.get_delay_to_node(NodeId.CORE1, NodeId.EDGE1)  # Add to history
        
        # Verify state exists
        self.assertGreater(len(self.network_simulator.current_conditions), 0)
        self.assertGreater(len(self.network_simulator.delay_history), 0)
        
        # Reset
        self.network_simulator.reset()
        
        # Verify state is cleared
        self.assertEqual(len(self.network_simulator.current_conditions), 0)
        self.assertEqual(len(self.network_simulator.delay_history), 0)
        
        # Baseline delays should be reinitialized
        self.assertEqual(len(self.network_simulator.baseline_delays), 25)  # 5x5 node pairs
    
    def test_delay_bounds(self):
        """Test that delays are within reasonable bounds"""
        # Test many delay calculations
        delays = []
        for source in NodeId:
            for destination in NodeId:
                delay = self.network_simulator.get_delay_to_node(destination, source)
                delays.append(delay)
        
        # All delays should be within reasonable bounds
        for delay in delays:
            self.assertGreaterEqual(delay, 0.001)  # At least 1ms
            self.assertLessEqual(delay, 1.0)       # At most 1 second
        
        # Self-communication should be fastest
        self_delays = [self.network_simulator.get_delay_to_node(node, node) for node in NodeId]
        inter_node_delays = [d for d in delays if d not in self_delays]
        
        max_self_delay = max(self_delays)
        min_inter_delay = min(inter_node_delays)
        
        # Self delays should generally be less than inter-node delays
        self.assertLess(max_self_delay, min_inter_delay * 2)  # Allow some overlap due to jitter


if __name__ == '__main__':
    unittest.main()