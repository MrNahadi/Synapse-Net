"""
Redundancy and failover strategies for high-risk nodes.
Implements replication strategies, automated failover mechanisms,
and simulates simultaneous multi-node failures and network partitioning.

Integrates with risk assessment from task 18 (SystemicFailureRiskAssessor).

Requirements: 17.1, 17.2, 17.3, 17.4, 17.5
"""

import random
import time
import logging
from typing import List, Dict, Optional, Set, Tuple
from dataclasses import dataclass, field
from enum import Enum
from collections import defaultdict

from models import (
    NodeId, FailureType, FailureScenario, SimulationState,
    NodeMetrics, MigrationEvent
)


class RedundancyStrategy(Enum):
    """Redundancy strategy types"""
    ACTIVE_ACTIVE = "active_active"  # All replicas actively serve requests
    ACTIVE_PASSIVE = "active_passive"  # Primary active, backups passive
    N_WAY_REPLICATION = "n_way_replication"  # N replicas with quorum
    GEOGRAPHIC_REDUNDANCY = "geographic_redundancy"  # Distributed across layers


class FailoverMode(Enum):
    """Failover mode types"""
    AUTOMATIC = "automatic"  # Automatic failover without intervention
    SEMI_AUTOMATIC = "semi_automatic"  # Automatic with confirmation
    MANUAL = "manual"  # Manual intervention required


@dataclass
class RiskAssessment:
    """Risk assessment data from Java SystemicFailureRiskAssessor"""
    node_id: NodeId
    risk_score: float  # 0.0-1.0
    failure_type: FailureType
    criticality_score: float  # 0.0-1.0
    dependent_nodes: Set[NodeId]
    cascade_risk_score: float  # 0.0-1.0
    mitigation_strategies: Set[str]


@dataclass
class ReplicationGroup:
    """Replication group for a service or data"""
    group_id: str
    primary_node: NodeId
    replica_nodes: Set[NodeId]
    strategy: RedundancyStrategy
    replication_factor: int
    consistency_level: str  # "strong", "eventual", "causal"
    last_sync_time: float = field(default_factory=time.time)
    
    def get_all_nodes(self) -> Set[NodeId]:
        """Get all nodes in the replication group"""
        return {self.primary_node} | self.replica_nodes


@dataclass
class FailoverEvent:
    """Failover event record"""
    event_id: str
    failed_node: NodeId
    failover_node: NodeId
    failover_mode: FailoverMode
    start_time: float
    completion_time: Optional[float] = None
    success: bool = True
    services_migrated: List[str] = field(default_factory=list)
    downtime_seconds: float = 0.0
    reason: str = ""


@dataclass
class MultiNodeFailureScenario:
    """Scenario for simultaneous multi-node failures"""
    scenario_id: str
    failed_nodes: Set[NodeId]
    failure_types: Dict[NodeId, FailureType]
    start_time: float
    duration: float
    is_network_partition: bool = False
    partition_groups: List[Set[NodeId]] = field(default_factory=list)


