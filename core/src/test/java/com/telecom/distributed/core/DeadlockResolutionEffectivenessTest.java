package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.telecom.distributed.core.model.TransactionId;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for deadlock resolution effectiveness.
 * **Feature: distributed-telecom-system, Property 25: Deadlock Resolution Effectiveness**
 * **Validates: Requirements 13.2**
 */
@RunWith(JUnitQuickcheck.class)
@Tag("Feature: distributed-telecom-system, Property 25: Deadlock Resolution Effectiveness")
public class DeadlockResolutionEffectivenessTest {

    /**
     * Property 25: Deadlock Resolution Effectiveness
     * For any detected deadlock, the resolution mechanism should break the deadlock 
     * while minimizing transaction aborts.
     */
    @Property(trials = 100)
    public void resolutionBreaksSimpleDeadlock() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        TransactionId t1 = TransactionId.generate();
        TransactionId t2 = TransactionId.generate();
        long startTime1 = System.currentTimeMillis() - 1000;
        long startTime2 = System.currentTimeMillis();
        
        // Create a simple cycle
        DeadlockSimulator.createSimpleCycle(detector, t1, t2);
        
        // Detect deadlock
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        assertFalse(deadlocked.isEmpty(), "Deadlock should be detected");
        
        // Select victim and resolve
        Map<TransactionId, Long> startTimes = new HashMap<>();
        startTimes.put(t1, startTime1);
        startTimes.put(t2, startTime2);
        
        TransactionId victim = detector.selectVictim(deadlocked, startTimes);
        assertNotNull(victim, "A victim should be selected");
        assertTrue(deadlocked.contains(victim), "Victim should be from deadlocked set");
        
        // Perform recovery
        detector.performRecovery(victim);
        
