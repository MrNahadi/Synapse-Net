package com.telecom.distributed.core.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Logical clock implementation for event ordering in distributed systems.
 */
public class LogicalClock {
    private final AtomicLong clock;

    public LogicalClock() {
        this.clock = new AtomicLong(0);
    }

    public LogicalClock(long initialValue) {
        this.clock = new AtomicLong(initialValue);
    }

    /**
     * Increments the logical clock and returns the new value.
     * @return The new clock value
     */
    public long tick() {
        return clock.incrementAndGet();
    }

    /**
     * Updates the logical clock based on a received timestamp.
     * @param receivedTime The timestamp from a received message
     * @return The updated clock value
     */
    public long update(long receivedTime) {
        long currentTime = clock.get();
        long newTime = Math.max(currentTime, receivedTime) + 1;
        clock.set(newTime);
        return newTime;
    }

    /**
     * Gets the current logical clock value.
     * @return Current clock value
     */
    public long getTime() {
        return clock.get();
    }

    /**
     * Sets the logical clock to a specific value.
     * @param time The time to set
     */
    public void setTime(long time) {
        clock.set(time);
    }

    @Override
    public String toString() {
        return "LogicalClock{" + clock.get() + "}";
    }
}