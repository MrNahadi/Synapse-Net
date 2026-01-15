#!/usr/bin/env python3
"""
Demonstration of redundancy and failover strategies.
Shows integration with risk assessment and multi-node failure handling.
"""

import time
import logging
from models import (
    NodeId, FailureType, SimulationState, NodeMetrics,
    LoadBalancingMetrics
)
from redundancy_failover import (
    RedundancyFailoverManager, RiskAssessment, FailoverMode
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

def create_sample_state() -> SimulationState:
    """Create a sample simulation state"""
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
        total_services=20,
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
            for i in range(20)
        },
        active_failures=[],
        migration_history=[],
        load_balancing_metrics=load_balancing_metrics
    )

def create_sample_risk_assessments() -> list:
    """Create sample risk assessments (simulating Java SystemicFailureRiskAssessor output)"""
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
            node_id=NodeId.CLOUD1,
            risk_score=0.68,
            failure_type=FailureType.OMISSION,
            criticality_score=0.75,
            dependent_nodes={NodeId.CORE1, NodeId.CORE2},
            cascade_risk_score=0.45,
            mitigation_strategies={"Geographic redundancy"}
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
        )
    ]

def main():
    """Main demonstration"""
    print("="*70)
    print("Redundancy and Failover Strategies Demonstration")
    print("="*70)
    print()
    
    # Initialize manager
    manager = RedundancyFailoverManager()
    
    # Create simulation state
    state = create_sample_state()
    print(f"Initial state: {len(state.active_nodes)} active nodes, {len(state.failed_nodes)} failed nodes")
    print()
    
    # Step 1: Configure redundancy based on risk assessment
    print("Step 1: Configuring redundancy based on risk assessment...")
    print("-" * 70)
    risk_assessments = create_sample_risk_assessments()
    
    for risk in risk_assessments:
        print(f"  {risk.node_id.value}: risk={risk.risk_score:.3f}, "
              f"criticality={risk.criticality_score:.3f}, "
              f"cascade_risk={risk.cascade_risk_score:.3f}")
    
    redundancy_config = manager.configure_redundancy_from_risk_assessment(risk_assessments)
    
    print(f"\nRedundancy strategies configured:")
    for node, strategy in redundancy_config.items():
        print(f"  {node.value}: {strategy.value}")
    
    print(f"\nReplication groups created: {len(manager.replication_groups)}")
    for group_id, group in manager.replication_groups.items():
        print(f"  {group_id}: primary={group.primary_node.value}, "
              f"replicas={[n.value for n in group.replica_nodes]}, "
              f"strategy={group.strategy.value}")
    print()
    
    # Step 2: Simulate single node failure and failover
    print("Step 2: Simulating single node failure and automated failover...")
    print("-" * 70)
    failed_node = NodeId.CORE1
    print(f"Injecting failure on {failed_node.value}...")
    state.failed_nodes.add(failed_node)
    
    failover_event = manager.implement_automated_failover(
        failed_node, state, FailoverMode.AUTOMATIC
    )
    
    if failover_event:
        print(f"Failover executed:")
        print(f"  Failed node: {failover_event.failed_node.value}")
        print(f"  Failover node: {failover_event.failover_node.value}")
        print(f"  Success: {failover_event.success}")
        print(f"  Downtime: {failover_event.downtime_seconds:.2f}s")
        print(f"  Services migrated: {len(failover_event.services_migrated)}")
    else:
        print("Failover could not be executed")
    print()
    
    # Step 3: Simulate multi-node failure
    print("Step 3: Simulating multi-node failure scenario...")
    print("-" * 70)
    
    # Reset state
    state.failed_nodes.clear()
    
    scenario = manager.simulate_multi_node_failure(
        node_count=2,
        state=state,
        include_network_partition=False
    )
    
    print(f"Multi-node failure scenario created:")
    print(f"  Scenario ID: {scenario.scenario_id}")
    print(f"  Failed nodes: {[n.value for n in scenario.failed_nodes]}")
    print(f"  Failure types: {[(n.value, ft.value) for n, ft in scenario.failure_types.items()]}")
    print(f"  Duration: {scenario.duration:.1f}s")
    print(f"  Network partition: {scenario.is_network_partition}")
    
    # Mark nodes as failed
    for node in scenario.failed_nodes:
        state.failed_nodes.add(node)
    
    # Execute recovery
    print(f"\nExecuting multi-node failure recovery...")
    failover_events = manager.handle_multi_node_failure_recovery(scenario, state)
    
    print(f"Recovery completed:")
    print(f"  Total failovers attempted: {len(failover_events)}")
    print(f"  Successful failovers: {sum(1 for e in failover_events if e.success)}")
    print(f"  Failed failovers: {sum(1 for e in failover_events if not e.success)}")
    print()
    
    # Step 4: Simulate network partition
    print("Step 4: Simulating network partition...")
    print("-" * 70)
    
    # Reset state
    state.failed_nodes.clear()
    
    partition_scenario = manager.simulate_network_partition(state)
    
    print(f"Network partition scenario created:")
    print(f"  Scenario ID: {partition_scenario.scenario_id}")
    print(f"  Partition groups: {len(partition_scenario.partition_groups)}")
    for i, partition in enumerate(partition_scenario.partition_groups):
        print(f"    Partition {i+1}: {[n.value for n in partition]}")
    print(f"  Nodes in minority partitions: {[n.value for n in partition_scenario.failed_nodes]}")
    print(f"  Duration: {partition_scenario.duration:.1f}s")
    print()
    
    # Step 5: Display statistics
    print("Step 5: System statistics...")
    print("-" * 70)
    stats = manager.get_redundancy_statistics()
    
    print(f"Redundancy and Failover Statistics:")
    print(f"  Replication groups: {stats['replication_groups']}")
    print(f"  Total failovers: {stats['total_failovers']}")
    print(f"  Successful failovers: {stats['successful_failovers']}")
    print(f"  Failed failovers: {stats['failed_failovers']}")
    print(f"  Average downtime: {stats['average_downtime']:.2f}s")
    print(f"  Multi-node failures: {stats['multi_node_failures']}")
    print(f"  Network partitions: {stats['network_partitions']}")
    
    if stats['failovers_by_node']:
        print(f"\n  Failovers by node:")
        for node, count in stats['failovers_by_node'].items():
            print(f"    {node}: {count}")
    
    if stats['replication_strategies']:
        print(f"\n  Replication strategies:")
        for strategy, count in stats['replication_strategies'].items():
            print(f"    {strategy}: {count}")
    
    print()
    print("="*70)
    print("Demonstration completed successfully!")
    print("="*70)

if __name__ == '__main__':
    main()