class RedundancyFailoverManager:
    """
    Manages redundancy strategies and automated failover for high-risk nodes.
    Integrates with risk assessment to prioritize protection strategies.
    """
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        
        # Replication groups
        self.replication_groups: Dict[str, ReplicationGroup] = {}
        
        # Failover tracking
        self.failover_history: List[FailoverEvent] = []
        self.active_failovers: Set[str] = set()
        
        # Risk-based configuration
        self.high_risk_threshold = 0.7
        self.critical_risk_threshold = 0.85
        
        # Failover configuration
        self.failover_timeout = 30.0  # 30 seconds max failover time
        self.health_check_interval = 5.0  # 5 seconds between health checks
        self.min_replica_count = 2  # Minimum replicas for high-risk nodes
        
        # Multi-node failure tracking
        self.multi_node_failure_scenarios: List[MultiNodeFailureScenario] = []
        
        # Network partition tracking
        self.network_partitions: List[Tuple[float, List[Set[NodeId]]]] = []
        
        self.logger.info("RedundancyFailoverManager initialized")
    
    def configure_redundancy_from_risk_assessment(
            self, risk_assessments: List[RiskAssessment]) -> Dict[NodeId, RedundancyStrategy]:
        """
        Configure redundancy strategies based on risk assessment results.
        High-risk nodes get more aggressive redundancy strategies.
        
        Requirements: 17.1
        """
        redundancy_config = {}
        
        for risk in risk_assessments:
            strategy = self._select_redundancy_strategy(risk)
            redundancy_config[risk.node_id] = strategy
            
            # Create replication groups for high-risk nodes
            if risk.risk_score >= self.high_risk_threshold:
                self._create_replication_group_for_node(risk, strategy)
                
                self.logger.info(
                    "Configured %s redundancy for high-risk node %s (risk: %.3f)",
                    strategy.value, risk.node_id.value, risk.risk_score
                )
        
        return redundancy_config
    
    def _select_redundancy_strategy(self, risk: RiskAssessment) -> RedundancyStrategy:
        """Select appropriate redundancy strategy based on risk profile"""
        
        # Critical risk nodes need strongest redundancy
        if risk.risk_score >= self.critical_risk_threshold:
            if risk.failure_type == FailureType.BYZANTINE:
                return RedundancyStrategy.N_WAY_REPLICATION  # Need quorum for Byzantine
            else:
                return RedundancyStrategy.ACTIVE_ACTIVE  # Maximum availability
        
        # High risk nodes need strong redundancy
        elif risk.risk_score >= self.high_risk_threshold:
            if len(risk.dependent_nodes) > 2:
                return RedundancyStrategy.ACTIVE_ACTIVE  # Many dependents need high availability
            else:
                return RedundancyStrategy.ACTIVE_PASSIVE  # Standard failover
        
        # Medium risk nodes need basic redundancy
        elif risk.risk_score >= 0.5:
            return RedundancyStrategy.ACTIVE_PASSIVE
        
        # Low risk nodes can use geographic redundancy
        else:
            return RedundancyStrategy.GEOGRAPHIC_REDUNDANCY
    
    def _create_replication_group_for_node(
            self, risk: RiskAssessment, strategy: RedundancyStrategy):
        """Create replication group for a high-risk node"""
        
        # Determine replication factor based on risk
        if risk.risk_score >= self.critical_risk_threshold:
            replication_factor = 3  # Triple redundancy for critical nodes
        elif risk.risk_score >= self.high_risk_threshold:
            replication_factor = 2  # Double redundancy for high-risk nodes
        else:
            replication_factor = 1  # Single backup
        
        # Select replica nodes (avoid nodes with same failure type)
        replica_nodes = self._select_replica_nodes(
            risk.node_id, risk.failure_type, replication_factor
        )
        
        # Determine consistency level based on failure type
        if risk.failure_type == FailureType.BYZANTINE:
            consistency_level = "strong"  # Byzantine needs strong consistency
        elif risk.failure_type == FailureType.OMISSION:
            consistency_level = "causal"  # Omission can use causal
        else:
            consistency_level = "eventual"  # Crash can use eventual
        
        group_id = f"replication_group_{risk.node_id.value}"
        
        replication_group = ReplicationGroup(
            group_id=group_id,
            primary_node=risk.node_id,
            replica_nodes=replica_nodes,
            strategy=strategy,
            replication_factor=replication_factor,
            consistency_level=consistency_level
        )
        
        self.replication_groups[group_id] = replication_group
        
        self.logger.info(
            "Created replication group %s: primary=%s, replicas=%s, strategy=%s",
            group_id, risk.node_id.value, 
            [n.value for n in replica_nodes], strategy.value
        )
    
    def _select_replica_nodes(
            self, primary_node: NodeId, primary_failure_type: FailureType,
            count: int) -> Set[NodeId]:
        """Select replica nodes with different failure characteristics"""
        
        # Define failure types for each node
        node_failure_types = {
            NodeId.EDGE1: FailureType.CRASH,
            NodeId.EDGE2: FailureType.OMISSION,
            NodeId.CORE1: FailureType.BYZANTINE,
            NodeId.CORE2: FailureType.CRASH,
            NodeId.CLOUD1: FailureType.OMISSION
        }
        
        # Prefer nodes with different failure types
        candidates = [
            node for node in NodeId 
            if node != primary_node and node_failure_types.get(node) != primary_failure_type
        ]
        
        # If not enough different-type nodes, include same-type nodes
        if len(candidates) < count:
            candidates.extend([
                node for node in NodeId 
                if node != primary_node and node not in candidates
            ])
        
        # Select up to 'count' replicas
        selected = set(random.sample(candidates, min(count, len(candidates))))
        
        return selected
    
    def implement_automated_failover(
            self, failed_node: NodeId, state: SimulationState,
            failover_mode: FailoverMode = FailoverMode.AUTOMATIC) -> Optional[FailoverEvent]:
        """
        Implement automated failover mechanism when a node fails.
        
        Requirements: 17.3
        """
        current_time = time.time()
        
        # Find replication group containing the failed node
        replication_group = self._find_replication_group_for_node(failed_node)
        
        if not replication_group:
            self.logger.warning(
                "No replication group found for failed node %s, cannot failover",
                failed_node.value
            )
            return None
        
        # Select failover target
        failover_node = self._select_failover_target(
            failed_node, replication_group, state
        )
        
        if not failover_node:
            self.logger.error(
                "No suitable failover target found for node %s",
                failed_node.value
            )
            return None
        
        # Execute failover
        event_id = f"failover_{failed_node.value}_{int(current_time)}"
        
        failover_event = FailoverEvent(
            event_id=event_id,
            failed_node=failed_node,
            failover_node=failover_node,
            failover_mode=failover_mode,
            start_time=current_time,
            reason=f"Node {failed_node.value} failure detected"
        )
        
        # Simulate failover execution
        success, downtime = self._execute_failover(
            failed_node, failover_node, replication_group, state
        )
        
        failover_event.success = success
        failover_event.downtime_seconds = downtime
        failover_event.completion_time = current_time + downtime
        
        # Migrate services
        services_migrated = self._migrate_services_during_failover(
            failed_node, failover_node, state
        )
        failover_event.services_migrated = services_migrated
        
        # Update replication group
        if success:
            self._update_replication_group_after_failover(
                replication_group, failed_node, failover_node
            )
        
        self.failover_history.append(failover_event)
        
        if success:
            self.logger.info(
                "Failover successful: %s -> %s (downtime: %.2fs, services: %d)",
                failed_node.value, failover_node.value, downtime, len(services_migrated)
            )
        else:
            self.logger.error(
                "Failover failed: %s -> %s",
                failed_node.value, failover_node.value
            )
        
        return failover_event
    
    def _find_replication_group_for_node(self, node_id: NodeId) -> Optional[ReplicationGroup]:
        """Find replication group containing the specified node"""
        for group in self.replication_groups.values():
            if node_id == group.primary_node or node_id in group.replica_nodes:
                return group
        return None
    
    def _select_failover_target(
            self, failed_node: NodeId, replication_group: ReplicationGroup,
            state: SimulationState) -> Optional[NodeId]:
        """Select best failover target from replication group"""
        
        # Get available replica nodes (not failed)
        available_replicas = [
            node for node in replication_group.replica_nodes
            if node not in state.failed_nodes
        ]
        
        if not available_replicas:
            return None
        
        # Score each replica based on current load and performance
        best_node = None
        best_score = float('-inf')
        
        for replica in available_replicas:
            score = self._calculate_failover_target_score(replica, state)
            if score > best_score:
                best_score = score
                best_node = replica
        
        return best_node
    
    def _calculate_failover_target_score(
            self, node_id: NodeId, state: SimulationState) -> float:
        """Calculate score for failover target selection"""
        
        metrics = state.node_metrics.get(node_id)
        if not metrics:
            return 0.0
        
        # Lower utilization is better
        cpu_score = 1.0 - (metrics.cpu_utilization / 72.0)
        memory_score = 1.0 - (metrics.memory_usage / 16.0)
        
        # Higher performance is better
        latency_score = 1.0 - (metrics.latency / 22.0)
        throughput_score = metrics.throughput / 1250.0
        
        # Combined score
        score = (cpu_score * 0.3 + memory_score * 0.3 + 
                latency_score * 0.2 + throughput_score * 0.2)
        
        return score
    
    def _execute_failover(
            self, failed_node: NodeId, failover_node: NodeId,
            replication_group: ReplicationGroup, state: SimulationState) -> Tuple[bool, float]:
        """Execute failover operation"""
        
        # Simulate failover time based on strategy
        if replication_group.strategy == RedundancyStrategy.ACTIVE_ACTIVE:
            # Active-active has minimal downtime (already serving)
            downtime = random.uniform(0.5, 2.0)
            success_rate = 0.98
        elif replication_group.strategy == RedundancyStrategy.ACTIVE_PASSIVE:
            # Active-passive needs activation time
            downtime = random.uniform(2.0, 10.0)
            success_rate = 0.95
        elif replication_group.strategy == RedundancyStrategy.N_WAY_REPLICATION:
            # N-way needs quorum reconfiguration
            downtime = random.uniform(5.0, 15.0)
            success_rate = 0.92
        else:  # GEOGRAPHIC_REDUNDANCY
            # Geographic failover takes longer
            downtime = random.uniform(10.0, 30.0)
            success_rate = 0.90
        
        # Determine success
        success = random.random() < success_rate
        
        return success, downtime
    
    def _migrate_services_during_failover(
            self, failed_node: NodeId, failover_node: NodeId,
            state: SimulationState) -> List[str]:
        """Migrate services from failed node to failover node"""
        
        migrated_services = []
        
        for service_id, allocated_node in state.service_allocations.items():
            if allocated_node == failed_node:
                # Migrate service to failover node
                state.service_allocations[service_id] = failover_node
                migrated_services.append(service_id)
        
        return migrated_services
    
    def _update_replication_group_after_failover(
            self, replication_group: ReplicationGroup,
            failed_node: NodeId, failover_node: NodeId):
        """Update replication group after successful failover"""
        
        if failed_node == replication_group.primary_node:
            # Promote failover node to primary
            replication_group.primary_node = failover_node
            replication_group.replica_nodes.discard(failover_node)
            replication_group.replica_nodes.add(failed_node)
            
            self.logger.info(
                "Promoted %s to primary in group %s",
                failover_node.value, replication_group.group_id
            )
    
    def simulate_multi_node_failure(
            self, node_count: int, state: SimulationState,
            include_network_partition: bool = False) -> MultiNodeFailureScenario:
        """
        Simulate simultaneous multi-node failures.
        
        Requirements: 17.4
        """
        current_time = time.time()
        
        # Select random nodes to fail
        available_nodes = list(state.active_nodes - state.failed_nodes)
        failed_count = min(node_count, len(available_nodes))
        failed_nodes = set(random.sample(available_nodes, failed_count))
        
        # Assign failure types based on node characteristics
        failure_types = {}
        node_failure_map = {
            NodeId.EDGE1: FailureType.CRASH,
            NodeId.EDGE2: FailureType.OMISSION,
            NodeId.CORE1: FailureType.BYZANTINE,
            NodeId.CORE2: FailureType.CRASH,
            NodeId.CLOUD1: FailureType.OMISSION
        }
        
        for node in failed_nodes:
            failure_types[node] = node_failure_map.get(node, FailureType.CRASH)
        
        # Generate failure duration
        duration = random.uniform(30.0, 300.0)  # 30 seconds to 5 minutes
        
        # Create partition groups if requested
        partition_groups = []
        is_partition = False
        
        if include_network_partition:
            is_partition = True
            partition_groups = self._create_network_partition_groups(
                state.active_nodes, failed_nodes
            )
        
        scenario_id = f"multi_failure_{int(current_time)}"
        
        scenario = MultiNodeFailureScenario(
            scenario_id=scenario_id,
            failed_nodes=failed_nodes,
            failure_types=failure_types,
            start_time=current_time,
            duration=duration,
            is_network_partition=is_partition,
            partition_groups=partition_groups
        )
        
        self.multi_node_failure_scenarios.append(scenario)
        
        self.logger.warning(
            "Multi-node failure scenario created: %d nodes failed, partition=%s",
            len(failed_nodes), is_partition
        )
        
        return scenario
    
    def simulate_network_partition(
            self, state: SimulationState) -> MultiNodeFailureScenario:
        """
        Simulate network partitioning scenario.
        
        Requirements: 17.5
        """
        current_time = time.time()
        
        # Create partition groups
        all_nodes = state.active_nodes
        partition_groups = self._create_network_partition_groups(all_nodes, set())
        
        # Nodes in smaller partitions are effectively "failed" from majority perspective
        majority_partition = max(partition_groups, key=len)
        failed_nodes = all_nodes - majority_partition
        
        # Network partition failures
        failure_types = {
            node: FailureType.NETWORK_PARTITION for node in failed_nodes
        }
        
        duration = random.uniform(60.0, 300.0)  # 1-5 minutes
        
        scenario_id = f"network_partition_{int(current_time)}"
        
        scenario = MultiNodeFailureScenario(
            scenario_id=scenario_id,
            failed_nodes=failed_nodes,
            failure_types=failure_types,
            start_time=current_time,
            duration=duration,
            is_network_partition=True,
            partition_groups=partition_groups
        )
        
        self.multi_node_failure_scenarios.append(scenario)
        self.network_partitions.append((current_time, partition_groups))
        
        self.logger.warning(
            "Network partition created: %d partitions, majority=%d nodes, minority=%d nodes",
            len(partition_groups), len(majority_partition), len(failed_nodes)
        )
        
        return scenario
    
    def _create_network_partition_groups(
            self, all_nodes: Set[NodeId], failed_nodes: Set[NodeId]) -> List[Set[NodeId]]:
        """Create network partition groups"""
        
        available_nodes = list(all_nodes - failed_nodes)
        
        if len(available_nodes) < 2:
            return [set(available_nodes)]
        
        # Create 2-3 partition groups
        num_partitions = random.randint(2, min(3, len(available_nodes)))
        
        # Shuffle and split nodes
        random.shuffle(available_nodes)
        partition_size = len(available_nodes) // num_partitions
        
        partitions = []
        for i in range(num_partitions):
            start_idx = i * partition_size
            if i == num_partitions - 1:
                # Last partition gets remaining nodes
                partition = set(available_nodes[start_idx:])
            else:
                partition = set(available_nodes[start_idx:start_idx + partition_size])
            
            if partition:
                partitions.append(partition)
        
        return partitions
    
    def handle_multi_node_failure_recovery(
            self, scenario: MultiNodeFailureScenario,
            state: SimulationState) -> List[FailoverEvent]:
        """
        Handle recovery from multi-node failure scenario.
        Coordinates multiple failovers and ensures system stability.
        
        Requirements: 17.2, 17.3
        """
        failover_events = []
        
        # Sort failed nodes by risk/criticality (handle critical nodes first)
        sorted_failures = self._prioritize_failure_recovery(
            scenario.failed_nodes, state
        )
        
        for failed_node in sorted_failures:
            # Attempt failover for each failed node
            failover_event = self.implement_automated_failover(
                failed_node, state, FailoverMode.AUTOMATIC
            )
            
            if failover_event:
                failover_events.append(failover_event)
                
                # Add delay between failovers to avoid overwhelming system
                time.sleep(0.1)  # Small delay in simulation
        
        # Handle network partition recovery
        if scenario.is_network_partition:
            self._handle_partition_recovery(scenario, state)
        
        self.logger.info(
            "Multi-node failure recovery completed: %d/%d nodes recovered",
            sum(1 for e in failover_events if e.success), len(scenario.failed_nodes)
        )
        
        return failover_events
    
    def _prioritize_failure_recovery(
            self, failed_nodes: Set[NodeId], state: SimulationState) -> List[NodeId]:
        """Prioritize failed nodes for recovery based on criticality"""
        
        # Define criticality scores (from architecture)
        criticality_scores = {
            NodeId.CORE1: 0.95,  # Highest - transaction commit
            NodeId.CORE2: 0.90,  # High - recovery and load balancing
            NodeId.CLOUD1: 0.75,  # Medium-high - analytics
            NodeId.EDGE1: 0.70,  # Medium - RPC handling
            NodeId.EDGE2: 0.65   # Medium - migration
        }
        
        # Sort by criticality (highest first)
        sorted_nodes = sorted(
            failed_nodes,
            key=lambda n: criticality_scores.get(n, 0.5),
            reverse=True
        )
        
        return sorted_nodes
    
    def _handle_partition_recovery(
            self, scenario: MultiNodeFailureScenario, state: SimulationState):
        """Handle recovery from network partition"""
        
        # Merge partition groups back together
        self.logger.info(
            "Network partition recovery: merging %d partitions",
            len(scenario.partition_groups)
        )
        
        # In real system, would need to reconcile state across partitions
        # For simulation, we just log the recovery
        
        # Remove partition from tracking
        self.network_partitions = [
            (t, groups) for t, groups in self.network_partitions
            if t != scenario.start_time
        ]
    
    def get_redundancy_statistics(self) -> Dict:
        """Get redundancy and failover statistics"""
        stats = {
            'replication_groups': len(self.replication_groups),
            'total_failovers': len(self.failover_history),
            'successful_failovers': sum(1 for e in self.failover_history if e.success),
            'failed_failovers': sum(1 for e in self.failover_history if not e.success),
            'average_downtime': 0.0,
            'multi_node_failures': len(self.multi_node_failure_scenarios),
            'network_partitions': len(self.network_partitions),
            'failovers_by_node': {},
            'replication_strategies': {}
        }
        
        if self.failover_history:
            total_downtime = sum(e.downtime_seconds for e in self.failover_history)
            stats['average_downtime'] = total_downtime / len(self.failover_history)
            
            # Count failovers by node
            for event in self.failover_history:
                node = event.failed_node.value
                stats['failovers_by_node'][node] = stats['failovers_by_node'].get(node, 0) + 1
        
        # Count replication strategies
        for group in self.replication_groups.values():
            strategy = group.strategy.value
            stats['replication_strategies'][strategy] = stats['replication_strategies'].get(strategy, 0) + 1
        
        return stats
    
    def reset(self):
        """Reset redundancy and failover manager state"""
        self.replication_groups.clear()
        self.failover_history.clear()
        self.active_failovers.clear()
        self.multi_node_failure_scenarios.clear()
        self.network_partitions.clear()
        self.logger.info("RedundancyFailoverManager state reset")
