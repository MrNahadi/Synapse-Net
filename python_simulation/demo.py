#!/usr/bin/env python3
"""
Demo script for Python dynamic load balancing simulation.
Shows the simulation framework in action with failure injection and network delay simulation.
"""

import sys
import os
import time
import logging

# Add the python_simulation directory to the path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from load_balancer_simulation import LoadBalancerSimulation
from models import SimulationConfig, NodeId, FailureType

def setup_logging():
    """Set up logging for the demo"""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.StreamHandler(sys.stdout)
        ]
    )

def run_demo():
    """Run a demonstration of the load balancing simulation"""
    print("="*60)
    print("PYTHON DYNAMIC LOAD BALANCING SIMULATION DEMO")
    print("="*60)
    
    # Configure simulation
    config = SimulationConfig(
        simulation_duration=30.0,  # 30 seconds
        time_step=1.0,             # 1 second steps
        failure_injection_enabled=True,
        network_delay_simulation_enabled=True,
        adaptive_migration_enabled=True,
        java_integration_enabled=False  # Disabled for demo
    )
    
    print(f"Configuration:")
    print(f"  Duration: {config.simulation_duration} seconds")
    print(f"  Time Step: {config.time_step} seconds")
    print(f"  Failure Injection: {config.failure_injection_enabled}")
    print(f"  Network Delay Simulation: {config.network_delay_simulation_enabled}")
    print(f"  Adaptive Migration: {config.adaptive_migration_enabled}")
    print()
    
    # Create and run simulation
    simulation = LoadBalancerSimulation(config)
    
    print("Starting simulation...")
    print("Initial state:")
    summary = simulation.get_performance_summary()
    print(f"  Active nodes: {summary['active_nodes']}")
    print(f"  Failed nodes: {summary['failed_nodes']}")
    print(f"  Load balance index: {summary['load_balance_index']:.3f}")
    print()
    
    # Inject a specific failure for demonstration
    print("Injecting test failure on EDGE1...")
    current_time = time.time()
    failure = simulation.failure_injector.inject_specific_failure(
        node_id=NodeId.EDGE1,
        failure_type=FailureType.CRASH,
        duration=10.0,
        severity=0.8,
        start_time=current_time
    )
    print(f"  Failure injected: {failure.failure_type.value} on {failure.node_id.value}")
    print()
    
    # Run simulation
    start_time = time.time()
    result = simulation.run_simulation()
    end_time = time.time()
    
    print("Simulation completed!")
    print("="*60)
    print("RESULTS SUMMARY")
    print("="*60)
    
    print(f"Execution time: {end_time - start_time:.2f} seconds")
    print(f"Total requests processed: {result.total_requests_processed}")
    print(f"Successful requests: {result.successful_requests}")
    print(f"Failed requests: {result.failed_requests}")
    print(f"Success rate: {(result.successful_requests / result.total_requests_processed * 100):.1f}%" if result.total_requests_processed > 0 else "N/A")
    print(f"Average response time: {result.average_response_time:.3f} seconds")
    print()
    
    print(f"Migration statistics:")
    print(f"  Total migrations: {result.total_migrations}")
    print(f"  Successful migrations: {result.successful_migrations}")
    print(f"  Failed migrations: {result.failed_migrations}")
    print()
    
    print("Final system state:")
    final_summary = simulation.get_performance_summary()
    print(f"  Active nodes: {final_summary['active_nodes']}")
    print(f"  Failed nodes: {final_summary['failed_nodes']}")
    print(f"  Total services: {final_summary['total_services']}")
    print(f"  Load balance index: {final_summary['load_balance_index']:.3f}")
    print()
    
    print("CPU distribution:")
    for node, cpu_load in final_summary['cpu_distribution'].items():
        print(f"  {node.value}: {cpu_load:.1f}%")
    print()
    
    print("Memory distribution:")
    for node, memory_load in final_summary['memory_distribution'].items():
        print(f"  {node.value}: {memory_load:.1f} GB")
    print()
    
    # Show failure statistics
    if simulation.failure_injector:
        failure_stats = simulation.failure_injector.get_failure_statistics()
        if failure_stats:
            print("Failure injection statistics:")
            print(f"  Total failures: {failure_stats['total_failures']}")
            print(f"  Average duration: {failure_stats['average_duration']:.1f} seconds")
            print(f"  Average severity: {failure_stats['average_severity']:.2f}")
            print()
    
    # Show network delay statistics
    if simulation.network_delay_simulator:
        delay_stats = simulation.network_delay_simulator.get_delay_statistics()
        if delay_stats and delay_stats.get('total_measurements', 0) > 0:
            print("Network delay statistics:")
            print(f"  Total measurements: {delay_stats['total_measurements']}")
            print("  Sample delays:")
            for pair, stats in list(delay_stats['node_pair_stats'].items())[:3]:
                print(f"    {pair}: avg={stats['average_delay']*1000:.1f}ms, jitter={stats['jitter']*1000:.1f}ms")
            print()
    
    print("Demo completed successfully!")
    print("="*60)

if __name__ == '__main__':
    setup_logging()
    try:
        run_demo()
    except KeyboardInterrupt:
        print("\nDemo interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\nDemo failed with error: {e}")
        sys.exit(1)