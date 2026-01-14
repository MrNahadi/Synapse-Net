"""
Data models for Python load balancing simulation.
These models mirror the Java models for integration.
"""

from dataclasses import dataclass
from enum import Enum
from typing import Dict, List, Optional, Set
import time
from datetime import datetime


class NodeId(Enum):
    """Node identifiers matching Java NodeId enum"""
    EDGE1 = "EDGE1"
    EDGE2 = "EDGE2" 
    CORE1 = "CORE1"
    CORE2 = "CORE2"
    CLOUD1 = "CLOUD1"


class FailureType(Enum):
    """Failure types matching Java FailureType enum"""
    CRASH = "CRASH"
    OMISSION = "OMISSION"
    BYZANTINE = "BYZANTINE"
    NETWORK_PARTITION = "NETWORK_PARTITION"


class TrafficPatternType(Enum):
    """Traffic pattern types"""
    BURST = "BURST"
    STEADY = "STEADY"
    DECLINING = "DECLINING"


class LoadBalancingStrategy(Enum):
    """Load balancing strategies matching Java implementation"""
    WEIGHTED_ROUND_ROBIN = "weighted_round_robin"
    LEAST_CONNECTIONS = "least_connections"
    RESOURCE_AWARE = "resource_aware"


@dataclass
class NodeMetrics:
    """Node performance metrics matching Java NodeMetrics"""
    latency: float  # milliseconds
    throughput: float  # Mbps
    packet_loss: float  # percentage
    cpu_utilization: float  # percentage
    memory_usage: float  # GB
    transactions_per_sec: int  # tx/sec
    lock_contention: float  # percentage
    timestamp: float = None
    
    def __post_init__(self):
        if self.timestamp is None:
            self.timestamp = time.time()


@dataclass
class ServiceRequest:
    """Service request matching Java ServiceRequest"""
    service_id: str
    cpu_requirement: float
    memory_requirement: float
    transaction_load: int
    priority: int
    timestamp: float = None
    
    def __post_init__(self):
        if self.timestamp is None:
            self.timestamp = time.time()


@dataclass
class TrafficPattern:
    """Traffic pattern for dynamic adaptation"""
    pattern_type: TrafficPatternType
    intensity: float  # 0-100 scale
    duration: float  # seconds
    start_time: float = None
    
    def __post_init__(self):
        if self.start_time is None:
            self.start_time = time.time()


@dataclass
class FailureScenario:
    """Failure injection scenario"""
    node_id: NodeId
    failure_type: FailureType
    start_time: float
    duration: float  # seconds
    severity: float  # 0.0-1.0
    recovery_time: float = None


@dataclass
class NetworkDelay:
    """Network delay configuration between nodes"""
    source: NodeId
    destination: NodeId
    base_delay: float  # milliseconds
    jitter: float  # milliseconds
    packet_loss_rate: float  # 0.0-1.0


@dataclass
class MigrationEvent:
    """Service migration event"""
    service_id: str
    source_node: NodeId
    destination_node: NodeId
    start_time: float
    completion_time: Optional[float] = None
    success: bool = True
    reason: str = ""


@dataclass
class LoadBalancingMetrics:
    """Load balancing metrics matching Java LoadBalancingMetrics"""
    cpu_distribution: Dict[NodeId, float]
    memory_distribution: Dict[NodeId, float]
    transaction_distribution: Dict[NodeId, int]
    load_balance_index: float  # 0.0-1.0, higher is more balanced
    total_services: int
    migrations: int
    last_update_timestamp: float
    
    def __post_init__(self):
        if self.last_update_timestamp is None:
            self.last_update_timestamp = time.time()


@dataclass
class SimulationState:
    """Current state of the simulation"""
    current_time: float
    active_nodes: Set[NodeId]
    failed_nodes: Set[NodeId]
    node_metrics: Dict[NodeId, NodeMetrics]
    service_allocations: Dict[str, NodeId]
    active_failures: List[FailureScenario]
    migration_history: List[MigrationEvent]
    load_balancing_metrics: LoadBalancingMetrics
    
    def __post_init__(self):
        if self.current_time is None:
            self.current_time = time.time()


@dataclass
class SimulationConfig:
    """Configuration for load balancing simulation"""
    simulation_duration: float  # seconds
    time_step: float  # seconds
    failure_injection_enabled: bool = True
    network_delay_simulation_enabled: bool = True
    adaptive_migration_enabled: bool = True
    java_integration_enabled: bool = True
    
    # Node baseline characteristics from dataset
    node_baseline_metrics: Dict[NodeId, NodeMetrics] = None
    
    def __post_init__(self):
        if self.node_baseline_metrics is None:
            self.node_baseline_metrics = {
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


@dataclass
class SimulationResult:
    """Results from load balancing simulation"""
    total_requests_processed: int
    successful_requests: int
    failed_requests: int
    average_response_time: float
    total_migrations: int
    successful_migrations: int
    failed_migrations: int
    node_utilization_history: Dict[NodeId, List[float]]
    load_balance_index_history: List[float]
    failure_recovery_times: List[float]
    simulation_duration: float
    final_state: SimulationState