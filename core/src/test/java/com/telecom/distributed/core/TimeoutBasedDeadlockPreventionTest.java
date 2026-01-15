package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.telecom.distributed.core.model.TransactionId;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for timeout-based deadlock prevention.
 * **Feature: distributed-telecom-system, Property 26: Timeout-Based Deadlock Prevention**
 * **Validates: Requirements 13.4**
 */
@RunWith(JUnitQuickcheck.class)
@Tag("Feature: distributed-telecom-system, Property 26: Timeout-Based Deadlock Prevention")
public class TimeoutBasedDeadlockPreventionTest {

    /**
     * Property 26: Timeout-Based Deadlock Prevention
     * For any transaction that exceeds its timeout threshold, the system should abort it 
     * to prevent potential deadlocks.
     */
    @Property(trials = 100)
    public void detectsTimeoutForOldTransaction() {
        long timeoutMs = 1000; // 1 second timeout
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(timeoutMs);
        
        TransactionId transaction = TransactionId.generate();
        long startTime = System.currentTimeMillis() - 2000; // Started 2 seconds ago
        
        // Check if transaction has timed out
        boolean timedOut = detector.isTimedOut(transaction, startTime);
        
        assertTrue(timedOut, 
                  "Transaction that started 2 seconds ago should timeout with 1 second threshold");
    }

    @Property(trials = 100)
    public void doesNotTimeoutRecentTransaction() {
        long timeoutMs = 5000; // 5 second timeout
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(timeoutMs);
        
        TransactionId transaction = TransactionId.generate();
        long startTime = System.currentTimeMillis() - 1000; // Started 1 second ago
        
        // Check if transaction has timed out
        boolean timedOut = detector.isTimedOut(transaction, startTime);
        
        assertFalse(timedOut,
                   "Transaction that started 1 second ago should not timeout with 5 second threshold");
    }

    @Property(trials = 100)
    public void timeoutThresholdIsRespected() {
        long timeoutMs = 3000; // 3 second timeout
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(timeoutMs);
        
        TransactionId transaction = TransactionId.generate();
        
        // Transaction just at the threshold
        long startTimeAtThreshold = System.currentTimeMillis() - timeoutMs;
        boolean timedOutAtThreshold = detector.isTimedOut(transaction, startTimeAtThreshold);
        
        // Transaction just before threshold
        long startTimeBeforeThreshold = System.currentTimeMillis() - (timeoutMs - 100);
        boolean timedOutBefore = detector.isTimedOut(transaction, startTimeBeforeThreshold);
        
        // Transaction after threshold
        long startTimeAfterThreshold = System.currentTimeMillis() - (timeoutMs + 100);
        boolean timedOutAfter = detector.isTimedOut(transaction, startTimeAfterThreshold);
        
        // At or after threshold should timeout
        assertTrue(timedOutAtThreshold || timedOutAfter,
                  "Transaction at or past threshold should timeout");
        
        // Before threshold should not timeout
        assertFalse(timedOutBefore,
                   "Transaction before threshold should not timeout");
    }

    @Property(trials = 100)
    public void timeoutPreventsDeadlockFormation() throws InterruptedException {
        long timeoutMs = 500; // Short timeout for testing
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(timeoutMs);
        
        TransactionId t1 = TransactionId.generate();
        TransactionId t2 = TransactionId.generate();
        long startTime1 = System.currentTimeMillis();
        long startTime2 = System.currentTimeMillis();
        
        // Start creating a potential deadlock
        detector.recordResourceAcquisition(t1, "resource_1");
        detector.recordResourceAcquisition(t2, "resource_2");
        
        // Wait for timeout
        Thread.sleep(timeoutMs + 100);
        
        // Check if transactions have timed out
        boolean t1TimedOut = detector.isTimedOut(t1, startTime1);
        boolean t2TimedOut = detector.isTimedOut(t2, startTime2);
        
        // At least one should have timed out
        assertTrue(t1TimedOut && t2TimedOut,
                  "Both transactions should timeout after threshold period");
        
        // If we abort timed-out transactions, deadlock is prevented
        if (t1TimedOut) {
            detector.removeTransaction(t1);
        }
        if (t2TimedOut) {
            detector.removeTransaction(t2);
        }
        
        // No deadlock should exist after timeout-based cleanup
        Set<TransactionId> deadlocked = detector.detectDeadlocks();
        assertTrue(deadlocked.isEmpty(),
                  "No deadlock should exist after timeout-based transaction removal");
    }

