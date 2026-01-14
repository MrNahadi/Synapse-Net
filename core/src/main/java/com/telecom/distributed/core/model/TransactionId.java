package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for distributed transactions in the telecom system.
 */
public class TransactionId {
    private final String id;

    public TransactionId(String id) {
        this.id = Objects.requireNonNull(id, "Transaction ID cannot be null");
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID().toString());
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TransactionId{" + id + '}';
    }
}