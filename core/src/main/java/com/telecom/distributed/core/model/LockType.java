package com.telecom.distributed.core.model;

/**
 * Types of distributed locks that can be acquired on resources.
 */
public enum LockType {
    SHARED("Shared lock - allows concurrent reads"),
    EXCLUSIVE("Exclusive lock - prevents all other access"),
    INTENTION_SHARED("Intention shared lock - indicates intention to acquire shared locks on children"),
    INTENTION_EXCLUSIVE("Intention exclusive lock - indicates intention to acquire exclusive locks on children");

    private final String description;

    LockType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompatibleWith(LockType other) {
        switch (this) {
            case SHARED:
                return other == SHARED || other == INTENTION_SHARED;
            case EXCLUSIVE:
                return false; // Exclusive locks are not compatible with any other locks
            case INTENTION_SHARED:
                return other != EXCLUSIVE;
            case INTENTION_EXCLUSIVE:
                return other == INTENTION_SHARED || other == INTENTION_EXCLUSIVE;
            default:
                return false;
        }
    }
}