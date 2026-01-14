"""
Adaptive migration engine for dynamic service migration based on
load balancing metrics and system conditions.
"""

import time
import random
import logging
from typing import List, Dict, Optional, Set
from dataclasses import dataclass
from collections import defaultdict

from models import (
    NodeId, ServiceRequest, MigrationEvent, SimulationState,
    LoadBalancingMetrics, NodeMetrics
)


@dataclass
class MigrationDecision:
    """Decision to migrate a service"""
    service_id: str
    source_node: NodeId
    destination_node: NodeId
    reason: str
    priority: float  # 0.0-1.0, higher = more urgent
    estimated_benefit: float  # Expected improvement metric


class AdaptiveMigrationEngine:
    """
    Adaptive migration engine that evaluates and executes service migrations
    to optimize load distribution and system performance.
    """
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        
        # Migration configuration
        self.migration_threshold = 0.7  # Migrate when load imbalance > 70%
        self.migration_cooldown = 30.0  # 30 seconds between migrations per service
        self.max_concurrent_migrations = 3  # Maximum concurrent migrations
        self.migration_success_rate = 0.95  # 95% migration success rate
        
        # Migration tracking
        self.last_migration_times: Dict[str, float] = {}
        self.active_migrations: Set[str] = set()
        self.migration_history: List[MigrationEvent] = []
        
        # Performance tracking for migration decisions
        self.node_performance_history = defaultdict(list)
        self.load_imbalance_history = []
        
        self.logger.info("AdaptiveMigrationEngine initialized")
    
    def evaluate_migrations(self, state: SimulationState) -> List[MigrationEvent]:
        """
        Evaluate potential service migrations based on current system state.
        
        Args:
            state: Current simulation state
            
        Returns:
            List of migration events to execute
        """
        current_time = time.time()
        migrations = []
        
        # Skip if too many concurrent migrations
        if len(self.active_migrations) >= self.max_concurrent_migrations:
            return migrations
        
        # Update performance history
        self._update_performance_history(state)
        
        # Identify overloaded and underloaded nodes
        overloaded_nodes = self._identify_overloaded_nodes(state)
        underloaded_nodes = self._identify_underloaded_nodes(state)
        
        if not overloaded_nodes or not underloaded_nodes:
            return migrations
        
        # Generate migration decisions
        migration_decisions = self._generate_migration_decisions(
            state, overloaded_nodes, underloaded_nodes, current_time
        )
        
        # Execute top priority migrations
        for decision in sorted(migration_decisions, key=lambda d: d.priority, reverse=True):
            if len(migrations) >= self.max_concurrent_migrations:
                break
            
            migration = self._execute_migration_decision(decision, current_time)
            if migration:
                migrations.append(migration)
                self.active_migrations.add(decision.service_id)
        
        return migrations
    
    def _update_performance_history(self, state: SimulationState):
        """Update performance history for trend analysis"""
        current_time = time.time()
        
        # Record node utilization
        for node_id in NodeId:
            cpu_load = state.load_balancing_metrics.cpu_distribution.get(node_id, 0.0)
            self.node_performance_history[node_id].append((current_time, cpu_load))
            
            # Keep only recent history (last 5 minutes)
            cutoff_time = current_time - 300.0
            self.node_performance_history[node_id] = [
                (t, load) for t, load in self.node_performance_history[node_id]
                if t >= cutoff_time
            ]
        
        # Record load balance index
        load_balance_index = state.load_balancing_metrics.load_balance_index
        self.load_imbalance_history.append((current_time, 1.0 - load_balance_index))
        
        # Keep only recent history
        cutoff_time = current_time - 300.0
        self.load_imbalance_history = [
            (t, imbalance) for t, imbalance in self.load_imbalance_history
            if t >= cutoff_time
        ]
    
    def _identify_overloaded_nodes(self, state: SimulationState) -> List[NodeId]:
        """Identify nodes that are overloaded and candidates for migration source"""
        overloaded_nodes = []
        
        for node_id in state.active_nodes:
            if node_id in state.failed_nodes:
                continue
            
            node_metrics = state.node_metrics.get(node_id)
            if not node_metrics:
                continue
            
            # Check CPU utilization
            current_cpu = state.load_balancing_metrics.cpu_distribution.get(node_id, 0.0)
            cpu_threshold = node_metrics.cpu_utilization * 0.8  # 80% of capacity
            
            # Check memory utilization
            current_memory = state.load_balancing_metrics.memory_distribution.get(node_id, 0.0)
            memory_threshold = node_metrics.memory_usage * 0.8  # 80% of capacity
            
            # Check transaction load
            current_transactions = state.load_balancing_metrics.transaction_distribution.get(node_id, 0)
            transaction_threshold = node_metrics.transactions_per_sec * 0.8  # 80% of capacity
            
            # Node is overloaded if any resource exceeds threshold
            if (current_cpu > cpu_threshold or 
                current_memory > memory_threshold or 
                current_transactions > transaction_threshold):
                
                overloaded_nodes.append(node_id)
                self.logger.debug("Node %s identified as overloaded (CPU: %.1f/%.1f, Mem: %.1f/%.1f, Tx: %d/%d)",
                                node_id.value, current_cpu, cpu_threshold,
                                current_memory, memory_threshold,
                                current_transactions, transaction_threshold)
        
        return overloaded_nodes
    
    def _identify_underloaded_nodes(self, state: SimulationState) -> List[NodeId]:
        """Identify nodes that are underloaded and candidates for migration destination"""
        underloaded_nodes = []
        
        for node_id in state.active_nodes:
            if node_id in state.failed_nodes:
                continue
            
            node_metrics = state.node_metrics.get(node_id)
            if not node_metrics:
                continue
            
            # Check CPU utilization
            current_cpu = state.load_balancing_metrics.cpu_distribution.get(node_id, 0.0)
            cpu_threshold = node_metrics.cpu_utilization * 0.5  # 50% of capacity
            
            # Check memory utilization
            current_memory = state.load_balancing_metrics.memory_distribution.get(node_id, 0.0)
            memory_threshold = node_metrics.memory_usage * 0.5  # 50% of capacity
            
            # Check transaction load
            current_transactions = state.load_balancing_metrics.transaction_distribution.get(node_id, 0)
            transaction_threshold = node_metrics.transactions_per_sec * 0.5  # 50% of capacity
            
            # Node is underloaded if all resources are below threshold
            if (current_cpu < cpu_threshold and 
                current_memory < memory_threshold and 
                current_transactions < transaction_threshold):
                
                underloaded_nodes.append(node_id)
                self.logger.debug("Node %s identified as underloaded (CPU: %.1f/%.1f, Mem: %.1f/%.1f, Tx: %d/%d)",
                                node_id.value, current_cpu, cpu_threshold,
                                current_memory, memory_threshold,
                                current_transactions, transaction_threshold)
        
        return underloaded_nodes
    
    def _generate_migration_decisions(self, state: SimulationState, 
                                    overloaded_nodes: List[NodeId],
                                    underloaded_nodes: List[NodeId],
                                    current_time: float) -> List[MigrationDecision]:
        """Generate migration decisions for load balancing"""
        decisions = []
        
        for source_node in overloaded_nodes:
            # Find services on this node that can be migrated
            migratable_services = self._find_migratable_services(source_node, state, current_time)
            
            for service_id in migratable_services:
                # Find best destination node
                best_destination = self._find_best_destination(
                    service_id, source_node, underloaded_nodes, state
                )
                
                if best_destination:
                    # Calculate migration priority and benefit
                    priority = self._calculate_migration_priority(
                        service_id, source_node, best_destination, state
                    )
                    
                    estimated_benefit = self._estimate_migration_benefit(
                        service_id, source_node, best_destination, state
                    )
                    
                    decision = MigrationDecision(
                        service_id=service_id,
                        source_node=source_node,
                        destination_node=best_destination,
                        reason=f"Load balancing: {source_node.value} overloaded",
                        priority=priority,
                        estimated_benefit=estimated_benefit
                    )
                    
                    decisions.append(decision)
        
        return decisions
    
    def _find_migratable_services(self, node_id: NodeId, state: SimulationState, 
                                current_time: float) -> List[str]:
        """Find services on a node that can be migrated"""
        migratable_services = []
        
        for service_id, allocated_node in state.service_allocations.items():
            if allocated_node != node_id:
                continue
            
            # Check if service is in cooldown period
            last_migration_time = self.last_migration_times.get(service_id, 0)
            if current_time - last_migration_time < self.migration_cooldown:
                continue
            
            # Check if service is already being migrated
            if service_id in self.active_migrations:
                continue
            
            migratable_services.append(service_id)
        
        return migratable_services
    
    def _find_best_destination(self, service_id: str, source_node: NodeId,
                             underloaded_nodes: List[NodeId], 
                             state: SimulationState) -> Optional[NodeId]:
        """Find the best destination node for a service migration"""
        if not underloaded_nodes:
            return None
        
        best_node = None
        best_score = float('-inf')
        
        for destination_node in underloaded_nodes:
            if destination_node == source_node:
                continue
            
            score = self._calculate_destination_score(
                service_id, source_node, destination_node, state
            )
            
            if score > best_score:
                best_score = score
                best_node = destination_node
        
        return best_node
    
    def _calculate_destination_score(self, service_id: str, source_node: NodeId,
                                   destination_node: NodeId, state: SimulationState) -> float:
        """Calculate score for a potential migration destination"""
        dest_metrics = state.node_metrics.get(destination_node)
        if not dest_metrics:
            return 0.0
        
        # Current utilization (lower is better)
        current_cpu = state.load_balancing_metrics.cpu_distribution.get(destination_node, 0.0)
        current_memory = state.load_balancing_metrics.memory_distribution.get(destination_node, 0.0)
        current_transactions = state.load_balancing_metrics.transaction_distribution.get(destination_node, 0)
        
        cpu_utilization = current_cpu / dest_metrics.cpu_utilization if dest_metrics.cpu_utilization > 0 else 0
        memory_utilization = current_memory / dest_metrics.memory_usage if dest_metrics.memory_usage > 0 else 0
        transaction_utilization = current_transactions / dest_metrics.transactions_per_sec if dest_metrics.transactions_per_sec > 0 else 0
        
        # Capacity score (higher available capacity = better)
        capacity_score = (1.0 - cpu_utilization) * 0.4 + (1.0 - memory_utilization) * 0.3 + (1.0 - transaction_utilization) * 0.3
        
        # Performance score (better performance = higher score)
        performance_score = (1.0 / dest_metrics.latency) * (dest_metrics.throughput / 1000.0)
        
        # Reliability score (based on failure characteristics)
        reliability_score = self._get_node_reliability_score(destination_node)
        
        # Network proximity score (closer nodes = better)
        proximity_score = self._get_network_proximity_score(source_node, destination_node)
        
        # Combined score
        total_score = (capacity_score * 0.4 + 
                      performance_score * 0.3 + 
                      reliability_score * 0.2 + 
                      proximity_score * 0.1)
        
        return total_score
    
    def _get_node_reliability_score(self, node_id: NodeId) -> float:
        """Get reliability score for a node based on failure characteristics"""
        # Higher score for more reliable nodes
        reliability_scores = {
            NodeId.EDGE1: 0.9,   # Crash failures - predictable
            NodeId.EDGE2: 0.7,   # Omission failures - less predictable
            NodeId.CORE1: 0.6,   # Byzantine failures - least predictable
            NodeId.CORE2: 0.8,   # Crash failures - predictable
            NodeId.CLOUD1: 0.7   # Omission failures - less predictable
        }
        return reliability_scores.get(node_id, 0.5)
    
    def _get_network_proximity_score(self, source: NodeId, destination: NodeId) -> float:
        """Get network proximity score (higher = closer)"""
        # Same layer nodes are closer
        edge_nodes = {NodeId.EDGE1, NodeId.EDGE2}
        core_nodes = {NodeId.CORE1, NodeId.CORE2}
        cloud_nodes = {NodeId.CLOUD1}
        
        source_layer = self._get_node_layer(source)
        dest_layer = self._get_node_layer(destination)
        
        if source_layer == dest_layer:
            return 1.0  # Same layer
        elif abs(self._get_layer_distance(source_layer, dest_layer)) == 1:
            return 0.7  # Adjacent layers
        else:
            return 0.4  # Distant layers
    
    def _get_node_layer(self, node: NodeId) -> int:
        """Get numeric layer for distance calculation"""
        if node in {NodeId.EDGE1, NodeId.EDGE2}:
            return 0  # Edge layer
        elif node in {NodeId.CORE1, NodeId.CORE2}:
            return 1  # Core layer
        else:  # CLOUD1
            return 2  # Cloud layer
    
    def _get_layer_distance(self, layer1: int, layer2: int) -> int:
        """Get distance between network layers"""
        return abs(layer1 - layer2)
    
    def _calculate_migration_priority(self, service_id: str, source_node: NodeId,
                                    destination_node: NodeId, state: SimulationState) -> float:
        """Calculate migration priority (0.0-1.0)"""
        source_metrics = state.node_metrics.get(source_node)
        if not source_metrics:
            return 0.0
        
        # Priority based on source node overload severity
        current_cpu = state.load_balancing_metrics.cpu_distribution.get(source_node, 0.0)
        cpu_overload = max(0.0, current_cpu - source_metrics.cpu_utilization * 0.8)
        cpu_overload_ratio = cpu_overload / (source_metrics.cpu_utilization * 0.2) if source_metrics.cpu_utilization > 0 else 0
        
        current_memory = state.load_balancing_metrics.memory_distribution.get(source_node, 0.0)
        memory_overload = max(0.0, current_memory - source_metrics.memory_usage * 0.8)
        memory_overload_ratio = memory_overload / (source_metrics.memory_usage * 0.2) if source_metrics.memory_usage > 0 else 0
        
        # Higher overload = higher priority
        overload_priority = min(1.0, max(cpu_overload_ratio, memory_overload_ratio))
        
        # Add urgency based on trend (increasing load = higher priority)
        trend_priority = self._calculate_load_trend_priority(source_node)
        
        # Combined priority
        priority = overload_priority * 0.7 + trend_priority * 0.3
        
        return min(1.0, max(0.0, priority))
    
    def _calculate_load_trend_priority(self, node_id: NodeId) -> float:
        """Calculate priority based on load trend"""
        history = self.node_performance_history.get(node_id, [])
        if len(history) < 2:
            return 0.0
        
        # Calculate load trend over recent history
        recent_loads = [load for _, load in history[-10:]]  # Last 10 measurements
        if len(recent_loads) < 2:
            return 0.0
        
        # Simple linear trend calculation
        trend = (recent_loads[-1] - recent_loads[0]) / len(recent_loads)
        
        # Positive trend (increasing load) increases priority
        return min(1.0, max(0.0, trend / 10.0))  # Normalize to 0-1 range
    
    def _estimate_migration_benefit(self, service_id: str, source_node: NodeId,
                                  destination_node: NodeId, state: SimulationState) -> float:
        """Estimate the benefit of migrating a service"""
        # Estimate load reduction on source node
        # This is simplified - in practice, you'd need service-specific resource usage
        estimated_cpu_reduction = 5.0  # Assume 5% CPU reduction
        estimated_memory_reduction = 0.5  # Assume 0.5GB memory reduction
        
        source_metrics = state.node_metrics.get(source_node)
        if not source_metrics:
            return 0.0
        
        # Calculate benefit as percentage of capacity freed
        cpu_benefit = estimated_cpu_reduction / source_metrics.cpu_utilization if source_metrics.cpu_utilization > 0 else 0
        memory_benefit = estimated_memory_reduction / source_metrics.memory_usage if source_metrics.memory_usage > 0 else 0
        
        # Combined benefit
        benefit = (cpu_benefit + memory_benefit) / 2.0
        
        return min(1.0, max(0.0, benefit))
    
    def _execute_migration_decision(self, decision: MigrationDecision, 
                                  current_time: float) -> Optional[MigrationEvent]:
        """Execute a migration decision"""
        # Simulate migration success/failure
        success = random.random() < self.migration_success_rate
        
        # Simulate migration time (1-10 seconds)
        migration_duration = random.uniform(1.0, 10.0)
        completion_time = current_time + migration_duration
        
        migration = MigrationEvent(
            service_id=decision.service_id,
            source_node=decision.source_node,
            destination_node=decision.destination_node,
            start_time=current_time,
            completion_time=completion_time if success else None,
            success=success,
            reason=decision.reason
        )
        
        # Update tracking
        self.last_migration_times[decision.service_id] = current_time
        self.migration_history.append(migration)
        
        if success:
            self.logger.info("Migration started: %s from %s to %s (priority: %.2f, benefit: %.2f)",
                           decision.service_id, decision.source_node.value,
                           decision.destination_node.value, decision.priority, decision.estimated_benefit)
        else:
            self.logger.warning("Migration failed: %s from %s to %s",
                              decision.service_id, decision.source_node.value,
                              decision.destination_node.value)
            # Remove from active migrations since it failed
            self.active_migrations.discard(decision.service_id)
        
        return migration
    
    def complete_migration(self, service_id: str):
        """Mark a migration as completed"""
        self.active_migrations.discard(service_id)
        self.logger.debug("Migration completed for service: %s", service_id)
    
    def get_migration_statistics(self) -> Dict:
        """Get migration statistics"""
        if not self.migration_history:
            return {}
        
        total_migrations = len(self.migration_history)
        successful_migrations = sum(1 for m in self.migration_history if m.success)
        
        stats = {
            'total_migrations': total_migrations,
            'successful_migrations': successful_migrations,
            'failed_migrations': total_migrations - successful_migrations,
            'success_rate': successful_migrations / total_migrations if total_migrations > 0 else 0,
            'active_migrations': len(self.active_migrations),
            'migrations_by_source': {},
            'migrations_by_destination': {}
        }
        
        # Count by source and destination
        for migration in self.migration_history:
            source = migration.source_node.value
            destination = migration.destination_node.value
            
            stats['migrations_by_source'][source] = stats['migrations_by_source'].get(source, 0) + 1
            stats['migrations_by_destination'][destination] = stats['migrations_by_destination'].get(destination, 0) + 1
        
        return stats
    
    def reset(self):
        """Reset adaptive migration engine state"""
        self.last_migration_times.clear()
        self.active_migrations.clear()
        self.migration_history.clear()
        self.node_performance_history.clear()
        self.load_imbalance_history.clear()
        self.logger.info("AdaptiveMigrationEngine state reset")