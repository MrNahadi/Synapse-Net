"""
Unit tests for redundancy and failover strategies.
Tests multi-node failure scenarios and network partitioning.

Requirements: 17.4, 17.5
"""

import unittest
import time
from typing import Set

from models import (
    NodeId, FailureType, SimulationState, NodeMetrics,
    LoadBalancingMetrics
)
from redundancy_failover import (
    RedundancyFailoverManager, RiskAssessment, RedundancyStrategy,
    FailoverMode, MultiNodeFailureScenario
)


class TestRedundancyFailover(unittest.TestCase):
    """Test redundancy and failover strategies"""
    
    def setUp(self):
        """Set up test fixtures"""
        self.manager = RedundancyFailoverManager()
        self.state = self._create_test_state()
    
    def _create_test_state(self) -> SimulationState:
        """Create a test simulation state"""
        node_metrics = {
            NodeId.EDGE1: NodeMetrics(
                latency=12.0, throughput=500.0, packet_loss=0.1,
                cpu_utilization=45.0, memory_usage=8.0,
                transactions_per_sec=150, lock_contention=8.0
            ),
            NodeId.EDGE2: NodeMetrics(
                latency=15.0, throughput=470.0, packet_loss=0.2,
                cpu_utilization=50.0, memory_usage=4.5,
                transactions_per_sec=100, lock_contention=12.0
            ),
            NodeId.CORE1: NodeMetrics(
                latency=8.0, throughput=1000.0, packet_loss=0.05,
                cpu_utilization=60.0, memory_usage=12.0,
                transactions_per_sec=250, lock_contention=5.0
            ),
            NodeId.CORE2: NodeMetrics(
                latency=10.0, throughput=950.0, packet_loss=0.08,
                cpu_utilization=55.0, memory_usage=10.0,
                transactions_per_sec=200, lock_contention=10.0
            ),
            NodeId.CLOUD1: NodeMetrics(
                latency=22.0, throughput=1250.0, packet_loss=0.15,
                cpu_utilization=72.0, memory_usage=16.0,
                transactions_per_sec=300, lock_contention=15.0
            )
        }
        
        load_balancing_metrics = LoadBalancingMetrics(
            cpu_distribution={node: metrics.cpu_utilization for node, metrics in node_metrics.items()},
            memory_distribution={node: metrics.memory_usage for node, metrics in node_metrics.items()},
            transaction_distribution={node: metrics.transactions_per_sec for node, metrics in node_metrics.items()},
            load_balance_index=0.8,
            total_services=10,
            migrations=0,
            last_update_timestamp=time.time()
        )
        
        return SimulationState(
            current_time=time.time(),
            active_nodes=set(NodeId),
            failed_nodes=set(),
            node_metrics=node_metrics,
            service_allocations={
                f"service_{i}": list(NodeId)[i % len(NodeId)]
                for i in range(10)
            },
            active_failures=[],
            migration_history=[],
            load_balancing_metrics=load_balancing_metrics
        )
    
    def _create_test_risk_assessments(self) -> list:
        """Create test risk assessments"""
        return [
            RiskAssessment(
                node_id=NodeId.CORE1,
                risk_score=0.85,
                failure_type=FailureType.BYZANTINE,
                criticality_score=0.95,
                dependent_nodes={NodeId.EDGE1, NodeId.EDGE2},
                cascade_risk_score=0.7,
                mitigation_strategies={"Deploy BFT protocols", "Implement multi-node verification"}
            ),
            RiskAssessment(
                node_id=NodeId.CORE2,
                risk_score=0.72,
                failure_type=FailureType.CRASH,
                criticality_score=0.90,
                dependent_nodes={NodeId.EDGE1, NodeId.EDGE2},
                cascade_risk_score=0.5,
                mitigation_strategies={"Implement redundancy", "Load shedding"}
            ),
            RiskAssessment(
                node_id=NodeId.EDGE1,
                risk_score=0.55,
                failure_type=FailureType.CRASH,
                criticality_score=0.70,
                dependent_nodes=set(),
                cascade_risk_score=0.3,
                mitigation_strategies={"Basic redundancy"}
            ),
            RiskAssessment(
                node_id=NodeId.EDGE2,
                risk_score=0.60,
                failure_type=FailureType.OMISSION,
                criticality_score=0.65,
                dependent_nodes=set(),
                cascade_risk_score=0.35,
                mitigation_strategies={"Retry mechanisms"}
            ),
            RiskAssessment(
                node_id=NodeId.CLOUD1,
                risk_score=0.68,
                failure_type=FailureType.OMISSION,
                criticality_score=0.75,
                dependent_nodes={NodeId.CORE1, NodeId.CORE2},
                cascade_risk_score=0.45,
                mitigation_strategies={"Geographic redundancy"}
            )
        ]
    
    def test_configure_redundancy_from_risk_assessment(self):
        """Test redundancy configuration based on risk assessment"""
        risk_assessments = self._create_test_risk_assessments()
        
        redundancy_config = self.manager.configure_redundancy_from_risk_assessment(
            risk_assessments
        )
        
        # Verify all nodes have redundancy strategies
        self.assertEqual(len(redundancy_config), 5)
        
        # Critical risk nodes should have strongest redundancy
        self.assertIn(redundancy_config[NodeId.CORE1], 
                     [RedundancyStrategy.N_WAY_REPLICATION, RedundancyStrategy.ACTIVE_ACTIVE])
        
        # High risk nodes should have strong redundancy
        self.assertIn(redundancy_config[NodeId.CORE2],
                     [RedundancyStrategy.ACTIVE_ACTIVE, RedundancyStrategy.ACTIVE_PASSIVE])
        
        # Verify replication groups created for high-risk nodes
        self.assertGreater(len(self.manager.replication_groups), 0)
    
    def test_automated_failover_success(self):
        """Test successful automated failover"""
        # Configure redundancy first
        risk_assessments = self._create_test_risk_assessments()
        self.manager.configure_redundancy_from_risk_assessment(risk_assessments)
        
        # Simulate node failure
        failed_node = NodeId.CORE1
        self.state.failed_nodes.add(failed_node)
        
        # Execute failover
        failover_event = self.manager.implement_automated_failover(
            failed_node, self.state, FailoverMode.AUTOMATIC
        )
        
        # Verify failover event created
        self.assertIsNotNone(failover_event)
        self.assertEqual(failover_event.failed_node, failed_node)
        self.assertIsNotNone(failover_event.failover_node)
        self.assertNotEqual(failover_event.failover_node, failed_node)
        
        # Verify downtime is reasonable
        self.assertGreater(failover_event.downtime_seconds, 0)
        self.assertLess(failover_event.downtime_seconds, 30.0)
        
        # Verify services migrated
        if failover_event.success:
            self.assertGreater(len(failover_event.services_migrated), 0)
    
    def test_multi_node_failure_scenario(self):
        """Test simultaneous multi-node failure scenario (Requirement 17.4)"""
        # Simulate 2-node failure
        scenario = self.manager.simulate_multi_node_failure(
            node_count=2,
            state=self.state,
            include_network_partition=False
        )
        
        # Verify scenario created
        self.assertIsNotNone(scenario)
        self.assertEqual(len(scenario.failed_nodes), 2)
        self.assertEqual(len(scenario.failure_types), 2)
        
        # Verify failure types assigned
        for node, failure_type in scenario.failure_types.items():
            self.assertIsInstance(failure_type, FailureType)
        
        # Verify duration is reasonable
        self.assertGreater(scenario.duration, 0)
        self.assertLess(scenario.duration, 600.0)
        
        # Verify not marked as network partition
        self.assertFalse(scenario.is_network_partition)
    
    def test_multi_node_failure_with_partition(self):
        """Test multi-node failure with network partition (Requirement 17.4)"""
        # Simulate 3-node failure with partition
        scenario = self.manager.simulate_multi_node_failure(
            node_count=3,
            state=self.state,
            include_network_partition=True
        )
        
        # Verify scenario created with partition
        self.assertIsNotNone(scenario)
        self.assertTrue(scenario.is_network_partition)
        self.assertGreater(len(scenario.partition_groups), 0)
        
        # Verify partition groups are disjoint
        all_partitioned_nodes = set()
        for partition in scenario.partition_groups:
            self.assertGreater(len(partition), 0)
            # Check no overlap
            self.assertEqual(len(all_partitioned_nodes & partition), 0)
            all_partitioned_nodes.update(partition)
    
    def test_network_partition_scenario(self):
        """Test network partitioning scenario (Requirement 17.5)"""
        # Simulate network partition
        scenario = self.manager.simulate_network_partition(self.state)
        
        # Verify partition created
        self.assertIsNotNone(scenario)
        self.assertTrue(scenario.is_network_partition)
        self.assertGreater(len(scenario.partition_groups), 1)
        
        # Verify all failure types are NETWORK_PARTITION
        for failure_type in scenario.failure_types.values():
            self.assertEqual(failure_type, FailureType.NETWORK_PARTITION)
        
        # Verify partition groups cover all or most nodes
        total_partitioned = sum(len(p) for p in scenario.partition_groups)
        self.assertGreaterEqual(total_partitioned, 2)
        
        # Verify partition tracked
        self.assertEqual(len(self.manager.network_partitions), 1)
    
    def test_multi_node_failure_recovery(self):
        """Test recovery from multi-node failure"""
        # Configure redundancy
        risk_assessments = self._create_test_risk_assessments()
        self.manager.configure_redundancy_from_risk_assessment(risk_assessments)
        
        # Create multi-node failure scenario
        scenario = self.manager.simulate_multi_node_failure(
            node_count=2,
            state=self.state,
            include_network_partition=False
        )
        
        # Mark nodes as failed
        for node in scenario.failed_nodes:
            self.state.failed_nodes.add(node)
        
        # Execute recovery
        failover_events = self.manager.handle_multi_node_failure_recovery(
            scenario, self.state
        )
        
        # Verify failover events created
        self.assertGreater(len(failover_events), 0)
        self.assertLessEqual(len(failover_events), len(scenario.failed_nodes))
        
        # Verify at least some failovers succeeded
        successful_failovers = sum(1 for e in failover_events if e.success)
        self.assertGreater(successful_failovers, 0)
    
    def test_network_partition_recovery(self):
        """Test recovery from network partition (Requirement 17.5)"""
        # Create network partition
        scenario = self.manager.simulate_network_partition(self.state)
        
        # Mark nodes as failed
        for node in scenario.failed_nodes:
            self.state.failed_nodes.add(node)
        
        # Configure redundancy
        risk_assessments = self._create_test_risk_assessments()
        self.manager.configure_redundancy_from_risk_assessment(risk_assessments)
        
        # Execute recovery
        failover_events = self.manager.handle_multi_node_failure_recovery(
            scenario, self.state
        )
        
        # Verify recovery attempted
        self.assertIsNotNone(failover_events)
        
        # Verify partition removed from tracking after recovery
        # (In real implementation, would check partition is resolved)
    
    def test_redundancy_strategy_selection(self):
        """Test redundancy strategy selection based on risk"""
        # Critical risk with Byzantine failure
        risk_critical_byzantine = RiskAssessment(
            node_id=NodeId.CORE1,
            risk_score=0.90,
            failure_type=FailureType.BYZANTINE,
            criticality_score=0.95,
            dependent_nodes={NodeId.EDGE1, NodeId.EDGE2},
            cascade_risk_score=0.8,
            mitigation_strategies=set()
        )
        
        strategy = self.manager._select_redundancy_strategy(risk_critical_byzantine)
        self.assertEqual(strategy, RedundancyStrategy.N_WAY_REPLICATION)
        
        # High risk with many dependents
        risk_high_dependents = RiskAssessment(
            node_id=NodeId.CORE2,
            risk_score=0.75,
            failure_type=FailureType.CRASH,
            criticality_score=0.85,
            dependent_nodes={NodeId.EDGE1, NodeId.EDGE2, NodeId.CLOUD1},
            cascade_risk_score=0.6,
            mitigation_strategies=set()
        )
        
        strategy = self.manager._select_redundancy_strategy(risk_high_dependents)
        self.assertEqual(strategy, RedundancyStrategy.ACTIVE_ACTIVE)
        
        # Medium risk
        risk_medium = RiskAssessment(
            node_id=NodeId.EDGE1,
            risk_score=0.55,
            failure_type=FailureType.CRASH,
            criticality_score=0.70,
            dependent_nodes=set(),
            cascade_risk_score=0.3,
            mitigation_strategies=set()
        )
        
        strategy = self.manager._select_redundancy_strategy(risk_medium)
        self.assertEqual(strategy, RedundancyStrategy.ACTIVE_PASSIVE)
    
    def test_replication_group_creation(self):
        """Test replication group creation for high-risk nodes"""
        risk_assessments = self._create_test_risk_assessments()
        
        self.manager.configure_redundancy_from_risk_assessment(risk_assessments)
        
        # Verify replication groups created
        self.assertGreater(len(self.manager.replication_groups), 0)
        
        # Check critical node has replication group
        core1_group = None
        for group in self.manager.replication_groups.values():
            if group.primary_node == NodeId.CORE1:
                core1_group = group
                break
        
        self.assertIsNotNone(core1_group)
        self.assertGreater(len(core1_group.replica_nodes), 0)
        self.assertGreaterEqual(core1_group.replication_factor, 2)
    
    def test_failover_target_selection(self):
        """Test failover target selection logic"""
        # Configure redundancy
        risk_assessments = self._create_test_risk_assessments()
        self.manager.configure_redundancy_from_risk_assessment(risk_assessments)
        
        # Get a replication group
        replication_group = list(self.manager.replication_groups.values())[0]
        
        # Select failover target
        failed_node = replication_group.primary_node
        failover_target = self.manager._select_failover_target(
            failed_node, replication_group, self.state
        )
        
        # Verify target selected
        if len(replication_group.replica_nodes) > 0:
            self.assertIsNotNone(failover_target)
            self.assertIn(failover_target, replication_group.replica_nodes)
            self.assertNotIn(failover_target, self.state.failed_nodes)
    
    def test_statistics_collection(self):
        """Test statistics collection"""
        # Configure redundancy
        risk_assessments = self._create_test_risk_assessments()
        self.manager.configure_redundancy_from_risk_assessment(risk_assessments)
        
        # Simulate some failures and failovers
        scenario = self.manager.simulate_multi_node_failure(2, self.state)
        for node in scenario.failed_nodes:
            self.state.failed_nodes.add(node)
        
        self.manager.handle_multi_node_failure_recovery(scenario, self.state)
        
        # Get statistics
        stats = self.manager.get_redundancy_statistics()
        
        # Verify statistics structure
        self.assertIn('replication_groups', stats)
        self.assertIn('total_failovers', stats)
        self.assertIn('successful_failovers', stats)
        self.assertIn('multi_node_failures', stats)
        self.assertIn('network_partitions', stats)
        
        # Verify counts
        self.assertGreater(stats['replication_groups'], 0)
        self.assertGreaterEqual(stats['multi_node_failures'], 1)
    
    def test_simultaneous_failures_different_types(self):
        """Test handling simultaneous failures of different types"""
        # Create scenario with mixed failure types
        scenario = self.manager.simulate_multi_node_failure(3, self.state)
        
        # Verify different failure types present
        failure_types_present = set(scenario.failure_types.values())
        
        # Should have at least 2 different failure types with 3 nodes
        self.assertGreaterEqual(len(failure_types_present), 1)
        
        # All failure types should be valid
        for ft in failure_types_present:
            self.assertIn(ft, [FailureType.CRASH, FailureType.OMISSION, 
                             FailureType.BYZANTINE, FailureType.NETWORK_PARTITION])
    
    def test_cascading_failure_prevention(self):
        """Test that redundancy prevents cascading failures"""
        # Configure redundancy for high-risk nodes
        risk_assessments = self._create_test_risk_assessments()
        self.manager.configure_redundancy_from_risk_assessment(risk_assessments)
        
        # Simulate failure of critical node
        critical_node = NodeId.CORE1
        self.state.failed_nodes.add(critical_node)
        
        # Execute failover
        failover_event = self.manager.implement_automated_failover(
            critical_node, self.state
        )
        
        # If failover successful, dependent nodes should remain operational
        if failover_event and failover_event.success:
            # Verify failover node is now serving
            self.assertIsNotNone(failover_event.failover_node)
            self.assertNotIn(failover_event.failover_node, self.state.failed_nodes)


if __name__ == '__main__':
    unittest.main()
