package com.telecom.distributed.core;

import com.telecom.distributed.core.model.*;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Core interface for managing inter-node communication in the distributed telecom system.
 * Handles RPC calls, message broadcasting, and network partition detection.
 */
public interface CommunicationManager {
    
    /**
     * Sends an RPC request to a target node asynchronously.
     * @param target Target node identifier
     * @param request RPC request to send
     * @return Future containing the response message
     */
    CompletableFuture<Message> sendRPC(NodeId target, RPCRequest request);
    
    /**
     * Broadcasts a message to multiple target nodes.
     * @param message Message to broadcast
     * @param targets Set of target node identifiers
     */
    void broadcastMessage(Message message, Set<NodeId> targets);
    
    /**
     * Registers a handler for specific message types.
     * @param type Message type to handle
     * @param handler Handler implementation
     */
    void registerMessageHandler(MessageType type, MessageHandler handler);
    
    /**
     * Detects network partitions in the system.
     * @return Network partition information, null if no partition detected
     */
    NetworkPartition detectPartition();
}