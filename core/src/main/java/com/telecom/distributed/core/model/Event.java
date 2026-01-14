package com.telecom.distributed.core.model;

import java.util.Objects;
import java.util.Set;

/**
 * Event model for event ordering in the distributed telecom system.
 */
public class Event {
    private final String eventId;
    private final NodeId sourceNode;
    private final String eventType;
    private final Object eventData;
    private final long timestamp;
    private final long logicalClock;
    private final Set<String> dependencies;
    private final int priority;

    public Event(String eventId, NodeId sourceNode, String eventType, Object eventData,
                long timestamp, long logicalClock, Set<String> dependencies, int priority) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.sourceNode = Objects.requireNonNull(sourceNode, "Source node cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "Event type cannot be null");
        this.eventData = eventData;
        this.timestamp = timestamp;
        this.logicalClock = logicalClock;
        this.dependencies = dependencies != null ? Set.copyOf(dependencies) : Set.of();
        this.priority = validatePriority(priority);
    }

    private int validatePriority(int priority) {
        if (priority < 0 || priority > 10) {
            throw new IllegalArgumentException("Priority must be between 0-10, got: " + priority);
        }
        return priority;
    }

    // Getters
    public String getEventId() { return eventId; }
    public NodeId getSourceNode() { return sourceNode; }
    public String getEventType() { return eventType; }
    public Object getEventData() { return eventData; }
    public long getTimestamp() { return timestamp; }
    public long getLogicalClock() { return logicalClock; }
    public Set<String> getDependencies() { return dependencies; }
    public int getPriority() { return priority; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return timestamp == event.timestamp &&
               logicalClock == event.logicalClock &&
               priority == event.priority &&
               Objects.equals(eventId, event.eventId) &&
               Objects.equals(sourceNode, event.sourceNode) &&
               Objects.equals(eventType, event.eventType) &&
               Objects.equals(eventData, event.eventData) &&
               Objects.equals(dependencies, event.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, sourceNode, eventType, eventData, timestamp, logicalClock, dependencies, priority);
    }

    @Override
    public String toString() {
        return "Event{" +
               "eventId='" + eventId + '\'' +
               ", sourceNode=" + sourceNode +
               ", eventType='" + eventType + '\'' +
               ", timestamp=" + timestamp +
               ", logicalClock=" + logicalClock +
               ", priority=" + priority +
               ", dependencies=" + dependencies.size() +
               '}';
    }
}