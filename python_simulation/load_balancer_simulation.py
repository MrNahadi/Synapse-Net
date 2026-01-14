"""
Main load balancing simulation framework for dynamic process allocation.
Integrates with Java load balancing strategy from task 9.
"""

import time
import random
import logging
import math
from typing import Dict, List, Optional, Set
from collections import defaultdict, deque

from models import (
    NodeId, FailureType, TrafficPatternType, LoadBalancingStrategy,
    NodeMetrics, ServiceRequest, TrafficPattern, SimulationState,
    SimulationConfig, SimulationResult, LoadBalancingMetrics
)
from failure_injection import FailureInjector
from network_delay_simulator import NetworkDelaySimulator
from adaptive_migration import AdaptiveMigrationEngine
from java_integration import JavaLoadBalancerIntegration


class LoadBalancerSimulation:
    """
    Main simulation framework for dynamic load balancing with failure injection,
    network delay simulation, and adaptive migration.
    """
    
    def __init__(self, config: SimulationConfig):
        self.config = config
        self.logger = logging.getLogger(__name__)
        
        # Initialize simulation state
        self.state = SimulationState(
            current_time=time.time(),
            active_nodes=set(NodeId),
            failed_nodes=set(),
            node_metrics=config.node_baseline_metrics.copy(),
            service_allocations={},
            active_failures=[],
            migration_history=[],
            load_balancing_metrics=LoadBalancingMetrics(
                cpu_distribution={node: 0.0 for node in NodeId},
                memory_distribution={node: 0.0 for node in NodeId},
                transaction_distribution={node: 0 for node in NodeId},
                load_balance_index=1.0,
                total_services=0,
                migrations=0,
                last_update_timestamp=time.time()
            )
        )
        
        # Initialize components
        self.failure_injector = FailureInjector() if config.failure_injection_enabled else None
        self.network_delay_simulator = NetworkDelaySimulator() if config.network_delay_simulation_enabled else None
        self.adaptive_migration_engine = AdaptiveMigrationEngine() if config.adaptive_migration_enabled else None
        self.java_integration = JavaLoadBalancerIntegration() if config.java_integration_enabled else None
        
        # Simulation tracking
        self.request_history = deque(maxlen=10000)
        self.performance_history = defaultdict(list)
        self.load_balance_history = []
        
        # Current load balancing strategy
        self.current_strategy = LoadBalancingStrategy.RESOURCE_AWARE
        self.node_weights = self._initialize_node_weights()
        
        self.logger.info("LoadBalancerSimulation initialized with config: %s", config)
    
    def _initialize_node_weights(self) -> Dict[NodeId, float]:
        """Initialize node weights based on baseline characteristics"""
        weights = {}
        for node_id, metrics in self.config.node_baseline_metrics.items():
            # Weight based on throughput/latency ratio and reliability
            base_weight = (metrics.throughput / metrics.latency) / 100.0
            
            # Adjust for failure characteristics
            if node_id in [NodeId.EDGE1, NodeId.CORE2]:  # Crash failures
                reliability_factor = 0.9
            elif node_id in [NodeId.EDGE2, NodeId.CLOUD1]:  # Omission failures
                reliability_factor = 0.8
            else:  # Byzantine failures (CORE1)
                reliability_factor = 0.7
            
            weights[node_id] = base_weight * reliability_factor
        
        return weights
    
    def run_simulation(self) -> SimulationResult:
        """
        Run the complete load balancing simulation.
        
        Returns:
            SimulationResult: Complete simulation results
        """
        self.logger.info("Starting load balancing simulation for %.2f seconds", 
                        self.config.simulation_duration)
        
        start_time = time.time()
        simulation_end_time = start_time + self.config.simulation_duration
        
        total_requests = 0
        successful_requests = 0
        failed_requests = 0
        response_times = []
        
        while time.time() < simulation_end_time:
            current_time = time.time()
            self.state.current_time = current_time
            
            # Generate service requests based on traffic patterns
            requests = self._generate_service_requests(current_time)
            
            # Process each request
            for request in requests:
                total_requests += 1
                success, response_time = self._process_service_request(request)
                
                if success:
                    successful_requests += 1
                    response_times.append(response_time)
                else:
                    failed_requests += 1
            
            # Update system state
            self._update_system_state(current_time)
            
            # Record performance metrics
            self._record_performance_metrics(current_time)
            
            # Sleep for time step
            time.sleep(self.config.time_step)
        
        # Calculate final results
        simulation_duration = time.time() - start_time
        average_response_time = sum(response_times) / len(response_times) if response_times else 0.0
        
        result = SimulationResult(
            total_requests_processed=total_requests,
            successful_requests=successful_requests,
            failed_requests=failed_requests,
            average_response_time=average_response_time,
            total_migrations=len(self.state.migration_history),
            successful_migrations=sum(1 for m in self.state.migration_history if m.success),
            failed_migrations=sum(1 for m in self.state.migration_history if not m.success),
            node_utilization_history=dict(self.performance_history),
            load_balance_index_history=self.load_balance_history,
            failure_recovery_times=self._calculate_failure_recovery_times(),
            simulation_duration=simulation_duration,
            final_state=self.state
        )
        
        self.logger.info("Simulation completed: %d requests processed, %.2f%% success rate",
                        total_requests, (successful_requests / total_requests * 100) if total_requests > 0 else 0)
        
        return result
    
    def _generate_service_requests(self, current_time: float) -> List[ServiceRequest]:
        """Generate service requests based on current traffic patterns"""
        requests = []
        
        # Base request rate (requests per time step)
        base_rate = 5
        
        # Adjust rate based on time of day simulation
        time_factor = 1.0 + 0.5 * math.sin((current_time % 86400) / 86400 * 2 * math.pi)
        
        # Generate random number of requests using exponential distribution
        # (approximates Poisson for small rates)
        lambda_rate = base_rate * time_factor
        num_requests = max(0, int(random.expovariate(1.0 / lambda_rate)) if lambda_rate > 0 else 0)
        
        for i in range(num_requests):
            request = ServiceRequest(
                service_id=f"service_{current_time}_{i}",
                cpu_requirement=random.uniform(5.0, 25.0),  # 5-25% CPU
                memory_requirement=random.uniform(0.5, 3.0),  # 0.5-3GB memory
                transaction_load=random.randint(1, 10),  # 1-10 transactions
                priority=random.randint(1, 10),  # Priority 1-10
                timestamp=current_time
            )
            requests.append(request)
        
        return requests
    
    def _process_service_request(self, request: ServiceRequest) -> tuple[bool, float]:
        """
        Process a service request using the current load balancing strategy.
        
        Returns:
            tuple: (success, response_time)
        """
        start_time = time.time()
        
        try:
            # Select node using current strategy
            selected_node = self._select_node_for_request(request)
            
            if selected_node is None:
                self.logger.warning("No available node for request %s", request.service_id)
                return False, 0.0
            
            # Check if node is available (not failed)
            if selected_node in self.state.failed_nodes:
                self.logger.warning("Selected node %s is failed, rejecting request %s", 
                                  selected_node, request.service_id)
                return False, 0.0
            
            # Simulate network delay
            network_delay = self._simulate_network_delay(selected_node)
            
            # Update node load
            self._update_node_load(selected_node, request)
            
            # Record service allocation
            self.state.service_allocations[request.service_id] = selected_node
            
            # Calculate response time
            processing_time = self.state.node_metrics[selected_node].latency / 1000.0  # Convert to seconds
            total_response_time = processing_time + network_delay
            
            self.logger.debug("Allocated service %s to node %s (response time: %.3fs)",
                            request.service_id, selected_node, total_response_time)
            
            return True, total_response_time
            
        except Exception as e:
            self.logger.error("Error processing request %s: %s", request.service_id, e)
            return False, 0.0
    
    def _select_node_for_request(self, request: ServiceRequest) -> Optional[NodeId]:
        """Select the best node for a service request based on current strategy"""
        available_nodes = list(self.state.active_nodes - self.state.failed_nodes)
        
        if not available_nodes:
            return None
        
        if self.current_strategy == LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN:
            return self._select_node_weighted_round_robin(request, available_nodes)
        elif self.current_strategy == LoadBalancingStrategy.LEAST_CONNECTIONS:
            return self._select_node_least_connections(request, available_nodes)
        else:  # RESOURCE_AWARE
            return self._select_node_resource_aware(request, available_nodes)
    
    def _select_node_weighted_round_robin(self, request: ServiceRequest, available_nodes: List[NodeId]) -> NodeId:
        """Select node using weighted round robin strategy"""
        # Create weighted list
        weighted_nodes = []
        for node in available_nodes:
            weight = self.node_weights.get(node, 1.0)
            copies = max(1, int(weight * 10))
            weighted_nodes.extend([node] * copies)
        
        # Select based on round robin
        index = hash(request.service_id) % len(weighted_nodes)
        return weighted_nodes[index]
    
    def _select_node_least_connections(self, request: ServiceRequest, available_nodes: List[NodeId]) -> NodeId:
        """Select node with least connections"""
        connection_counts = defaultdict(int)
        for service_id, node in self.state.service_allocations.items():
            if node in available_nodes:
                connection_counts[node] += 1
        
        return min(available_nodes, key=lambda node: connection_counts[node])
    
    def _select_node_resource_aware(self, request: ServiceRequest, available_nodes: List[NodeId]) -> NodeId:
        """Select node using resource-aware strategy"""
        best_node = None
        best_score = float('-inf')
        
        for node in available_nodes:
            if self._can_accommodate_request(node, request):
                score = self._calculate_node_score(node, request)
                if score > best_score:
                    best_score = score
                    best_node = node
        
        # Fallback to least loaded node if no node can fully accommodate
        if best_node is None:
            best_node = self._find_least_loaded_node(available_nodes)
        
        return best_node
    
    def _can_accommodate_request(self, node: NodeId, request: ServiceRequest) -> bool:
        """Check if node can accommodate the request"""
        metrics = self.state.node_metrics[node]
        current_cpu = self.state.load_balancing_metrics.cpu_distribution[node]
        current_memory = self.state.load_balancing_metrics.memory_distribution[node]
        current_transactions = self.state.load_balancing_metrics.transaction_distribution[node]
        
        cpu_ok = (current_cpu + request.cpu_requirement) <= metrics.cpu_utilization
        memory_ok = (current_memory + request.memory_requirement) <= metrics.memory_usage
        transaction_ok = (current_transactions + request.transaction_load) <= metrics.transactions_per_sec
        
        return cpu_ok and memory_ok and transaction_ok
    
    def _calculate_node_score(self, node: NodeId, request: ServiceRequest) -> float:
        """Calculate score for node selection"""
        metrics = self.state.node_metrics[node]
        weight = self.node_weights.get(node, 1.0)
        
        current_cpu = self.state.load_balancing_metrics.cpu_distribution[node]
        current_memory = self.state.load_balancing_metrics.memory_distribution[node]
        current_transactions = self.state.load_balancing_metrics.transaction_distribution[node]
        
        # Calculate utilization ratios
        cpu_utilization = current_cpu / metrics.cpu_utilization if metrics.cpu_utilization > 0 else 0
        memory_utilization = current_memory / metrics.memory_usage if metrics.memory_usage > 0 else 0
        transaction_utilization = current_transactions / metrics.transactions_per_sec if metrics.transactions_per_sec > 0 else 0
        
        # Capacity score (higher available capacity = better)
        capacity_score = (1.0 - cpu_utilization) * 0.4 + (1.0 - memory_utilization) * 0.3 + (1.0 - transaction_utilization) * 0.3
        
        # Performance score
        performance_score = weight * (1.0 / metrics.latency) * (metrics.throughput / 1000.0)
        
        # Priority bonus
        priority_bonus = request.priority / 10.0
        
        return capacity_score * 0.6 + performance_score * 0.3 + priority_bonus * 0.1
    
    def _find_least_loaded_node(self, available_nodes: List[NodeId]) -> NodeId:
        """Find the least loaded node"""
        return min(available_nodes, key=lambda node: (
            self.state.load_balancing_metrics.cpu_distribution[node] +
            self.state.load_balancing_metrics.memory_distribution[node] +
            self.state.load_balancing_metrics.transaction_distribution[node]
        ))
    
    def _simulate_network_delay(self, node: NodeId) -> float:
        """Simulate network delay to the selected node"""
        if self.network_delay_simulator:
            return self.network_delay_simulator.get_delay_to_node(node)
        else:
            # Use baseline latency with some jitter
            base_latency = self.state.node_metrics[node].latency / 1000.0  # Convert to seconds
            jitter = random.uniform(-0.002, 0.002)  # ±2ms jitter
            return max(0.001, base_latency + jitter)
    
    def _update_node_load(self, node: NodeId, request: ServiceRequest):
        """Update node load after allocating a request"""
        metrics = self.state.load_balancing_metrics
        metrics.cpu_distribution[node] += request.cpu_requirement
        metrics.memory_distribution[node] += request.memory_requirement
        metrics.transaction_distribution[node] += request.transaction_load
        metrics.total_services += 1
        metrics.last_update_timestamp = time.time()
    
    def _update_system_state(self, current_time: float):
        """Update overall system state"""
        # Handle failure injection
        if self.failure_injector:
            new_failures = self.failure_injector.inject_failures(current_time, self.state)
            self.state.active_failures.extend(new_failures)
            
            # Update failed nodes
            for failure in self.state.active_failures:
                if failure.start_time <= current_time <= failure.start_time + failure.duration:
                    self.state.failed_nodes.add(failure.node_id)
                    if failure.node_id in self.state.active_nodes:
                        self.state.active_nodes.remove(failure.node_id)
                elif current_time > failure.start_time + failure.duration:
                    # Recovery
                    if failure.node_id in self.state.failed_nodes:
                        self.state.failed_nodes.remove(failure.node_id)
                        self.state.active_nodes.add(failure.node_id)
                        failure.recovery_time = current_time
        
        # Handle adaptive migration
        if self.adaptive_migration_engine:
            migrations = self.adaptive_migration_engine.evaluate_migrations(self.state)
            self.state.migration_history.extend(migrations)
            
            # Apply successful migrations
            for migration in migrations:
                if migration.success:
                    self.state.service_allocations[migration.service_id] = migration.destination_node
                    self.state.load_balancing_metrics.migrations += 1
        
        # Update load balance index
        self._update_load_balance_index()
    
    def _update_load_balance_index(self):
        """Update the load balance index"""
        cpu_loads = list(self.state.load_balancing_metrics.cpu_distribution.values())
        
        if not cpu_loads or all(load == 0 for load in cpu_loads):
            self.state.load_balancing_metrics.load_balance_index = 1.0
            return
        
        # Calculate coefficient of variation
        mean_load = sum(cpu_loads) / len(cpu_loads)
        if mean_load == 0:
            self.state.load_balancing_metrics.load_balance_index = 1.0
            return
        
        variance = sum((load - mean_load) ** 2 for load in cpu_loads) / len(cpu_loads)
        coefficient_of_variation = (variance ** 0.5) / mean_load
        
        # Convert to balance index (1 = perfectly balanced, 0 = completely unbalanced)
        self.state.load_balancing_metrics.load_balance_index = max(0.0, 1.0 - min(1.0, coefficient_of_variation))
    
    def _record_performance_metrics(self, current_time: float):
        """Record performance metrics for analysis"""
        for node in NodeId:
            cpu_load = self.state.load_balancing_metrics.cpu_distribution[node]
            self.performance_history[node].append(cpu_load)
        
        self.load_balance_history.append(self.state.load_balancing_metrics.load_balance_index)
    
    def _calculate_failure_recovery_times(self) -> List[float]:
        """Calculate failure recovery times"""
        recovery_times = []
        for failure in self.state.active_failures:
            if failure.recovery_time:
                recovery_time = failure.recovery_time - failure.start_time
                recovery_times.append(recovery_time)
        return recovery_times
    
    def set_load_balancing_strategy(self, strategy: LoadBalancingStrategy):
        """Change the load balancing strategy"""
        self.current_strategy = strategy
        self.logger.info("Changed load balancing strategy to: %s", strategy)
    
    def update_node_weights(self, weights: Dict[NodeId, float]):
        """Update node weights for load balancing"""
        self.node_weights.update(weights)
        self.logger.info("Updated node weights: %s", weights)
    
    def get_current_state(self) -> SimulationState:
        """Get current simulation state"""
        return self.state
    
    def get_performance_summary(self) -> Dict:
        """Get performance summary"""
        return {
            'active_nodes': len(self.state.active_nodes),
            'failed_nodes': len(self.state.failed_nodes),
            'total_services': self.state.load_balancing_metrics.total_services,
            'load_balance_index': self.state.load_balancing_metrics.load_balance_index,
            'total_migrations': len(self.state.migration_history),
            'cpu_distribution': dict(self.state.load_balancing_metrics.cpu_distribution),
            'memory_distribution': dict(self.state.load_balancing_metrics.memory_distribution)
        }