package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for messages in the distributed telecom system.
 */
public class MessageId {
    private final String id;

    public MessageId(String id) {
        this.id = Objects.requireNonNull(id, "Message ID cannot be null");
    }

    public static MessageId generate() {
        return new MessageId(UUID.randomUUID().toString());
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageId messageId = (MessageId) o;
        return Objects.equals(id, messageId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MessageId{" + id + '}';
    }
}