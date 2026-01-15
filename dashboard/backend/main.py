"""
FastAPI backend for the Distributed Telecom System Dashboard.
Exposes real-time metrics from the Python simulation.
"""

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Dict, List, Optional
from enum import Enum
import asyncio
import json
import sys
import os
import random
import time
from datetime import datetime

# Add python_simulation to path - handle both running from dashboard/backend and project root
backend_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.dirname(os.path.dirname(backend_dir))
python_sim_path = os.path.join(project_root, 'python_simulation')
if python_sim_path not in sys.path:
    sys.path.insert(0, python_sim_path)

# Try to import simulation modules, fall back to mock mode if unavailable
SIMULATION_AVAILABLE = False
try:
    from models import NodeId, FailureType, NodeMetrics, SimulationConfig
    from load_balancer_simulation import LoadBalancerSimulation
    SIMULATION_AVAILABLE = True
except ImportError as e:
    print(f"Warning: Could not import simulation modules: {e}")
    print("Running in mock data mode only.")
    # Define minimal NodeId enum for mock mode
    class NodeId(Enum):
        EDGE1 = "EDGE1"
        EDGE2 = "EDGE2"
        CORE1 = "CORE1"
        CORE2 = "CORE2"
        CLOUD1 = "CLOUD1"

app = FastAPI(title="Distributed Telecom Dashboard API", version="1.0.0")

# CORS for frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global simulation instance
simulation: Optional[LoadBalancerSimulation] = None
connected_clients: List[WebSocket] = []

class NodeStatus(str, Enum):
    HEALTHY = "healthy"
    WARNING = "warning"
    CRITICAL = "critical"
    FAILED = "failed"

class NodeData(BaseModel):
    id: str
    name: str
    layer: str
    status: NodeStatus
    latency: float
    throughput: float
    cpu: float
    memory: float
    transactions: int
    lockContention: float
    failureType: str

class TransactionData(BaseModel):
    id: str
    status: str
    nodes: List[str]
    startTime: str
    duration: float

class FailoverEvent(BaseModel):
    id: str
    timestamp: str
    sourceNode: str
    targetNode: str
    reason: str
    status: str

class SystemMetrics(BaseModel):
    nodes: List[NodeData]
    loadBalanceIndex: float
    totalServices: int
    activeTransactions: int
    failedNodes: int
    totalMigrations: int

def get_node_layer(node_id: str) -> str:
    if node_id.startswith("EDGE"):
        return "Edge"
    elif node_id.startswith("CORE"):
        return "Core"
    return "Cloud"

def get_failure_type(node_id: str) -> str:
    mapping = {
        "EDGE1": "Crash",
        "EDGE2": "Omission", 
        "CORE1": "Byzantine",
        "CORE2": "Crash",
        "CLOUD1": "Omission"
    }
    return mapping.get(node_id, "Unknown")

def calculate_status(metrics, is_failed: bool) -> NodeStatus:
    if is_failed:
        return NodeStatus.FAILED
    # Handle both real NodeMetrics objects and mock data
    cpu = getattr(metrics, 'cpu_utilization', metrics.get('cpu', 0) if isinstance(metrics, dict) else 0)
    memory = getattr(metrics, 'memory_usage', metrics.get('memory', 0) if isinstance(metrics, dict) else 0)
    latency = getattr(metrics, 'latency', metrics.get('latency', 0) if isinstance(metrics, dict) else 0)
    
    if cpu > 80 or memory > 14:
        return NodeStatus.CRITICAL
    if cpu > 60 or latency > 18:
        return NodeStatus.WARNING
    return NodeStatus.HEALTHY

def get_simulation_metrics() -> SystemMetrics:
    """Get current metrics from simulation or generate mock data."""
    global simulation
    
    if SIMULATION_AVAILABLE and simulation:
        state = simulation.get_current_state()
        nodes = []
        for node_id in NodeId:
            metrics = state.node_metrics.get(node_id)
            if metrics:
                is_failed = node_id in state.failed_nodes
                nodes.append(NodeData(
                    id=node_id.value,
                    name=node_id.value,
                    layer=get_node_layer(node_id.value),
                    status=calculate_status(metrics, is_failed),
                    latency=metrics.latency,
                    throughput=metrics.throughput,
                    cpu=metrics.cpu_utilization,
                    memory=metrics.memory_usage,
                    transactions=metrics.transactions_per_sec,
                    lockContention=metrics.lock_contention,
                    failureType=get_failure_type(node_id.value)
                ))
        
        return SystemMetrics(
            nodes=nodes,
            loadBalanceIndex=state.load_balancing_metrics.load_balance_index,
            totalServices=state.load_balancing_metrics.total_services,
            activeTransactions=random.randint(50, 150),
            failedNodes=len(state.failed_nodes),
            totalMigrations=len(state.migration_history)
        )
    
    # Mock data when simulation not running or not available
    return generate_mock_metrics()

