package com.telecom.distributed.core;

import com.telecom.distributed.core.model.TransactionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Utility for simulating cyclic dependencies and deadlock scenarios for testing.
 */
public class DeadlockSimulator {
    private static final Logger logger = LoggerFactory.getLogger(DeadlockSimulator.class);
    
    /**
     * Creates a simple cycle: T1 -> T2 -> T1
     */
    public static void createSimpleCycle(DistributedDeadlockDetector detector,
                                        TransactionId t1, TransactionId t2) {
        String resource1 = "resource_1";
        String resource2 = "resource_2";
        
        // T1 holds resource1, waits for resource2 (held by T2)
        detector.recordResourceAcquisition(t1, resource1);
        detector.recordWaitFor(t1, t2, resource2);
        
        // T2 holds resource2, waits for resource1 (held by T1)
        detector.recordResourceAcquisition(t2, resource2);
        detector.recordWaitFor(t2, t1, resource1);
        
        logger.debug("Created simple cycle: {} -> {} -> {}", t1, t2, t1);
    }
    
    /**
     * Creates a three-way cycle: T1 -> T2 -> T3 -> T1
     */
    public static void createThreeWayCycle(DistributedDeadlockDetector detector,
                                          TransactionId t1, TransactionId t2, TransactionId t3) {
        String resource1 = "resource_1";
        String resource2 = "resource_2";
        String resource3 = "resource_3";
        
        // T1 holds resource1, waits for resource2 (held by T2)
        detector.recordResourceAcquisition(t1, resource1);
        detector.recordWaitFor(t1, t2, resource2);
        
        // T2 holds resource2, waits for resource3 (held by T3)
        detector.recordResourceAcquisition(t2, resource2);
        detector.recordWaitFor(t2, t3, resource3);
        
        // T3 holds resource3, waits for resource1 (held by T1)
        detector.recordResourceAcquisition(t3, resource3);
        detector.recordWaitFor(t3, t1, resource1);
        
        logger.debug("Created three-way cycle: {} -> {} -> {} -> {}", t1, t2, t3, t1);
    }
    
    /**
     * Creates a complex cycle with multiple paths
     */
    public static void createComplexCycle(DistributedDeadlockDetector detector,
                                         List<TransactionId> transactions) {
        if (transactions.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 transactions for a cycle");
        }
        
        // Create a cycle where each transaction waits for the next
        for (int i = 0; i < transactions.size(); i++) {
            TransactionId current = transactions.get(i);
            TransactionId next = transactions.get((i + 1) % transactions.size());
            
            String currentResource = "resource_" + i;
            String nextResource = "resource_" + ((i + 1) % transactions.size());
            
            detector.recordResourceAcquisition(current, currentResource);
            detector.recordWaitFor(current, next, nextResource);
        }
        
        logger.debug("Created complex cycle with {} transactions", transactions.size());
    }
    
    /**
     * Creates a scenario with no deadlock (linear dependency chain)
     */
    public static void createLinearDependency(DistributedDeadlockDetector detector,
                                             List<TransactionId> transactions) {
        if (transactions.isEmpty()) {
            return;
        }
        
        // Create a linear chain: T1 -> T2 -> T3 (no cycle)
        for (int i = 0; i < transactions.size() - 1; i++) {
            TransactionId current = transactions.get(i);
            TransactionId next = transactions.get(i + 1);
            
            String currentResource = "resource_" + i;
            String nextResource = "resource_" + (i + 1);
            
            detector.recordResourceAcquisition(current, currentResource);
            detector.recordWaitFor(current, next, nextResource);
        }
        
        // Last transaction holds its resource but doesn't wait
        TransactionId last = transactions.get(transactions.size() - 1);
        detector.recordResourceAcquisition(last, "resource_" + (transactions.size() - 1));
        
        logger.debug("Created linear dependency chain with {} transactions", transactions.size());
    }
    
    /**
     * Creates multiple independent cycles
     */
    public static void createMultipleCycles(DistributedDeadlockDetector detector,
                                           List<List<TransactionId>> cycleSets) {
        for (int cycleIndex = 0; cycleIndex < cycleSets.size(); cycleIndex++) {
            List<TransactionId> cycle = cycleSets.get(cycleIndex);
            
            for (int i = 0; i < cycle.size(); i++) {
                TransactionId current = cycle.get(i);
                TransactionId next = cycle.get((i + 1) % cycle.size());
                
                String currentResource = "cycle" + cycleIndex + "_resource_" + i;
                String nextResource = "cycle" + cycleIndex + "_resource_" + ((i + 1) % cycle.size());
                
                detector.recordResourceAcquisition(current, currentResource);
                detector.recordWaitFor(current, next, nextResource);
            }
        }
        
        logger.debug("Created {} independent cycles", cycleSets.size());
    }
}
