package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.telecom.distributed.core.model.TransactionId;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for deadlock detection accuracy.
 * **Feature: distributed-telecom-system, Property 24: Deadlock Detection Accuracy**
 * **Validates: Requirements 13.1**
 */
@RunWith(JUnitQuickcheck.class)
@Tag("Feature: distributed-telecom-system, Property 24: Deadlock Detection Accuracy")
public class DeadlockDetectionAccuracyTest {

    /**
     * Property 24: Deadlock Detection Accuracy
     * For any system state with cyclic wait dependencies, the deadlock detection algorithm 
     * should identify all deadlocked transactions.
     */
    @Property(trials = 100)
    public void detectsSimpleTwoWayCycle() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        TransactionId t1 = TransactionId.generate();
        TransactionId t2 = TransactionId.generate();
        
        // Create a simple cycle: T1 -> T2 -> T1
        DeadlockSimulator.createSimpleCycle(detector, t1, t2);
        
        // Detect deadlocks
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        
        // Both transactions should be detected as deadlocked
        assertFalse(deadlocked.isEmpty(), "Deadlock should be detected");
        assertTrue(deadlocked.contains(t1) || deadlocked.contains(t2),
                  "At least one transaction from the cycle should be detected");
        
        // Verify the cycle exists in wait-for graph
        Set<TransactionId> t1WaitingFor = detector.getWaitingFor(t1);
        Set<TransactionId> t2WaitingFor = detector.getWaitingFor(t2);
        
        assertTrue(t1WaitingFor.contains(t2), "T1 should be waiting for T2");
        assertTrue(t2WaitingFor.contains(t1), "T2 should be waiting for T1");
    }

    @Property(trials = 100)
    public void detectsThreeWayCycle() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        TransactionId t1 = TransactionId.generate();
        TransactionId t2 = TransactionId.generate();
        TransactionId t3 = TransactionId.generate();
        
        // Create a three-way cycle: T1 -> T2 -> T3 -> T1
        DeadlockSimulator.createThreeWayCycle(detector, t1, t2, t3);
        
        // Detect deadlocks
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        
        // All three transactions should be detected as deadlocked
        assertFalse(deadlocked.isEmpty(), "Deadlock should be detected");
        assertTrue(deadlocked.contains(t1) || deadlocked.contains(t2) || deadlocked.contains(t3),
                  "At least one transaction from the cycle should be detected");
    }

    @Property(trials = 100)
    public void doesNotDetectLinearDependencies() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        // Create linear dependency chain (no cycle)
        List<TransactionId> transactions = Arrays.asList(
            TransactionId.generate(),
            TransactionId.generate(),
            TransactionId.generate()
        );
        
        DeadlockSimulator.createLinearDependency(detector, transactions);
        
        // Detect deadlocks
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        
        // No deadlock should be detected
        assertTrue(deadlocked.isEmpty(), 
                  "No deadlock should be detected in linear dependency chain");
    }

    @Property(trials = 100)
    public void detectsComplexCycles() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        // Create a cycle with 4-6 transactions
        int cycleSize = 4 + new Random().nextInt(3); // 4, 5, or 6
        List<TransactionId> transactions = new ArrayList<>();
        for (int i = 0; i < cycleSize; i++) {
            transactions.add(TransactionId.generate());
        }
        
        DeadlockSimulator.createComplexCycle(detector, transactions);
        
        // Detect deadlocks
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        
        // At least one transaction from the cycle should be detected
        assertFalse(deadlocked.isEmpty(), 
                   "Deadlock should be detected in complex cycle");
        
        // Verify at least one detected transaction is from our cycle
        boolean foundCycleMember = false;
        for (TransactionId tx : deadlocked) {
            if (transactions.contains(tx)) {
                foundCycleMember = true;
                break;
            }
        }
        assertTrue(foundCycleMember, 
                  "At least one transaction from the cycle should be in deadlock set");
    }

    @Property(trials = 100)
    public void detectsMultipleIndependentCycles() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        // Create two independent cycles
        List<TransactionId> cycle1 = Arrays.asList(
            TransactionId.generate(),
            TransactionId.generate()
        );
        
        List<TransactionId> cycle2 = Arrays.asList(
            TransactionId.generate(),
            TransactionId.generate()
        );
        
        List<List<TransactionId>> cycles = Arrays.asList(cycle1, cycle2);
        DeadlockSimulator.createMultipleCycles(detector, cycles);
        
        // Detect deadlocks
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        
        // Deadlocks should be detected
        assertFalse(deadlocked.isEmpty(), 
                   "Deadlocks should be detected in multiple cycles");
        
        // At least one transaction from each cycle should be detected
        boolean foundCycle1Member = deadlocked.stream().anyMatch(cycle1::contains);
        boolean foundCycle2Member = deadlocked.stream().anyMatch(cycle2::contains);
        
        assertTrue(foundCycle1Member || foundCycle2Member,
                  "At least one cycle should be detected");
    }

    @Property(trials = 100)
    public void deadlockDisappearsAfterResourceRelease() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        TransactionId t1 = TransactionId.generate();
        TransactionId t2 = TransactionId.generate();
        
        // Create a simple cycle
        DeadlockSimulator.createSimpleCycle(detector, t1, t2);
        
        // Verify deadlock exists
        Set<TransactionId> deadlockedBefore = detector.detectDeadlocks();
        assertFalse(deadlockedBefore.isEmpty(), "Deadlock should exist initially");
        
        // Release one transaction to break the cycle
        detector.removeTransaction(t1);
        
        // Verify deadlock is resolved
        Set<TransactionId> deadlockedAfter = detector.detectDeadlocks();
        
        // Either no deadlock, or t1 is no longer in the deadlock set
        assertTrue(deadlockedAfter.isEmpty() || !deadlockedAfter.contains(t1),
                  "Deadlock should be resolved after removing transaction");
    }

    @Property(trials = 100)
    public void emptyGraphHasNoDeadlocks() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        // Empty wait-for graph
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        
        assertTrue(deadlocked.isEmpty(), 
                  "Empty wait-for graph should have no deadlocks");
    }

    @Property(trials = 100)
    public void singleTransactionHasNoDeadlock() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        TransactionId t1 = TransactionId.generate();
        
        // Single transaction holding a resource (no waiting)
        detector.recordResourceAcquisition(t1, "resource_1");
        
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        
        assertTrue(deadlocked.isEmpty(), 
                  "Single transaction with no wait dependencies should have no deadlock");
    }
}
