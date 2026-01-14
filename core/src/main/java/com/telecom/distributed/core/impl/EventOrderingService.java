package com.telecom.distributed.core.impl;

import com.telecom.distributed.core.model.Event;
import com.telecom.distributed.core.model.LogicalClock;
import com.telecom.distributed.core.model.Message;

import java.util.List;
import java.util.Set;

/**
 * Interface for event ordering service in the distributed telecom system.
 */
public interface EventOrderingService {
    
    /**
     * Submits an event for ordering.
     * @param event The event to submit
     */
    void submitEvent(Event event);
    
    /**
     * Gets ordered events within a time range.
     * @param fromTimestamp Start timestamp
     * @param toTimestamp End timestamp
     * @return List of ordered events
     */
    List<Event> getOrderedEvents(long fromTimestamp, long toTimestamp);
    
    /**
     * Establishes causal ordering between events.
     * @param eventId The event ID
     * @param dependencies Set of dependent event IDs
     */
    void establishCausalOrder(String eventId, Set<String> dependencies);
    
    /**
     * Gets the logical clock for this service.
     * @return The logical clock
     */
    LogicalClock getLogicalClock();
    
    /**
     * Handles incoming event ordering messages.
     * @param message The event message
     */
    void handleEventMessage(Message message);
}