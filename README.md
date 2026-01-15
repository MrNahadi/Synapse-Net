# Distributed Telecom System

[![Java](https://img.shields.io/badge/Java-11-orange.svg)](https://www.oracle.com/java/)
[![Python](https://img.shields.io/badge/Python-3.8+-blue.svg)](https://www.python.org/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A carrier-grade edge-core-cloud distributed telecom system implementing advanced fault tolerance, load balancing, transaction management, and performance optimization across heterogeneous nodes.

## Overview

This project implements a comprehensive distributed telecom system that interconnects five heterogeneous nodes across three architectural layers (Edge, Core, Cloud). The system is designed to handle diverse failure modes (crash, omission, Byzantine), optimize performance across multiple dimensions, and provide strong consistency guarantees under concurrent operations.

### Key Features

- **Multi-Layer Architecture**: Edge-Core-Cloud topology with optimized service placement
- **Advanced Fault Tolerance**: Handles crash, omission, and Byzantine failures with automated recovery
- **Dynamic Load Balancing**: Resource-aware allocation with adaptive migration
- **Distributed Transactions**: 2PC/3PC protocols with deadlock detection and resolution
- **Performance Optimization**: Real-time bottleneck analysis and throughput maximization
- **Redundancy & Failover**: Risk-based replication strategies with automated failover
- **Property-Based Testing**: Comprehensive validation using QuickCheck for Java

## Architecture

### System Topology

```mermaid
graph TB
    subgraph Cloud["Cloud Layer"]
        Cloud1["Cloud1<br/>Analytics, DSM<br/>22ms, 1250Mbps, 16GB<br/>Omission Failures"]
    end
    
    subgraph Core["Core Layer"]
        Core1["Core1<br/>Transaction Commit<br/>8ms, 1000Mbps, 12GB<br/>Byzantine Failures"]
        Core2["Core2<br/>Load Balancing<br/>10ms, 950Mbps, 10GB<br/>Crash Failures"]
    end
    
    subgraph Edge["Edge Layer"]
        Edge1["Edge1<br/>RPC, Replication<br/>12ms, 500Mbps, 8GB<br/>Crash Failures"]
        Edge2["Edge2<br/>Migration, Recovery<br/>15ms, 470Mbps, 4.5GB<br/>Omission Failures"]
    end
    
    Edge1 <--> Core1
    Edge1 <--> Core2
    Edge2 <--> Core1
    Edge2 <--> Core2
    Core1 <--> Cloud1
    Core2 <--> Cloud1
    Edge1 -.-> Edge2
    Core1 -.-> Core2
```

### Node Characteristics

| Node   | Layer | Latency | Throughput | CPU    | Memory | Tx/sec | Failure Type |
|--------|-------|---------|------------|--------|--------|--------|--------------|
| Edge1  | Edge  | 12ms    | 500 Mbps   | 45%    | 8.0GB  | 150    | Crash        |
| Edge2  | Edge  | 15ms    | 470 Mbps   | 50%    | 4.5GB  | 100    | Omission     |
| Core1  | Core  | 8ms     | 1000 Mbps  | 60%    | 12.0GB | 250    | Byzantine    |
| Core2  | Core  | 10ms    | 950 Mbps   | 55%    | 10.0GB | 200    | Crash        |
| Cloud1 | Cloud | 22ms    | 1250 Mbps  | 72%    | 16.0GB | 300    | Omission     |

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Python 3.8 or higher
- Git

### Installation

1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/distributed-telecom-system.git
   cd distributed-telecom-system
   ```

2. Build the Java components
   ```bash
   mvn clean install
   ```

3. Install Python dependencies
   ```bash
   cd python_simulation
   pip install -r requirements.txt
   ```

### Running the System

#### Java System

```bash
# Run the main distributed system
cd core
mvn exec:java -Dexec.mainClass="com.telecom.distributed.core.DistributedTelecomSystem"

# Run tests
mvn test

# Run property-based tests
mvn test -Dtest="*PropertyTest"
```

#### Python Simulation

```bash
cd python_simulation

# Run load balancing simulation
python3 demo.py

# Run redundancy and failover demo
python3 redundancy_demo.py

# Run all tests
python3 run_tests.py
```

## Core Components

### Java Components

- **DistributedTelecomSystem**: Main orchestrator integrating all system components
- **NodeManager**: Manages individual node lifecycle and metrics
- **CommunicationManager**: Handles inter-node RPC and messaging
- **TransactionManager**: Implements 2PC/3PC distributed transaction protocols
- **FaultToleranceManager**: Detects and recovers from various failure types
- **LoadBalancer**: Dynamic resource-aware load distribution
- **ReplicationManager**: Data replication and migration strategies
- **PerformanceAnalyzer**: Real-time bottleneck identification and ranking
- **SystemOptimizer**: Multi-objective performance optimization

### Python Components

- **LoadBalancerSimulation**: Dynamic load balancing with failure injection
- **RedundancyFailoverManager**: Risk-based redundancy and automated failover
- **FailureInjector**: Simulates crash, omission, and Byzantine failures
- **NetworkDelaySimulator**: Realistic network latency and jitter simulation
- **AdaptiveMigrationEngine**: Intelligent service migration decisions

## Testing

The system employs a dual testing approach:

### Unit Testing
- Specific scenarios with known inputs/outputs
- Edge case validation
- Component integration testing

### Property-Based Testing
- 30 universal properties validated across all inputs
- Automated test case generation
- Regression prevention

Run all tests:
```bash
# Java tests
mvn test

# Python tests
cd python_simulation
python3 run_tests.py
```

## Performance Metrics

The system tracks and optimizes:

- **Latency**: 8-22ms range across nodes
- **Throughput**: 470-1250 Mbps capacity
- **CPU Utilization**: 45-72% operational range
- **Memory Usage**: 4.0-16.0 GB per node
- **Transaction Rate**: 100-300 tx/sec
- **Lock Contention**: 5-15% typical range

## Configuration

System configuration is managed through:

- **Java**: `SystemConfiguration` class with node-specific settings
- **Python**: `SimulationConfig` dataclass for simulation parameters

Example configuration:
```java
SystemConfiguration config = new SystemConfiguration.Builder()
    .withMaxConcurrentTransactions(1000)
    .withTransactionTimeout(5000)
    .withHealthCheckInterval(1000)
    .build();
```

## Documentation

- **[DOCUMENTATION.md](DOCUMENTATION.md)**: Comprehensive technical documentation with detailed architecture, algorithms, and implementation details
- **[Requirements](/.kiro/specs/distributed-telecom-system/requirements.md)**: Complete requirements specification
- **[Design](/.kiro/specs/distributed-telecom-system/design.md)**: System design and architecture
- **[Tasks](/.kiro/specs/distributed-telecom-system/tasks.md)**: Implementation task breakdown

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by carrier-grade telecom systems
- Built with modern distributed systems principles
- Implements formal verification and property-based testing methodologies

## Contact

For questions or support, please open an issue on GitHub.

---

Built for distributed systems excellence
