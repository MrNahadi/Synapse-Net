package com.telecom.distributed.core.model;

/**
 * Interface for handling messages in the distributed telecom system.
 */
@FunctionalInterface
public interface MessageHandler {
    /**
     * Handles an incoming message.
     * @param message The message to handle
     */
    void handle(Message message);
}