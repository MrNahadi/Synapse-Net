package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.telecom.distributed.core.impl.FaultToleranceManagerImpl;
import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for crash failure handling.
 * **Feature: distributed-telecom-system, Property 10: Crash Failure Handling**
 * **Validates: Requirements 4.1**
 */
@RunWith(JUnitQuickcheck.class)
@Tag("Feature: distributed-telecom-system, Property 10: Crash Failure Handling")
public class CrashFailureHandlingTest {

    /**
     * Property 10: Crash Failure Handling
     * For any crash failure on Edge1 or Core2, the system should detect the failure 
     * and maintain service availability through other nodes.
     */
    @Property(trials = 10)
    public void crashFailureHandling() throws Exception {
        // Test crash failure handling for Edge1 and Core2
        Set<NodeId> crashProneNodes = Set.of(NodeId.EDGE1, NodeId.CORE2);
        
        for (NodeId failingNode : crashProneNodes) {
            // Create fault tolerance manager
            FaultToleranceManagerImpl faultManager = new FaultToleranceManagerImpl();
            
            // Simulate crash failure
            faultManager.detectFailure(failingNode, FailureType.CRASH);
            
            // Verify failure is detected
            assertTrue(faultManager.isNodeFailed(failingNode), 
                "Crash failure should be detected for " + failingNode);
            
            // Verify other nodes remain available
            Set<NodeId> allNodes = Set.of(NodeId.EDGE1, NodeId.EDGE2, NodeId.CORE1, NodeId.CORE2, NodeId.CLOUD1);
            Set<NodeId> availableNodes = faultManager.getAvailableNodes();
            
            // Should have 4 available nodes (5 total - 1 failed)
            assertEquals(4, availableNodes.size(), 
                "Should have 4 available nodes after crash failure of " + failingNode);
            
            // Failed node should not be in available nodes
            assertFalse(availableNodes.contains(failingNode), 
                "Failed node " + failingNode + " should not be available");
            
            // Verify service availability is maintained
            assertTrue(faultManager.canProvideService(ServiceType.RPC_HANDLING), 
                "RPC service should remain available after crash of " + failingNode);
            
            assertTrue(faultManager.canProvideService(ServiceType.TRANSACTION_PROCESSING), 
                "Transaction service should remain available after crash of " + failingNode);
        }
    }
}