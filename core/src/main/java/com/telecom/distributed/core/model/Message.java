package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Message model for inter-node communication in the distributed telecom system.
 */
public class Message {
    private final MessageId id;
    private final NodeId sender;
    private final NodeId receiver;
    private final MessageType type;
    private final byte[] payload;
    private final long timestamp;
    private final int priority;

    public Message(MessageId id, NodeId sender, NodeId receiver, MessageType type,
                  byte[] payload, long timestamp, int priority) {
        this.id = Objects.requireNonNull(id, "Message ID cannot be null");
        this.sender = Objects.requireNonNull(sender, "Sender cannot be null");
        this.receiver = Objects.requireNonNull(receiver, "Receiver cannot be null");
        this.type = Objects.requireNonNull(type, "Message type cannot be null");
        this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
        this.timestamp = timestamp;
        this.priority = validatePriority(priority);
    }

    private int validatePriority(int priority) {
        if (priority < 0 || priority > 10) {
            throw new IllegalArgumentException("Priority must be between 0-10, got: " + priority);
        }
        return priority;
    }

    // Getters
    public MessageId getId() { return id; }
    public NodeId getSender() { return sender; }
    public NodeId getReceiver() { return receiver; }
    public MessageType getType() { return type; }
    public byte[] getPayload() { return payload.clone(); }
    public long getTimestamp() { return timestamp; }
    public int getPriority() { return priority; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return timestamp == message.timestamp &&
               priority == message.priority &&
               Objects.equals(id, message.id) &&
               Objects.equals(sender, message.sender) &&
               Objects.equals(receiver, message.receiver) &&
               type == message.type &&
               java.util.Arrays.equals(payload, message.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, sender, receiver, type, timestamp, priority);
        result = 31 * result + java.util.Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return "Message{" +
               "id=" + id +
               ", sender=" + sender +
               ", receiver=" + receiver +
               ", type=" + type +
               ", payloadSize=" + payload.length +
               ", timestamp=" + timestamp +
               ", priority=" + priority +
               '}';
    }
}