def generate_mock_metrics() -> SystemMetrics:
    """Generate realistic mock metrics for demo."""
    base_metrics = {
        "EDGE1": {"latency": 12, "throughput": 500, "cpu": 45, "memory": 8.0, "tx": 150, "lock": 8},
        "EDGE2": {"latency": 15, "throughput": 470, "cpu": 50, "memory": 4.5, "tx": 100, "lock": 12},
        "CORE1": {"latency": 8, "throughput": 1000, "cpu": 60, "memory": 12.0, "tx": 250, "lock": 5},
        "CORE2": {"latency": 10, "throughput": 950, "cpu": 55, "memory": 10.0, "tx": 200, "lock": 10},
        "CLOUD1": {"latency": 22, "throughput": 1250, "cpu": 72, "memory": 16.0, "tx": 300, "lock": 15},
    }
    
    nodes = []
    for node_id, base in base_metrics.items():
        # Add some variance
        variance = random.uniform(0.9, 1.1)
        cpu = min(95, base["cpu"] * variance)
        
        nodes.append(NodeData(
            id=node_id,
            name=node_id,
            layer=get_node_layer(node_id),
            status=NodeStatus.HEALTHY if cpu < 70 else (NodeStatus.WARNING if cpu < 85 else NodeStatus.CRITICAL),
            latency=base["latency"] * random.uniform(0.95, 1.05),
            throughput=base["throughput"] * random.uniform(0.95, 1.05),
            cpu=cpu,
            memory=base["memory"] * random.uniform(0.95, 1.05),
            transactions=int(base["tx"] * random.uniform(0.9, 1.1)),
            lockContention=base["lock"] * random.uniform(0.9, 1.1),
            failureType=get_failure_type(node_id)
        ))
    
    return SystemMetrics(
        nodes=nodes,
        loadBalanceIndex=random.uniform(0.82, 0.95),
        totalServices=random.randint(80, 120),
        activeTransactions=random.randint(50, 150),
        failedNodes=0,
        totalMigrations=random.randint(5, 20)
    )

@app.get("/")
async def root():
    return {"message": "Distributed Telecom Dashboard API", "status": "running"}

@app.get("/api/metrics", response_model=SystemMetrics)
async def get_metrics():
    """Get current system metrics."""
    return get_simulation_metrics()

@app.get("/api/nodes/{node_id}")
async def get_node(node_id: str):
    """Get specific node details."""
    metrics = get_simulation_metrics()
    for node in metrics.nodes:
        if node.id == node_id:
            return node
    return {"error": "Node not found"}

@app.get("/api/transactions")
async def get_transactions():
    """Get recent transactions."""
    transactions = []
    for i in range(10):
        transactions.append(TransactionData(
            id=f"TX-{random.randint(1000, 9999)}",
            status=random.choice(["committed", "pending", "preparing", "aborted"]),
            nodes=random.sample(["EDGE1", "EDGE2", "CORE1", "CORE2", "CLOUD1"], random.randint(2, 4)),
            startTime=datetime.now().isoformat(),
            duration=random.uniform(0.01, 0.5)
        ))
    return transactions

@app.get("/api/failover-events")
async def get_failover_events():
    """Get recent failover events."""
    events = []
    reasons = ["Node crash detected", "Omission failure", "Byzantine behavior", "Network partition"]
    for i in range(5):
        source = random.choice(["EDGE1", "EDGE2", "CORE1", "CORE2", "CLOUD1"])
        target = random.choice([n for n in ["EDGE1", "EDGE2", "CORE1", "CORE2", "CLOUD1"] if n != source])
        events.append(FailoverEvent(
            id=f"FO-{random.randint(100, 999)}",
            timestamp=datetime.now().isoformat(),
            sourceNode=source,
            targetNode=target,
            reason=random.choice(reasons),
            status=random.choice(["completed", "in_progress", "failed"])
        ))
    return events

@app.post("/api/simulation/start")
async def start_simulation():
    """Start the load balancing simulation."""
    global simulation
    
    if not SIMULATION_AVAILABLE:
        return {"status": "mock_mode", "message": "Simulation modules not available, using mock data"}
    
    config = SimulationConfig(
        simulation_duration=300.0,
        time_step=1.0,
        failure_injection_enabled=True,
        network_delay_simulation_enabled=True,
        adaptive_migration_enabled=True,
        java_integration_enabled=False
    )
    simulation = LoadBalancerSimulation(config)
    return {"status": "started", "duration": config.simulation_duration}

@app.post("/api/simulation/stop")
async def stop_simulation():
    """Stop the simulation."""
    global simulation
    simulation = None
    return {"status": "stopped"}

@app.websocket("/ws/metrics")
async def websocket_metrics(websocket: WebSocket):
    """WebSocket endpoint for real-time metrics updates."""
    await websocket.accept()
    connected_clients.append(websocket)
    
    try:
        while True:
            metrics = get_simulation_metrics()
            await websocket.send_json(metrics.model_dump())
            await asyncio.sleep(1)  # Update every second
    except WebSocketDisconnect:
        connected_clients.remove(websocket)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
