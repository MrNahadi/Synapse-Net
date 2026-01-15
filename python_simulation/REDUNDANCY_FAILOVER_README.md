# Redundancy and Failover Strategies

## Overview

This module implements comprehensive redundancy and automated failover strategies for high-risk nodes in the distributed telecom system. It integrates with the Java `SystemicFailureRiskAssessor` (task 18) to provide risk-based redundancy configuration and handles multi-node failures and network partitioning scenarios.

## Requirements Addressed

- **17.1**: Redundancy strategies for high-risk nodes based on risk assessment
- **17.2**: Replication strategies for fault tolerance
- **17.3**: Automated failover mechanisms
- **17.4**: Simulation of simultaneous multi-node failures
- **17.5**: Network partitioning simulation

## Key Components

### RedundancyFailoverManager

Main class that manages redundancy configuration and failover execution.

**Key Features:**
- Risk-based redundancy strategy selection
- Automated failover with multiple modes (automatic, semi-automatic, manual)
- Multi-node failure handling
- Network partition simulation and recovery
- Replication group management

### Redundancy Strategies

1. **ACTIVE_ACTIVE**: All replicas actively serve requests (highest availability)
2. **ACTIVE_PASSIVE**: Primary active, backups passive (standard failover)
3. **N_WAY_REPLICATION**: N replicas with quorum (Byzantine fault tolerance)
4. **GEOGRAPHIC_REDUNDANCY**: Distributed across network layers

### Failover Modes

1. **AUTOMATIC**: Immediate failover without intervention
2. **SEMI_AUTOMATIC**: Automatic with confirmation
3. **MANUAL**: Requires manual intervention

## Integration with Risk Assessment

The module integrates with Java `SystemicFailureRiskAssessor` through the `RiskAssessment` data structure:

```python
risk_assessment = RiskAssessment(
    node_id=NodeId.CORE1,
    risk_score=0.85,  # From Java risk assessor
    failure_type=FailureType.BYZANTINE,
    criticality_score=0.95,
    dependent_nodes={NodeId.EDGE1, NodeId.EDGE2},
    cascade_risk_score=0.7,
    mitigation_strategies={"Deploy BFT protocols"}
)
```

## Usage Example

```python
from redundancy_failover import RedundancyFailoverManager, RiskAssessment

# Initialize manager
manager = RedundancyFailoverManager()

# Configure redundancy based on risk assessment
risk_assessments = get_risk_assessments_from_java()
redundancy_config = manager.configure_redundancy_from_risk_assessment(risk_assessments)

# Handle node failure with automated failover
failover_event = manager.implement_automated_failover(
    failed_node=NodeId.CORE1,
    state=current_state,
    failover_mode=FailoverMode.AUTOMATIC
)

# Simulate multi-node failure
scenario = manager.simulate_multi_node_failure(
    node_count=3,
    state=current_state,
    include_network_partition=True
)

# Handle recovery
failover_events = manager.handle_multi_node_failure_recovery(scenario, current_state)
```

## Redundancy Strategy Selection Logic

The system automatically selects redundancy strategies based on risk profiles:

- **Critical Risk (≥0.85)**:
  - Byzantine failures → N-way replication (quorum-based)
  - Other failures → Active-active (maximum availability)

- **High Risk (≥0.70)**:
  - Many dependents → Active-active
  - Few dependents → Active-passive

- **Medium Risk (≥0.50)**:
  - Active-passive (standard failover)

- **Low Risk (<0.50)**:
  - Geographic redundancy (cost-effective)

## Multi-Node Failure Handling

The system handles simultaneous failures through:

1. **Prioritized Recovery**: Critical nodes recovered first
2. **Coordinated Failover**: Multiple failovers executed in sequence
3. **Cascade Prevention**: Redundancy prevents cascading failures
4. **State Consistency**: Maintains system consistency during recovery

## Network Partition Simulation

Network partitions are simulated by:

1. Dividing nodes into 2-3 partition groups
2. Identifying majority and minority partitions
3. Treating minority partition nodes as "failed" from majority perspective
4. Simulating partition duration (1-5 minutes)
5. Coordinating recovery when partition heals

## Testing

Comprehensive unit tests cover:

- Redundancy configuration from risk assessment
- Automated failover success and failure scenarios
- Multi-node failure scenarios (Requirement 17.4)
- Network partition scenarios (Requirement 17.5)
- Recovery coordination
- Statistics collection

Run tests:
```bash
cd python_simulation
python3 run_tests.py
```

## Demonstration

Run the demonstration to see the system in action:

```bash
cd python_simulation
python3 redundancy_demo.py
```

The demo shows:
1. Risk-based redundancy configuration
2. Single node failover
3. Multi-node failure recovery
4. Network partition handling
5. System statistics

## Performance Characteristics

- **Failover Time**:
  - Active-active: 0.5-2 seconds
  - Active-passive: 2-10 seconds
  - N-way replication: 5-15 seconds
  - Geographic: 10-30 seconds

- **Success Rates**:
  - Active-active: 98%
  - Active-passive: 95%
  - N-way replication: 92%
  - Geographic: 90%

## Integration Points

1. **Java Risk Assessment**: Receives risk scores from `SystemicFailureRiskAssessor`
2. **Failure Injection**: Works with `FailureInjector` for testing
3. **Load Balancer**: Coordinates with `LoadBalancerSimulation` for service migration
4. **Adaptive Migration**: Uses `AdaptiveMigrationEngine` for service relocation

## Future Enhancements

- Real-time risk score updates
- Machine learning-based failover prediction
- Cross-datacenter replication
- Automated capacity planning
- Advanced partition recovery strategies