        // Verify deadlock is broken
        Set<TransactionId> deadlockedAfter = detector.detectDeadlocks();
        assertTrue(deadlockedAfter.isEmpty() || !deadlockedAfter.contains(victim),
                  "Deadlock should be resolved after victim abort");
    }

    @Property(trials = 100)
    public void selectsYoungestTransactionAsVictim() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        TransactionId t1 = TransactionId.generate();
        TransactionId t2 = TransactionId.generate();
        TransactionId t3 = TransactionId.generate();
        
        long oldestTime = System.currentTimeMillis() - 3000;
        long middleTime = System.currentTimeMillis() - 1500;
        long youngestTime = System.currentTimeMillis();
        
        // Create a three-way cycle
        DeadlockSimulator.createThreeWayCycle(detector, t1, t2, t3);
        
        // Detect deadlock
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        assertFalse(deadlocked.isEmpty(), "Deadlock should be detected");
        
        // Map transactions to start times
        Map<TransactionId, Long> startTimes = new HashMap<>();
        startTimes.put(t1, oldestTime);
        startTimes.put(t2, middleTime);
        startTimes.put(t3, youngestTime);
        
        // Select victim - should be youngest (t3)
        TransactionId victim = detector.selectVictim(deadlocked, startTimes);
        assertNotNull(victim, "A victim should be selected");
        
        // Verify youngest transaction is preferred (minimizes work lost)
        // The victim should have the most recent start time among deadlocked transactions
        long victimStartTime = startTimes.get(victim);
        for (TransactionId tx : deadlocked) {
            if (startTimes.containsKey(tx)) {
                assertTrue(victimStartTime >= startTimes.get(tx),
                          "Victim should be youngest (most recent) transaction");
            }
        }
    }

    @Property(trials = 100)
    public void minimizesNumberOfAborts() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        // Create a simple two-way cycle
        TransactionId t1 = TransactionId.generate();
        TransactionId t2 = TransactionId.generate();
        
        DeadlockSimulator.createSimpleCycle(detector, t1, t2);
        
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        assertFalse(deadlocked.isEmpty(), "Deadlock should be detected");
        
        Map<TransactionId, Long> startTimes = new HashMap<>();
        startTimes.put(t1, System.currentTimeMillis() - 1000);
        startTimes.put(t2, System.currentTimeMillis());
        
        // Select and abort one victim
        TransactionId victim = detector.selectVictim(deadlocked, startTimes);
        detector.performRecovery(victim);
        
        // Verify only one transaction was aborted (minimal aborts)
        // After removing the victim, the other transaction should still be tracked
        TransactionId nonVictim = victim.equals(t1) ? t2 : t1;
        
        // The non-victim should still have its resources
        Set<String> nonVictimResources = detector.getHeldResources(nonVictim);
        assertFalse(nonVictimResources.isEmpty(),
                   "Non-victim transaction should still hold its resources");
        
        // Victim should have no resources
        Set<String> victimResources = detector.getHeldResources(victim);
        assertTrue(victimResources.isEmpty(),
                  "Victim should have no resources after abort");
    }

    @Property(trials = 100)
    public void resolutionHandlesMultipleCycles() {
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
        
        DeadlockSimulator.createMultipleCycles(detector, Arrays.asList(cycle1, cycle2));
        
        // Detect all deadlocks
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        assertFalse(deadlocked.isEmpty(), "Deadlocks should be detected");
        
        // Resolve deadlocks one at a time
        Map<TransactionId, Long> startTimes = new HashMap<>();
        long baseTime = System.currentTimeMillis();
        int counter = 0;
        for (TransactionId tx : deadlocked) {
            startTimes.put(tx, baseTime - (counter++ * 1000));
        }
        
        int abortsCount = 0;
        while (!detector.detectDeadlocks().isEmpty() && abortsCount < 10) {
            Set<TransactionId> currentDeadlocked = detector.detectDeadlocks();
            if (currentDeadlocked.isEmpty()) break;
            
            TransactionId victim = detector.selectVictim(currentDeadlocked, startTimes);
            detector.performRecovery(victim);
            abortsCount++;
        }
        
        // Verify all deadlocks are resolved
        Set<TransactionId> finalDeadlocked = detector.detectDeadlocks();
        assertTrue(finalDeadlocked.isEmpty(),
                  "All deadlocks should be resolved after victim aborts");
        
        // Verify minimal aborts (should be at most 2 for 2 independent cycles)
        assertTrue(abortsCount <= 2,
                  "Should abort at most one transaction per cycle");
    }

    @Property(trials = 100)
    public void recoveryCleanupIsComplete() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        TransactionId t1 = TransactionId.generate();
        TransactionId t2 = TransactionId.generate();
        
        DeadlockSimulator.createSimpleCycle(detector, t1, t2);
        
        // Get initial state
        Set<String> t1Resources = detector.getHeldResources(t1);
        assertFalse(t1Resources.isEmpty(), "T1 should hold resources");
        
        // Perform recovery on t1
        detector.performRecovery(t1);
        
        // Verify complete cleanup
        Set<String> t1ResourcesAfter = detector.getHeldResources(t1);
        assertTrue(t1ResourcesAfter.isEmpty(),
                  "Victim should have no resources after recovery");
        
        Set<TransactionId> t1WaitingFor = detector.getWaitingFor(t1);
        assertTrue(t1WaitingFor.isEmpty(),
                  "Victim should have no wait dependencies after recovery");
        
        Set<TransactionId> activeTransactions = detector.getActiveTransactions();
        assertFalse(activeTransactions.contains(t1),
                   "Victim should not be in active transactions after recovery");
    }

    @Property(trials = 100)
    public void resolutionPreservesNonDeadlockedTransactions() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        // Create a cycle
        TransactionId t1 = TransactionId.generate();
        TransactionId t2 = TransactionId.generate();
        DeadlockSimulator.createSimpleCycle(detector, t1, t2);
        
        // Add an independent transaction (not in deadlock)
        TransactionId t3 = TransactionId.generate();
        detector.recordResourceAcquisition(t3, "independent_resource");
        
        // Detect and resolve deadlock
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        
        Map<TransactionId, Long> startTimes = new HashMap<>();
        startTimes.put(t1, System.currentTimeMillis() - 1000);
        startTimes.put(t2, System.currentTimeMillis());
        
        TransactionId victim = detector.selectVictim(deadlocked, startTimes);
        detector.performRecovery(victim);
        
        // Verify independent transaction's resources are unaffected
        Set<String> t3Resources = detector.getHeldResources(t3);
        assertFalse(t3Resources.isEmpty(),
                   "Non-deadlocked transaction should still hold its resources");
        assertTrue(t3Resources.contains("independent_resource"),
                  "Non-deadlocked transaction should still hold its specific resource");
    }

    @Property(trials = 100)
    public void handlesEmptyDeadlockSet() {
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(5000);
        
        Set<TransactionId> emptySet = new HashSet<>();
        Map<TransactionId, Long> startTimes = new HashMap<>();
        
        // Should handle empty set gracefully
        TransactionId victim = detector.selectVictim(emptySet, startTimes);
        assertNull(victim, "No victim should be selected from empty deadlock set");
    }
}