    @Property(trials = 100)
    public void differentTimeoutThresholdsWork() {
        // Test with various timeout values
        long[] timeouts = {100, 500, 1000, 5000, 10000};
        
        for (long timeout : timeouts) {
            DistributedDeadlockDetector detector = new DistributedDeadlockDetector(timeout);
            TransactionId transaction = TransactionId.generate();
            
            // Transaction that exceeds this timeout
            long oldStartTime = System.currentTimeMillis() - (timeout + 100);
            boolean shouldTimeout = detector.isTimedOut(transaction, oldStartTime);
            
            assertTrue(shouldTimeout,
                      "Transaction should timeout with threshold " + timeout + "ms");
            
            // Transaction within timeout
            long recentStartTime = System.currentTimeMillis() - (timeout / 2);
            boolean shouldNotTimeout = detector.isTimedOut(transaction, recentStartTime);
            
            assertFalse(shouldNotTimeout,
                       "Transaction should not timeout within threshold " + timeout + "ms");
        }
    }

    @Property(trials = 100)
    public void timeoutCheckIsConsistent() {
        long timeoutMs = 2000;
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(timeoutMs);
        
        TransactionId transaction = TransactionId.generate();
        long startTime = System.currentTimeMillis() - 3000; // Definitely timed out
        
        // Multiple checks should give consistent results
        boolean check1 = detector.isTimedOut(transaction, startTime);
        boolean check2 = detector.isTimedOut(transaction, startTime);
        boolean check3 = detector.isTimedOut(transaction, startTime);
        
        assertEquals(check1, check2, "Timeout checks should be consistent");
        assertEquals(check2, check3, "Timeout checks should be consistent");
        assertTrue(check1, "Transaction should be timed out");
    }

    @Property(trials = 100)
    public void zeroTimeoutAlwaysTimesOut() {
        long timeoutMs = 0; // Zero timeout
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(timeoutMs);
        
        TransactionId transaction = TransactionId.generate();
        long startTime = System.currentTimeMillis() - 1; // Any past time
        
        boolean timedOut = detector.isTimedOut(transaction, startTime);
        
        assertTrue(timedOut,
                  "With zero timeout, any transaction with past start time should timeout");
    }

    @Property(trials = 100)
    public void veryLongTimeoutRarelyTimesOut() {
        long timeoutMs = 1000000; // Very long timeout (1000 seconds)
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(timeoutMs);
        
        TransactionId transaction = TransactionId.generate();
        long startTime = System.currentTimeMillis() - 5000; // Started 5 seconds ago
        
        boolean timedOut = detector.isTimedOut(transaction, startTime);
        
        assertFalse(timedOut,
                   "With very long timeout, recent transaction should not timeout");
    }

    @Property(trials = 100)
    public void timeoutBasedAbortBreaksCycle() throws InterruptedException {
        long timeoutMs = 300;
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(timeoutMs);
        
        TransactionId t1 = TransactionId.generate();
        TransactionId t2 = TransactionId.generate();
        long startTime1 = System.currentTimeMillis();
        long startTime2 = System.currentTimeMillis();
        
        // Create a deadlock cycle
        DeadlockSimulator.createSimpleCycle(detector, t1, t2);
        
        // Verify deadlock exists
        Set<TransactionId> deadlockedBefore = detector.detectDeadlocks();
        assertFalse(deadlockedBefore.isEmpty(), "Deadlock should exist");
        
        // Wait for timeout
        Thread.sleep(timeoutMs + 100);
        
        // Abort timed-out transactions
        if (detector.isTimedOut(t1, startTime1)) {
            detector.performRecovery(t1);
        }
        if (detector.isTimedOut(t2, startTime2)) {
            detector.performRecovery(t2);
        }
        
        // Verify deadlock is broken
        Set<TransactionId> deadlockedAfter = detector.detectDeadlocks();
        assertTrue(deadlockedAfter.isEmpty(),
                  "Deadlock should be broken after timeout-based abort");
    }

    @Property(trials = 100)
    public void timeoutPreventionIsProactive() {
        long timeoutMs = 1000;
        DistributedDeadlockDetector detector = new DistributedDeadlockDetector(timeoutMs);
        
        TransactionId transaction = TransactionId.generate();
        
        // Check timeout at different elapsed times
        long[] elapsedTimes = {0, 500, 999, 1000, 1001, 2000};
        
        for (long elapsed : elapsedTimes) {
            long startTime = System.currentTimeMillis() - elapsed;
            boolean timedOut = detector.isTimedOut(transaction, startTime);
            
            if (elapsed > timeoutMs) {
                assertTrue(timedOut,
                          "Transaction should timeout after " + elapsed + "ms (threshold: " + timeoutMs + "ms)");
            } else {
                assertFalse(timedOut,
                           "Transaction should not timeout at " + elapsed + "ms (threshold: " + timeoutMs + "ms)");
            }
        }
    }
}
