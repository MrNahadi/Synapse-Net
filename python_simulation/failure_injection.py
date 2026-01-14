"""
Failure injection mechanisms for testing load balancing resilience.
Implements crash, omission, and Byzantine failure scenarios.
"""

import random
import time
import logging
from typing import List, Dict, Optional
from dataclasses import dataclass

from models import NodeId, FailureType, FailureScenario, SimulationState


@dataclass
class FailureProfile:
    """Failure profile for a specific node type"""
    node_id: NodeId
    failure_type: FailureType
    base_failure_rate: float  # failures per hour
    mean_duration: float  # seconds
    severity_range: tuple[float, float]  # (min, max) severity


class FailureInjector:
    """
    Failure injection system that simulates realistic failure scenarios
    based on node characteristics and failure types.
    """
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        
        # Define failure profiles based on node characteristics from dataset
        self.failure_profiles = {
            NodeId.EDGE1: FailureProfile(
                node_id=NodeId.EDGE1,
                failure_type=FailureType.CRASH,
                base_failure_rate=0.1,  # 0.1 failures per hour
                mean_duration=30.0,  # 30 seconds average
                severity_range=(0.8, 1.0)  # High severity for crash
            ),
            NodeId.EDGE2: FailureProfile(
                node_id=NodeId.EDGE2,
                failure_type=FailureType.OMISSION,
                base_failure_rate=0.15,  # 0.15 failures per hour
                mean_duration=45.0,  # 45 seconds average
                severity_range=(0.3, 0.7)  # Medium severity for omission
            ),
            NodeId.CORE1: FailureProfile(
                node_id=NodeId.CORE1,
                failure_type=FailureType.BYZANTINE,
                base_failure_rate=0.05,  # 0.05 failures per hour (rare)
                mean_duration=120.0,  # 2 minutes average
                severity_range=(0.9, 1.0)  # Very high severity for Byzantine
            ),
            NodeId.CORE2: FailureProfile(
                node_id=NodeId.CORE2,
                failure_type=FailureType.CRASH,
                base_failure_rate=0.08,  # 0.08 failures per hour
                mean_duration=25.0,  # 25 seconds average
                severity_range=(0.7, 0.9)  # High severity for crash
            ),
            NodeId.CLOUD1: FailureProfile(
                node_id=NodeId.CLOUD1,
                failure_type=FailureType.OMISSION,
                base_failure_rate=0.12,  # 0.12 failures per hour
                mean_duration=60.0,  # 1 minute average
                severity_range=(0.4, 0.8)  # Medium-high severity for omission
            )
        }
        
        # Track failure history
        self.failure_history: List[FailureScenario] = []
        self.last_failure_times: Dict[NodeId, float] = {}
        
        # Failure correlation tracking
        self.cascading_failure_probability = 0.2  # 20% chance of cascading
        self.network_partition_probability = 0.05  # 5% chance of network partition
        
        self.logger.info("FailureInjector initialized with profiles for %d nodes", 
                        len(self.failure_profiles))
    
    def inject_failures(self, current_time: float, state: SimulationState) -> List[FailureScenario]:
        """
        Inject failures based on current system state and failure profiles.
        
        Args:
            current_time: Current simulation time
            state: Current simulation state
            
        Returns:
            List of new failure scenarios
        """
        new_failures = []
        
        # Check each node for potential failures
        for node_id in NodeId:
            if node_id in state.failed_nodes:
                continue  # Skip already failed nodes
            
            if self._should_inject_failure(node_id, current_time, state):
                failure = self._create_failure_scenario(node_id, current_time)
                new_failures.append(failure)
                self.failure_history.append(failure)
                self.last_failure_times[node_id] = current_time
                
                self.logger.info("Injected %s failure on node %s (duration: %.1fs, severity: %.2f)",
                               failure.failure_type.value, failure.node_id.value,
                               failure.duration, failure.severity)
        
        # Check for cascading failures
        if new_failures:
            cascading_failures = self._evaluate_cascading_failures(new_failures, current_time, state)
            new_failures.extend(cascading_failures)
        
        # Check for network partitions
        if random.random() < self.network_partition_probability / 3600.0:  # Per second probability
            partition_failures = self._create_network_partition(current_time, state)
            new_failures.extend(partition_failures)
        
        return new_failures
    
    def _should_inject_failure(self, node_id: NodeId, current_time: float, state: SimulationState) -> bool:
        """Determine if a failure should be injected on a specific node"""
        profile = self.failure_profiles[node_id]
        
        # Base probability per second
        base_prob_per_second = profile.base_failure_rate / 3600.0
        
        # Adjust probability based on node load
        load_factor = self._calculate_load_factor(node_id, state)
        adjusted_prob = base_prob_per_second * (1.0 + load_factor)
        
        # Adjust probability based on time since last failure (recovery period)
        time_since_last_failure = current_time - self.last_failure_times.get(node_id, 0)
        if time_since_last_failure < 300.0:  # 5 minute recovery period
            recovery_factor = time_since_last_failure / 300.0
            adjusted_prob *= recovery_factor
        
        # Random decision
        return random.random() < adjusted_prob
    
    def _calculate_load_factor(self, node_id: NodeId, state: SimulationState) -> float:
        """Calculate load factor that influences failure probability"""
        metrics = state.load_balancing_metrics
        
        cpu_load = metrics.cpu_distribution.get(node_id, 0.0)
        memory_load = metrics.memory_distribution.get(node_id, 0.0)
        transaction_load = metrics.transaction_distribution.get(node_id, 0)
        
        # Normalize loads (higher load = higher failure probability)
        node_metrics = state.node_metrics.get(node_id)
        if not node_metrics:
            return 0.0
        
        cpu_ratio = cpu_load / node_metrics.cpu_utilization if node_metrics.cpu_utilization > 0 else 0
        memory_ratio = memory_load / node_metrics.memory_usage if node_metrics.memory_usage > 0 else 0
        transaction_ratio = transaction_load / node_metrics.transactions_per_sec if node_metrics.transactions_per_sec > 0 else 0
        
        # Combined load factor (0.0 to 2.0, where 1.0 is normal load)
        load_factor = (cpu_ratio + memory_ratio + transaction_ratio) / 3.0
        return min(2.0, max(0.0, load_factor))
    
    def _create_failure_scenario(self, node_id: NodeId, current_time: float) -> FailureScenario:
        """Create a failure scenario for a specific node"""
        profile = self.failure_profiles[node_id]
        
        # Generate failure duration using exponential distribution around mean
        duration = random.expovariate(1.0 / profile.mean_duration)
        duration = max(5.0, min(600.0, duration))  # Clamp between 5 seconds and 10 minutes
        
        # Generate severity within range
        severity = random.uniform(*profile.severity_range)
        
        return FailureScenario(
            node_id=node_id,
            failure_type=profile.failure_type,
            start_time=current_time,
            duration=duration,
            severity=severity
        )
    
    def _evaluate_cascading_failures(self, initial_failures: List[FailureScenario], 
                                   current_time: float, state: SimulationState) -> List[FailureScenario]:
        """Evaluate potential cascading failures"""
        cascading_failures = []
        
        for failure in initial_failures:
            if random.random() < self.cascading_failure_probability:
                # Find dependent nodes that might cascade
                dependent_nodes = self._get_dependent_nodes(failure.node_id)
                
                for dependent_node in dependent_nodes:
                    if dependent_node not in state.failed_nodes and dependent_node not in [f.node_id for f in initial_failures]:
                        # Create cascading failure with reduced severity
                        cascading_failure = FailureScenario(
                            node_id=dependent_node,
                            failure_type=self.failure_profiles[dependent_node].failure_type,
                            start_time=current_time + random.uniform(5.0, 30.0),  # Delayed start
                            duration=failure.duration * 0.7,  # Shorter duration
                            severity=failure.severity * 0.6  # Reduced severity
                        )
                        cascading_failures.append(cascading_failure)
                        
                        self.logger.warning("Cascading failure: %s -> %s", 
                                          failure.node_id.value, dependent_node.value)
        
        return cascading_failures
    
    def _get_dependent_nodes(self, failed_node: NodeId) -> List[NodeId]:
        """Get nodes that depend on the failed node"""
        # Define dependency relationships based on architecture
        dependencies = {
            NodeId.CORE1: [NodeId.EDGE1, NodeId.EDGE2],  # Core1 failure affects edges
            NodeId.CORE2: [NodeId.EDGE1, NodeId.EDGE2],  # Core2 failure affects edges
            NodeId.CLOUD1: [NodeId.CORE1, NodeId.CORE2],  # Cloud1 failure affects cores
            NodeId.EDGE1: [],  # Edge failures don't cascade
            NodeId.EDGE2: []   # Edge failures don't cascade
        }
        
        return dependencies.get(failed_node, [])
    
    def _create_network_partition(self, current_time: float, state: SimulationState) -> List[FailureScenario]:
        """Create network partition scenario"""
        partition_failures = []
        
        # Randomly partition nodes into two groups
        all_nodes = list(NodeId)
        random.shuffle(all_nodes)
        partition_size = random.randint(1, len(all_nodes) - 1)
        
        partitioned_nodes = all_nodes[:partition_size]
        
        # Create network partition failures for partitioned nodes
        partition_duration = random.uniform(60.0, 300.0)  # 1-5 minutes
        
        for node in partitioned_nodes:
            if node not in state.failed_nodes:
                partition_failure = FailureScenario(
                    node_id=node,
                    failure_type=FailureType.NETWORK_PARTITION,
                    start_time=current_time,
                    duration=partition_duration,
                    severity=0.5  # Medium severity for partition
                )
                partition_failures.append(partition_failure)
        
        if partition_failures:
            self.logger.warning("Network partition created affecting nodes: %s", 
                              [f.node_id.value for f in partition_failures])
        
        return partition_failures
    
    def inject_specific_failure(self, node_id: NodeId, failure_type: FailureType, 
                              duration: float, severity: float, start_time: float) -> FailureScenario:
        """Inject a specific failure scenario for testing"""
        failure = FailureScenario(
            node_id=node_id,
            failure_type=failure_type,
            start_time=start_time,
            duration=duration,
            severity=severity
        )
        
        self.failure_history.append(failure)
        self.last_failure_times[node_id] = start_time
        
        self.logger.info("Manually injected %s failure on node %s", 
                        failure_type.value, node_id.value)
        
        return failure
    
    def get_failure_statistics(self) -> Dict:
        """Get failure injection statistics"""
        if not self.failure_history:
            return {}
        
        stats = {
            'total_failures': len(self.failure_history),
            'failures_by_type': {},
            'failures_by_node': {},
            'average_duration': 0.0,
            'average_severity': 0.0
        }
        
        # Count by type and node
        for failure in self.failure_history:
            failure_type = failure.failure_type.value
            node_id = failure.node_id.value
            
            stats['failures_by_type'][failure_type] = stats['failures_by_type'].get(failure_type, 0) + 1
            stats['failures_by_node'][node_id] = stats['failures_by_node'].get(node_id, 0) + 1
        
        # Calculate averages
        total_duration = sum(f.duration for f in self.failure_history)
        total_severity = sum(f.severity for f in self.failure_history)
        
        stats['average_duration'] = total_duration / len(self.failure_history)
        stats['average_severity'] = total_severity / len(self.failure_history)
        
        return stats
    
    def reset(self):
        """Reset failure injection state"""
        self.failure_history.clear()
        self.last_failure_times.clear()
        self.logger.info("FailureInjector state reset")