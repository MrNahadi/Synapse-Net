"""
Unit tests for the main load balancer simulation framework.
Tests dynamic process allocation and integration components.
"""

import unittest
import time
from unittest.mock import Mock, patch, MagicMock

from load_balancer_simulation import LoadBalancerSimulation
from models import (
    NodeId, LoadBalancingStrategy, SimulationConfig, ServiceRequest,
    TrafficPattern, TrafficPatternType, NodeMetrics
)


class TestLoadBalancerSimulation(unittest.TestCase):
    """Test cases for LoadBalancerSimulation class"""
    
    def setUp(self):
        """Set up test fixtures"""
        self.config = SimulationConfig(
            simulation_duration=10.0,  # Short duration for tests
            time_step=0.1,
            failure_injection_enabled=True,
            network_delay_simulation_enabled=True,
            adaptive_migration_enabled=True,
            java_integration_enabled=False  # Disable for unit tests
        )
        
        self.simulation = LoadBalancerSimulation(self.config)
    
    def test_initialization(self):
        """Test simulation initialization"""
        # Check that simulation state is properly initialized
        self.assertEqual(len(self.simulation.state.active_nodes), 5)  # All 5 nodes active
        self.assertEqual(len(self.simulation.state.failed_nodes), 0)   # No failed nodes initially
        self.assertEqual(len(self.simulation.state.node_metrics), 5)   # Metrics for all nodes
        
        # Check that components are initialized based on config
        self.assertIsNotNone(self.simulation.failure_injector)
        self.assertIsNotNone(self.simulation.network_delay_simulator)
        self.assertIsNotNone(self.simulation.adaptive_migration_engine)
        self.assertIsNone(self.simulation.java_integration)  # Disabled in config
        
        # Check initial strategy and weights
        self.assertEqual(self.simulation.current_strategy, LoadBalancingStrategy.RESOURCE_AWARE)
        self.assertEqual(len(self.simulation.node_weights), 5)
    
    def test_node_weights_initialization(self):
        """Test node weights initialization based on characteristics"""
        weights = self.simulation.node_weights
        
        # All weights should be positive
        for weight in weights.values():
            self.assertGreater(weight, 0.0)
        
        # CORE1 should have good weight despite Byzantine failures
        # EDGE1 should have decent weight with crash failures
        # Weights should reflect throughput/latency ratio and reliability
        self.assertGreater(weights[NodeId.CORE1], 0.0)
        self.assertGreater(weights[NodeId.EDGE1], 0.0)
    
    def test_service_request_generation(self):
        """Test service request generation"""
        current_time = time.time()
        
        # Mock random to control request generation
        with patch('load_balancer_simulation.random.expovariate', return_value=3), \
             patch('load_balancer_simulation.random.uniform') as mock_uniform, \
             patch('load_balancer_simulation.random.randint') as mock_randint:
            
            mock_uniform.side_effect = [15.0, 2.0, 20.0, 1.5, 10.0, 1.0]  # CPU, memory requirements
            mock_randint.side_effect = [5, 7, 3, 8, 2, 6]  # Transaction load, priority
            
            requests = self.simulation._generate_service_requests(current_time)
        
        # Should generate 3 requests
        self.assertEqual(len(requests), 3)
        
        # Check request properties
        for request in requests:
            self.assertIsInstance(request, ServiceRequest)
            self.assertGreater(request.cpu_requirement, 0.0)
            self.assertGreater(request.memory_requirement, 0.0)
            self.assertGreater(request.transaction_load, 0)
            self.assertGreater(request.priority, 0)
            self.assertAlmostEqual(request.timestamp, current_time, places=1)
    
    def test_node_selection_strategies(self):
        """Test different node selection strategies"""
        request = ServiceRequest(
            service_id="test_service",
            cpu_requirement=10.0,
            memory_requirement=1.0,
            transaction_load=5,
            priority=5
        )
        
        # Test weighted round robin
        self.simulation.current_strategy = LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN
        node1 = self.simulation._select_node_for_request(request)
        self.assertIn(node1, NodeId)
        
        # Test least connections
        self.simulation.current_strategy = LoadBalancingStrategy.LEAST_CONNECTIONS
        node2 = self.simulation._select_node_for_request(request)
        self.assertIn(node2, NodeId)
        
        # Test resource aware
        self.simulation.current_strategy = LoadBalancingStrategy.RESOURCE_AWARE
        node3 = self.simulation._select_node_for_request(request)
        self.assertIn(node3, NodeId)
    
    def test_resource_aware_node_selection(self):
        """Test resource-aware node selection logic"""
        request = ServiceRequest(
            service_id="test_service",
            cpu_requirement=10.0,
            memory_requirement=1.0,
            transaction_load=5,
            priority=8
        )
        
        # Set one node as overloaded
        self.simulation.state.load_balancing_metrics.cpu_distribution[NodeId.EDGE1] = 40.0  # High load
        self.simulation.state.load_balancing_metrics.cpu_distribution[NodeId.EDGE2] = 5.0   # Low load
        
        selected_node = self.simulation._select_node_resource_aware(request, list(NodeId))
        
        # Should prefer less loaded nodes
        self.assertIsNotNone(selected_node)
        self.assertIn(selected_node, NodeId)
    
    def test_can_accommodate_request(self):
        """Test request accommodation checking"""
        request = ServiceRequest(
            service_id="test_service",
            cpu_requirement=10.0,
            memory_requirement=1.0,
            transaction_load=5,
            priority=5
        )
        
        # Test with low current load (should accommodate)
        self.simulation.state.load_balancing_metrics.cpu_distribution[NodeId.EDGE1] = 5.0
        self.simulation.state.load_balancing_metrics.memory_distribution[NodeId.EDGE1] = 1.0
        self.simulation.state.load_balancing_metrics.transaction_distribution[NodeId.EDGE1] = 10
        
        can_accommodate = self.simulation._can_accommodate_request(NodeId.EDGE1, request)
        self.assertTrue(can_accommodate)
        
        # Test with high current load (should not accommodate)
        self.simulation.state.load_balancing_metrics.cpu_distribution[NodeId.EDGE1] = 40.0  # Near capacity
        
        cannot_accommodate = self.simulation._can_accommodate_request(NodeId.EDGE1, request)
        self.assertFalse(cannot_accommodate)
    
    def test_node_score_calculation(self):
        """Test node score calculation for selection"""
        request = ServiceRequest(
            service_id="test_service",
            cpu_requirement=10.0,
            memory_requirement=1.0,
            transaction_load=5,
            priority=8
        )
        
        # Calculate scores for different nodes
        score_edge1 = self.simulation._calculate_node_score(NodeId.EDGE1, request)
        score_core1 = self.simulation._calculate_node_score(NodeId.CORE1, request)
        
        # Scores should be positive
        self.assertGreater(score_edge1, 0.0)
        self.assertGreater(score_core1, 0.0)
        
        # CORE1 should generally have higher score due to better performance
        # (though this depends on current load)
        self.assertIsInstance(score_core1, float)
    
    def test_network_delay_simulation(self):
        """Test network delay simulation integration"""
        # Mock network delay simulator
        self.simulation.network_delay_simulator = Mock()
        self.simulation.network_delay_simulator.get_delay_to_node.return_value = 0.015  # 15ms
        
        delay = self.simulation._simulate_network_delay(NodeId.CORE1)
        
        self.assertEqual(delay, 0.015)
        self.simulation.network_delay_simulator.get_delay_to_node.assert_called_once_with(NodeId.CORE1)
    
    def test_node_load_update(self):
        """Test node load update after request allocation"""
        request = ServiceRequest(
            service_id="test_service",
            cpu_requirement=10.0,
            memory_requirement=1.0,
            transaction_load=5,
            priority=5
        )
        
        # Get initial load
        initial_cpu = self.simulation.state.load_balancing_metrics.cpu_distribution[NodeId.EDGE1]
        initial_memory = self.simulation.state.load_balancing_metrics.memory_distribution[NodeId.EDGE1]
        initial_transactions = self.simulation.state.load_balancing_metrics.transaction_distribution[NodeId.EDGE1]
        initial_services = self.simulation.state.load_balancing_metrics.total_services
        
        # Update load
        self.simulation._update_node_load(NodeId.EDGE1, request)
        
        # Check that load was updated
        new_cpu = self.simulation.state.load_balancing_metrics.cpu_distribution[NodeId.EDGE1]
        new_memory = self.simulation.state.load_balancing_metrics.memory_distribution[NodeId.EDGE1]
        new_transactions = self.simulation.state.load_balancing_metrics.transaction_distribution[NodeId.EDGE1]
        new_services = self.simulation.state.load_balancing_metrics.total_services
        
        self.assertEqual(new_cpu, initial_cpu + request.cpu_requirement)
        self.assertEqual(new_memory, initial_memory + request.memory_requirement)
        self.assertEqual(new_transactions, initial_transactions + request.transaction_load)
        self.assertEqual(new_services, initial_services + 1)
    
    def test_load_balance_index_calculation(self):
        """Test load balance index calculation"""
        # Set up unbalanced load
        self.simulation.state.load_balancing_metrics.cpu_distribution = {
            NodeId.EDGE1: 40.0,  # High load
            NodeId.EDGE2: 5.0,   # Low load
            NodeId.CORE1: 20.0,  # Medium load
            NodeId.CORE2: 15.0,  # Medium load
            NodeId.CLOUD1: 10.0  # Low-medium load
        }
        
        self.simulation._update_load_balance_index()
        
        # Index should be < 1.0 for unbalanced load
        index = self.simulation.state.load_balancing_metrics.load_balance_index
        self.assertLess(index, 1.0)
        self.assertGreaterEqual(index, 0.0)
        
        # Set up balanced load
        balanced_load = 20.0
        for node in NodeId:
            self.simulation.state.load_balancing_metrics.cpu_distribution[node] = balanced_load
        
        self.simulation._update_load_balance_index()
        
        # Index should be 1.0 for perfectly balanced load
        balanced_index = self.simulation.state.load_balancing_metrics.load_balance_index
        self.assertAlmostEqual(balanced_index, 1.0, places=2)
    
    def test_process_service_request(self):
        """Test service request processing"""
        request = ServiceRequest(
            service_id="test_service",
            cpu_requirement=10.0,
            memory_requirement=1.0,
            transaction_load=5,
            priority=5
        )
        
        # Mock network delay
        self.simulation.network_delay_simulator = Mock()
        self.simulation.network_delay_simulator.get_delay_to_node.return_value = 0.015
        
        success, response_time = self.simulation._process_service_request(request)
        
        # Should succeed with available nodes
        self.assertTrue(success)
        self.assertGreater(response_time, 0.0)
        
        # Service should be allocated
        self.assertIn(request.service_id, self.simulation.state.service_allocations)
    
    def test_process_request_with_failed_nodes(self):
        """Test service request processing with failed nodes"""
        request = ServiceRequest(
            service_id="test_service",
            cpu_requirement=10.0,
            memory_requirement=1.0,
            transaction_load=5,
            priority=5
        )
        
        # Mark all nodes as failed
        self.simulation.state.failed_nodes = set(NodeId)
        self.simulation.state.active_nodes = set()
        
        success, response_time = self.simulation._process_service_request(request)
        
        # Should fail with no available nodes
        self.assertFalse(success)
        self.assertEqual(response_time, 0.0)
    
    def test_strategy_change(self):
        """Test load balancing strategy change"""
        # Initial strategy
        self.assertEqual(self.simulation.current_strategy, LoadBalancingStrategy.RESOURCE_AWARE)
        
        # Change strategy
        self.simulation.set_load_balancing_strategy(LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN)
        self.assertEqual(self.simulation.current_strategy, LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN)
        
        # Change to least connections
        self.simulation.set_load_balancing_strategy(LoadBalancingStrategy.LEAST_CONNECTIONS)
        self.assertEqual(self.simulation.current_strategy, LoadBalancingStrategy.LEAST_CONNECTIONS)
    
    def test_node_weights_update(self):
        """Test node weights update"""
        new_weights = {
            NodeId.EDGE1: 1.5,
            NodeId.CORE1: 2.0
        }
        
        self.simulation.update_node_weights(new_weights)
        
        # Weights should be updated
        self.assertEqual(self.simulation.node_weights[NodeId.EDGE1], 1.5)
        self.assertEqual(self.simulation.node_weights[NodeId.CORE1], 2.0)
    
    def test_performance_summary(self):
        """Test performance summary generation"""
        summary = self.simulation.get_performance_summary()
        
        # Check summary structure
        self.assertIn('active_nodes', summary)
        self.assertIn('failed_nodes', summary)
        self.assertIn('total_services', summary)
        self.assertIn('load_balance_index', summary)
        self.assertIn('total_migrations', summary)
        self.assertIn('cpu_distribution', summary)
        self.assertIn('memory_distribution', summary)
        
        # Check values
        self.assertEqual(summary['active_nodes'], 5)  # All nodes active initially
        self.assertEqual(summary['failed_nodes'], 0)  # No failed nodes initially
        self.assertIsInstance(summary['load_balance_index'], float)
    
    def test_current_state_access(self):
        """Test current state access"""
        state = self.simulation.get_current_state()
        
        # Should return the simulation state
        self.assertEqual(state, self.simulation.state)
        self.assertEqual(len(state.active_nodes), 5)
        self.assertEqual(len(state.failed_nodes), 0)
    
    @patch('time.sleep')  # Mock sleep to speed up test
    def test_short_simulation_run(self, mock_sleep):
        """Test a short simulation run"""
        # Use very short duration
        short_config = SimulationConfig(
            simulation_duration=0.5,  # 0.5 seconds
            time_step=0.1,
            failure_injection_enabled=False,  # Disable for predictable test
            network_delay_simulation_enabled=False,
            adaptive_migration_enabled=False,
            java_integration_enabled=False
        )
        
        short_simulation = LoadBalancerSimulation(short_config)
        
        # Mock request generation to return predictable requests
        with patch.object(short_simulation, '_generate_service_requests') as mock_gen:
            mock_gen.return_value = [
                ServiceRequest("test1", 5.0, 0.5, 2, 5),
                ServiceRequest("test2", 8.0, 1.0, 3, 7)
            ]
            
            result = short_simulation.run_simulation()
        
        # Check result structure
        self.assertIsNotNone(result)
        self.assertGreaterEqual(result.total_requests_processed, 0)
        self.assertGreaterEqual(result.successful_requests, 0)
        self.assertGreaterEqual(result.simulation_duration, 0.0)
        self.assertIsNotNone(result.final_state)


if __name__ == '__main__':
    unittest.main()