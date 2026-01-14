package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.telecom.distributed.core.impl.TransactionManagerImpl;
import com.telecom.distributed.core.model.*;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for transaction commit protocol correctness.
 * **Feature: distributed-telecom-system, Property 4: Transaction Commit Protocol Correctness**
 * **Validates: Requirements 2.3, 12.4**
 */
@RunWith(JUnitQuickcheck.class)
@Tag("Feature: distributed-telecom-system, Property 4: Transaction Commit Protocol Correctness")
public class TransactionCommitProtocolCorrectnessTest {

    /**
     * Property 4: Transaction Commit Protocol Correctness
     * For any distributed transaction involving multiple nodes, the 2PC/3PC protocol 
     * should ensure atomicity - either all participants commit or all abort.
     */
    @Property(trials = 10)
    public void transactionCommitProtocolCorrectness() throws Exception {
        // Create transaction manager
        TransactionManagerImpl transactionManager = new TransactionManagerImpl();
        
        // Create a set of participant nodes
        Set<NodeId> participants = Set.of(NodeId.EDGE1, NodeId.CORE1, NodeId.CORE2);
        
        // Begin transaction
        TransactionId txId = transactionManager.beginTransaction();
        assertNotNull(txId, "Transaction ID should not be null");
        
        // Prepare phase - all participants should prepare
        transactionManager.prepare(txId, participants);
        
        // Verify transaction is in PREPARED state
        DistributedTransaction transaction = transactionManager.getTransaction(txId);
        assertEquals(TransactionState.PREPARED, transaction.getState(),
            "Transaction should be in PREPARED state after prepare phase");
        
        // Commit phase
        CommitResult result = transactionManager.commit(txId);
        
        // Verify atomicity - either all commit or all abort
        assertTrue(result == CommitResult.COMMITTED || result == CommitResult.ABORTED,
            "Transaction should either be COMMITTED or ABORTED, got: " + result);
        
        if (result == CommitResult.COMMITTED) {
            assertEquals(TransactionState.COMMITTED, transaction.getState(),
                "Transaction state should be COMMITTED when commit succeeds");
        } else {
            assertEquals(TransactionState.ABORTED, transaction.getState(),
                "Transaction state should be ABORTED when commit fails");
        }
        
        // Verify all participants have consistent state
        for (NodeId participant : participants) {
            TransactionState participantState = transactionManager.getParticipantState(txId, participant);
            assertEquals(transaction.getState(), participantState,
                "Participant " + participant + " should have same state as transaction");
        }
    }
}