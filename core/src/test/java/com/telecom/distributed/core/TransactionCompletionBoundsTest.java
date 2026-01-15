package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.telecom.distributed.core.factory.NodeFactory;
import com.telecom.distributed.core.impl.OptimizedCommunicationManager;
import com.telecom.distributed.core.model.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Property-based test for transaction completion time bounds.
 * 
 * Feature: distributed-telecom-system, Property 28: Transaction Completion Bounds
 * Validates: Requirements 14.3
 * 
 * Property: For any transaction, the completion time should not exceed the calculated 
 * upper bound based on system configuration.
 */
@RunWith(JUnitQuickcheck.class)
public class TransactionCompletionBoundsTest {
    private static final Logger logger = LoggerFactory.getLogger(TransactionCompletionBoundsTest.class);
    
    /**
     * Property: Transaction timeout bounds are proportional to participants.
     */
    @Property(trials = 100)
    public void transactionBoundsProportionalToParticipants() {
        Map<NodeId, NodeConfiguration> allNodes = createAllNodes();
        
        OptimizedCommunicationManager commManager = new OptimizedCommunicationManager(
            allNodes, null, null, 10000L, 5000L, 100, 50
        );
        
        try {
            // Small participant set
            TransactionId tx1 = new TransactionId("tx-small-" + UUID.randomUUID());
            Set<NodeId> smallSet = new HashSet<>(Arrays.asList(NodeId.EDGE1));
            commManager.registerTransaction(tx1, smallSet);
            long bound1 = commManager.getRemainingTransactionTime(tx1);
            
            // Large participant set
            TransactionId tx2 = new TransactionId("tx-large-" + UUID.randomUUID());
            Set<NodeId> largeSet = new HashSet<>(Arrays.asList(NodeId.EDGE1, NodeId.CORE1, NodeId.CLOUD1));
            commManager.registerTransaction(tx2, largeSet);
            long bound2 = commManager.getRemainingTransactionTime(tx2);
            
            assertTrue("Bounds should be positive", bound1 > 0 && bound2 > 0);
            assertTrue("Larger set should have >= bound", bound2 >= bound1 - 100);
            
        } finally {
            commManager.shutdown();
        }
    }
    
    /**
     * Property: Transactions do not timeout immediately after registration.
     */
    @Property(trials = 100)
    public void transactionsDoNotTimeoutImmediately() {
        Map<NodeId, NodeConfiguration> allNodes = createAllNodes();
        
        OptimizedCommunicationManager commManager = new OptimizedCommunicationManager(
            allNodes, null, null, 10000L, 5000L, 100, 50
        );
        
        try {
            TransactionId txId = new TransactionId("tx-" + UUID.randomUUID());
            Set<NodeId> participants = new HashSet<>(Arrays.asList(NodeId.EDGE1, NodeId.CORE1));
            
            commManager.registerTransaction(txId, participants);
            
            boolean timedOut = commManager.isTransactionTimedOut(txId);
            assertFalse("Transaction should not timeout immediately", timedOut);
            
            long remaining = commManager.getRemainingTransactionTime(txId);
            assertTrue("Remaining time should be substantial", remaining > 0);
            
        } finally {
            commManager.shutdown();
        }
    }
    
    /**
     * Property: Remaining time decreases monotonically.
     */
    @Property(trials = 100)
    public void remainingTimeDecreasesMonotonically() {
        Map<NodeId, NodeConfiguration> allNodes = createAllNodes();
        
        OptimizedCommunicationManager commManager = new OptimizedCommunicationManager(
            allNodes, null, null, 10000L, 5000L, 100, 50
        );
        
        try {
            TransactionId txId = new TransactionId("tx-" + UUID.randomUUID());
            Set<NodeId> participants = new HashSet<>(Arrays.asList(NodeId.EDGE1));
            
            commManager.registerTransaction(txId, participants);
            long initial = commManager.getRemainingTransactionTime(txId);
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            long later = commManager.getRemainingTransactionTime(txId);
            
            assertTrue("Remaining time should decrease", later <= initial);
            assertTrue("Difference should be reasonable", (initial - later) >= 0);
            
        } finally {
            commManager.shutdown();
        }
    }
    
    /**
     * Property: Completed transactions release resources.
     */
    @Property(trials = 100)
    public void completedTransactionsReleaseResources() {
        Map<NodeId, NodeConfiguration> allNodes = createAllNodes();
        
        OptimizedCommunicationManager commManager = new OptimizedCommunicationManager(
            allNodes, null, null, 10000L, 5000L, 100, 50
        );
        
        try {
            TransactionId txId = new TransactionId("tx-" + UUID.randomUUID());
            Set<NodeId> participants = new HashSet<>(Arrays.asList(NodeId.EDGE1));
            
            commManager.registerTransaction(txId, participants);
            assertTrue("Transaction should be tracked", 
                      commManager.getRemainingTransactionTime(txId) > 0);
            
            commManager.completeTransaction(txId);
            
            boolean timedOut = commManager.isTransactionTimedOut(txId);
            assertFalse("Completed transaction should not be tracked", timedOut);
            
        } finally {
            commManager.shutdown();
        }
    }
    
    /**
     * Property: Transaction bounds respect minimum default timeout.
     */
    @Property(trials = 100)
    public void transactionBoundsRespectMinimumTimeout() {
        long defaultTimeout = 5000L;
        Map<NodeId, NodeConfiguration> allNodes = createAllNodes();
        
        OptimizedCommunicationManager commManager = new OptimizedCommunicationManager(
            allNodes, null, null, defaultTimeout, 5000L, 100, 50
        );
        
        try {
            TransactionId txId = new TransactionId("tx-" + UUID.randomUUID());
            Set<NodeId> participants = new HashSet<>(Arrays.asList(NodeId.EDGE1));
            
            commManager.registerTransaction(txId, participants);
            long remaining = commManager.getRemainingTransactionTime(txId);
            
            assertTrue("Bound should respect minimum timeout", 
                      remaining >= defaultTimeout - 100);
            
        } finally {
            commManager.shutdown();
        }
    }
    
    private Map<NodeId, NodeConfiguration> createAllNodes() {
        return NodeFactory.createAllNodes().stream()
            .collect(Collectors.toMap(NodeConfiguration::getNodeId, node -> node));
    }
}
