package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Mutable message model for efficient memory pooling in communication manager.
 * Used internally for message reuse to reduce garbage collection overhead.
 */
public class MutableMessage {
    private MessageId id;
    private NodeId sender;
    private NodeId receiver;
    private MessageType type;
    private byte[] payload;
    private long timestamp;
    private int priority;

    public MutableMessage() {
        this.id = new MessageId(UUID.randomUUID().toString());
        this.payload = new byte[0];
        this.priority = 5; // default priority
    }

    // Setters for pooling
    public void setId(MessageId id) {
        this.id = id;
    }

    public void setSender(NodeId sender) {
        this.sender = sender;
    }

    public void setReceiver(NodeId receiver) {
        this.receiver = receiver;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setPriority(int priority) {
        if (priority < 0 || priority > 10) {
            throw new IllegalArgumentException("Priority must be between 0-10, got: " + priority);
        }
        this.priority = priority;
    }

    // Getters
    public MessageId getId() { return id; }
    public NodeId getSender() { return sender; }
    public NodeId getReceiver() { return receiver; }
    public MessageType getType() { return type; }
    public byte[] getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
    public int getPriority() { return priority; }

    /**
     * Resets the message to default state for reuse in pool.
     */
    public void reset() {
        this.id = new MessageId(UUID.randomUUID().toString());
        this.sender = null;
        this.receiver = null;
        this.type = null;
        this.payload = new byte[0];
        this.timestamp = 0;
        this.priority = 5;
    }

    /**
     * Converts to immutable Message.
     */
    public Message toImmutable() {
        return new Message(id, sender, receiver, type, payload, timestamp, priority);
    }

    @Override
    public String toString() {
        return "MutableMessage{" +
               "id=" + id +
               ", sender=" + sender +
               ", receiver=" + receiver +
               ", type=" + type +
               ", payloadSize=" + (payload != null ? payload.length : 0) +
               ", timestamp=" + timestamp +
               ", priority=" + priority +
               '}';
    }
}
