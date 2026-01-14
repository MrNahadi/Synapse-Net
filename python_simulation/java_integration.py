"""
Java integration module for interfacing with the Java load balancing strategy
from task 9. Provides bridge between Python simulation and Java implementation.
"""

import json
import subprocess
import logging
import tempfile
import os
from typing import Dict, List, Optional, Any
from dataclasses import asdict

from models import (
    NodeId, NodeMetrics, ServiceRequest, LoadBalancingMetrics,
    LoadBalancingStrategy, TrafficPattern
)


class JavaLoadBalancerIntegration:
    """
    Integration layer for communicating with Java load balancing implementation.
    Uses JSON-based communication through temporary files or process pipes.
    """
    
    def __init__(self, java_classpath: str = None):
        self.logger = logging.getLogger(__name__)
        
        # Java execution configuration
        self.java_classpath = java_classpath or self._detect_java_classpath()
        self.java_main_class = "com.telecom.distributed.core.LoadBalancerBridge"
        
        # Communication configuration
        self.use_temp_files = True  # Use temp files for communication
        self.temp_dir = tempfile.gettempdir()
        
        # State synchronization
        self.last_sync_time = 0.0
        self.java_state_cache = {}
        
        self.logger.info("JavaLoadBalancerIntegration initialized with classpath: %s", 
                        self.java_classpath)
    
    def _detect_java_classpath(self) -> str:
        """Detect Java classpath from Maven project structure"""
        # Look for compiled classes and dependencies
        possible_paths = [
            "core/target/classes",
            "core/target/dependency/*",
            "target/classes",
            "target/dependency/*"
        ]
        
        classpath_parts = []
        for path in possible_paths:
            if os.path.exists(path.split('*')[0]):  # Check if directory exists
                classpath_parts.append(path)
        
        if classpath_parts:
            return ":".join(classpath_parts)
        else:
            self.logger.warning("Could not detect Java classpath, using default")
            return "core/target/classes:core/target/dependency/*"
    
    def sync_node_metrics(self, node_metrics: Dict[NodeId, NodeMetrics]) -> bool:
        """
        Synchronize node metrics with Java load balancer.
        
        Args:
            node_metrics: Current node metrics from Python simulation
            
        Returns:
            True if synchronization successful
        """
        try:
            # Convert Python models to Java-compatible format
            java_metrics = self._convert_node_metrics_to_java(node_metrics)
            
            # Send to Java load balancer
            request = {
                "action": "updateNodeMetrics",
                "data": java_metrics
            }
            
            response = self._call_java_service(request)
            
            if response and response.get("success"):
                self.logger.debug("Successfully synchronized node metrics with Java")
                return True
            else:
                self.logger.error("Failed to synchronize node metrics: %s", 
                                response.get("error", "Unknown error"))
                return False
                
        except Exception as e:
            self.logger.error("Error synchronizing node metrics: %s", e)
            return False
    
    def get_node_selection(self, service_request: ServiceRequest) -> Optional[NodeId]:
        """
        Get node selection from Java load balancer.
        
        Args:
            service_request: Service request to allocate
            
        Returns:
            Selected node ID or None if allocation failed
        """
        try:
            # Convert service request to Java format
            java_request = self._convert_service_request_to_java(service_request)
            
            # Call Java load balancer
            request = {
                "action": "selectNode",
                "data": java_request
            }
            
            response = self._call_java_service(request)
            
            if response and response.get("success"):
                node_id_str = response.get("nodeId")
                if node_id_str:
                    return NodeId(node_id_str)
            else:
                self.logger.error("Java node selection failed: %s", 
                                response.get("error", "Unknown error"))
            
            return None
            
        except Exception as e:
            self.logger.error("Error getting node selection from Java: %s", e)
            return None
    
    def update_load_balancing_strategy(self, strategy: LoadBalancingStrategy) -> bool:
        """
        Update load balancing strategy in Java implementation.
        
        Args:
            strategy: New load balancing strategy
            
        Returns:
            True if update successful
        """
        try:
            request = {
                "action": "setStrategy",
                "data": {"strategy": strategy.value}
            }
            
            response = self._call_java_service(request)
            
            if response and response.get("success"):
                self.logger.info("Updated Java load balancing strategy to: %s", strategy.value)
                return True
            else:
                self.logger.error("Failed to update Java strategy: %s", 
                                response.get("error", "Unknown error"))
                return False
                
        except Exception as e:
            self.logger.error("Error updating Java strategy: %s", e)
            return False
    
    def get_load_balancing_metrics(self) -> Optional[LoadBalancingMetrics]:
        """
        Get current load balancing metrics from Java implementation.
        
        Returns:
            Load balancing metrics or None if retrieval failed
        """
        try:
            request = {
                "action": "getMetrics",
                "data": {}
            }
            
            response = self._call_java_service(request)
            
            if response and response.get("success"):
                java_metrics = response.get("metrics")
                if java_metrics:
                    return self._convert_java_metrics_to_python(java_metrics)
            else:
                self.logger.error("Failed to get Java metrics: %s", 
                                response.get("error", "Unknown error"))
            
            return None
            
        except Exception as e:
            self.logger.error("Error getting Java metrics: %s", e)
            return None
    
    def handle_traffic_fluctuation(self, traffic_pattern: TrafficPattern) -> bool:
        """
        Notify Java load balancer of traffic fluctuation.
        
        Args:
            traffic_pattern: Current traffic pattern
            
        Returns:
            True if notification successful
        """
        try:
            java_pattern = self._convert_traffic_pattern_to_java(traffic_pattern)
            
            request = {
                "action": "handleTrafficFluctuation",
                "data": java_pattern
            }
            
            response = self._call_java_service(request)
            
            if response and response.get("success"):
                self.logger.debug("Successfully notified Java of traffic fluctuation")
                return True
            else:
                self.logger.error("Failed to notify Java of traffic fluctuation: %s", 
                                response.get("error", "Unknown error"))
                return False
                
        except Exception as e:
            self.logger.error("Error notifying Java of traffic fluctuation: %s", e)
            return False
    
    def _call_java_service(self, request: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """
        Call Java service with request and get response.
        
        Args:
            request: Request data to send to Java
            
        Returns:
            Response data from Java or None if call failed
        """
        if self.use_temp_files:
            return self._call_java_via_temp_files(request)
        else:
            return self._call_java_via_process(request)
    
    def _call_java_via_temp_files(self, request: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Call Java service using temporary files for communication"""
        try:
            # Create temporary files for request and response
            request_file = tempfile.NamedTemporaryFile(mode='w', suffix='.json', 
                                                     dir=self.temp_dir, delete=False)
            response_file = tempfile.NamedTemporaryFile(mode='w', suffix='.json', 
                                                      dir=self.temp_dir, delete=False)
            
            # Write request to file
            json.dump(request, request_file, indent=2)
            request_file.close()
            
            response_file.close()  # Close so Java can write to it
            
            # Build Java command
            java_cmd = [
                "java",
                "-cp", self.java_classpath,
                self.java_main_class,
                request_file.name,
                response_file.name
            ]
            
            # Execute Java process
            result = subprocess.run(java_cmd, capture_output=True, text=True, timeout=30)
            
            if result.returncode != 0:
                self.logger.error("Java process failed with return code %d: %s", 
                                result.returncode, result.stderr)
                return None
            
            # Read response from file
            try:
                with open(response_file.name, 'r') as f:
                    response = json.load(f)
                return response
            except (json.JSONDecodeError, FileNotFoundError) as e:
                self.logger.error("Failed to read Java response: %s", e)
                return None
            
        except subprocess.TimeoutExpired:
            self.logger.error("Java process timed out")
            return None
        except Exception as e:
            self.logger.error("Error calling Java via temp files: %s", e)
            return None
        finally:
            # Clean up temporary files
            try:
                os.unlink(request_file.name)
                os.unlink(response_file.name)
            except:
                pass
    
    def _call_java_via_process(self, request: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Call Java service using process pipes for communication"""
        try:
            # Build Java command
            java_cmd = [
                "java",
                "-cp", self.java_classpath,
                self.java_main_class,
                "--stdin"  # Indicate to read from stdin
            ]
            
            # Execute Java process with input
            request_json = json.dumps(request)
            result = subprocess.run(java_cmd, input=request_json, capture_output=True, 
                                  text=True, timeout=30)
            
            if result.returncode != 0:
                self.logger.error("Java process failed with return code %d: %s", 
                                result.returncode, result.stderr)
                return None
            
            # Parse response
            try:
                response = json.loads(result.stdout)
                return response
            except json.JSONDecodeError as e:
                self.logger.error("Failed to parse Java response: %s", e)
                return None
            
        except subprocess.TimeoutExpired:
            self.logger.error("Java process timed out")
            return None
        except Exception as e:
            self.logger.error("Error calling Java via process: %s", e)
            return None
    
    def _convert_node_metrics_to_java(self, node_metrics: Dict[NodeId, NodeMetrics]) -> Dict[str, Any]:
        """Convert Python node metrics to Java-compatible format"""
        java_metrics = {}
        
        for node_id, metrics in node_metrics.items():
            java_metrics[node_id.value] = {
                "latency": metrics.latency,
                "throughput": metrics.throughput,
                "packetLoss": metrics.packet_loss,
                "cpuUtilization": metrics.cpu_utilization,
                "memoryUsage": metrics.memory_usage,
                "transactionsPerSec": metrics.transactions_per_sec,
                "lockContention": metrics.lock_contention,
                "timestamp": metrics.timestamp
            }
        
        return java_metrics
    
    def _convert_service_request_to_java(self, service_request: ServiceRequest) -> Dict[str, Any]:
        """Convert Python service request to Java-compatible format"""
        return {
            "serviceId": service_request.service_id,
            "cpuRequirement": service_request.cpu_requirement,
            "memoryRequirement": service_request.memory_requirement,
            "transactionLoad": service_request.transaction_load,
            "priority": service_request.priority,
            "timestamp": service_request.timestamp
        }
    
    def _convert_traffic_pattern_to_java(self, traffic_pattern: TrafficPattern) -> Dict[str, Any]:
        """Convert Python traffic pattern to Java-compatible format"""
        return {
            "patternType": traffic_pattern.pattern_type.value,
            "intensity": traffic_pattern.intensity,
            "duration": traffic_pattern.duration,
            "startTime": traffic_pattern.start_time
        }
    
    def _convert_java_metrics_to_python(self, java_metrics: Dict[str, Any]) -> LoadBalancingMetrics:
        """Convert Java metrics to Python LoadBalancingMetrics"""
        # Convert distribution maps
        cpu_distribution = {}
        memory_distribution = {}
        transaction_distribution = {}
        
        cpu_dist = java_metrics.get("cpuDistribution", {})
        for node_str, value in cpu_dist.items():
            cpu_distribution[NodeId(node_str)] = value
        
        memory_dist = java_metrics.get("memoryDistribution", {})
        for node_str, value in memory_dist.items():
            memory_distribution[NodeId(node_str)] = value
        
        transaction_dist = java_metrics.get("transactionDistribution", {})
        for node_str, value in transaction_dist.items():
            transaction_distribution[NodeId(node_str)] = value
        
        return LoadBalancingMetrics(
            cpu_distribution=cpu_distribution,
            memory_distribution=memory_distribution,
            transaction_distribution=transaction_distribution,
            load_balance_index=java_metrics.get("loadBalanceIndex", 0.0),
            total_services=java_metrics.get("totalServices", 0),
            migrations=java_metrics.get("migrations", 0),
            last_update_timestamp=java_metrics.get("lastUpdateTimestamp", 0.0)
        )
    
    def test_java_connectivity(self) -> bool:
        """
        Test connectivity to Java load balancer.
        
        Returns:
            True if Java service is accessible
        """
        try:
            request = {
                "action": "ping",
                "data": {}
            }
            
            response = self._call_java_service(request)
            
            if response and response.get("success"):
                self.logger.info("Java connectivity test successful")
                return True
            else:
                self.logger.error("Java connectivity test failed: %s", 
                                response.get("error", "No response"))
                return False
                
        except Exception as e:
            self.logger.error("Java connectivity test error: %s", e)
            return False
    
    def get_java_status(self) -> Dict[str, Any]:
        """
        Get status information from Java load balancer.
        
        Returns:
            Status information dictionary
        """
        try:
            request = {
                "action": "getStatus",
                "data": {}
            }
            
            response = self._call_java_service(request)
            
            if response and response.get("success"):
                return response.get("status", {})
            else:
                return {"error": response.get("error", "Failed to get status")}
                
        except Exception as e:
            self.logger.error("Error getting Java status: %s", e)
            return {"error": str(e)}
    
    def shutdown_java_service(self) -> bool:
        """
        Shutdown Java load balancer service.
        
        Returns:
            True if shutdown successful
        """
        try:
            request = {
                "action": "shutdown",
                "data": {}
            }
            
            response = self._call_java_service(request)
            
            if response and response.get("success"):
                self.logger.info("Java service shutdown successful")
                return True
            else:
                self.logger.error("Java service shutdown failed: %s", 
                                response.get("error", "Unknown error"))
                return False
                
        except Exception as e:
            self.logger.error("Error shutting down Java service: %s", e)
            return False