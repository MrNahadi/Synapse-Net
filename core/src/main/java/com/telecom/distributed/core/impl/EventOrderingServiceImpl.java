package com.telecom.distributed.core.impl;

import com.telecom.distributed.core.model.Event;
import com.telecom.distributed.core.model.LogicalClock;
import com.telecom.distributed.core.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * Implementation of EventOrderingService with priority-based and causal ordering.
 */
public class EventOrderingServiceImpl implements EventOrderingService {
    private static final Logger logger = LoggerFactory.getLogger(EventOrderingServiceImpl.class);
    
    private final LogicalClock logicalClock;
    private final ConcurrentSkipListMap<Long, List<Event>> eventsByTimestamp;
    private final ConcurrentHashMap<String, Event> eventsById;
    private final ConcurrentHashMap<String, Set<String>> causalDependencies;
    private final MessageSerializer serializer;

    public EventOrderingServiceImpl() {
        this.logicalClock = new LogicalClock();
        this.eventsByTimestamp = new ConcurrentSkipListMap<>();
        this.eventsById = new ConcurrentHashMap<>();
        this.causalDependencies = new ConcurrentHashMap<>();
        this.serializer = new MessageSerializer();
    }

    @Override
    public void submitEvent(Event event) {
        Objects.requireNonNull(event, "Event cannot be null");
        
        // Update logical clock
        long newClock = logicalClock.update(event.getLogicalClock());
        
        // Create event with updated logical clock
        Event orderedEvent = new Event(
            event.getEventId(),
            event.getSourceNode(),
            event.getEventType(),
            event.getEventData(),
            event.getTimestamp(),
            newClock,
            event.getDependencies(),
            event.getPriority()
        );
        
        // Store event
        eventsById.put(orderedEvent.getEventId(), orderedEvent);
        eventsByTimestamp.computeIfAbsent(orderedEvent.getLogicalClock(), k -> new ArrayList<>())
                         .add(orderedEvent);
        
        // Sort events at the same logical time by priority
        List<Event> eventsAtTime = eventsByTimestamp.get(orderedEvent.getLogicalClock());
        eventsAtTime.sort(Comparator.comparingInt(Event::getPriority).reversed()
                                   .thenComparing(Event::getTimestamp));
        
        logger.debug("Submitted event {} with logical clock {}", 
                    orderedEvent.getEventId(), newClock);
    }

    @Override
    public List<Event> getOrderedEvents(long fromTimestamp, long toTimestamp) {
        List<Event> result = new ArrayList<>();
        
        // Get events within the logical clock range
        NavigableMap<Long, List<Event>> rangeEvents = eventsByTimestamp.subMap(
            fromTimestamp, true, toTimestamp, true);
        
        for (List<Event> eventsAtTime : rangeEvents.values()) {
            // Events are already sorted by priority within each timestamp
            result.addAll(eventsAtTime);
        }
        
        // Apply causal ordering constraints
        result = applyCausalOrdering(result);
        
        logger.debug("Retrieved {} ordered events from {} to {}", 
                    result.size(), fromTimestamp, toTimestamp);
        
        return result;
    }

    @Override
    public void establishCausalOrder(String eventId, Set<String> dependencies) {
        Objects.requireNonNull(eventId, "Event ID cannot be null");
        Objects.requireNonNull(dependencies, "Dependencies cannot be null");
        
        if (!dependencies.isEmpty()) {
            causalDependencies.put(eventId, new HashSet<>(dependencies));
            logger.debug("Established causal dependencies for event {}: {}", 
                        eventId, dependencies);
        }
    }

    @Override
    public LogicalClock getLogicalClock() {
        return logicalClock;
    }

    @Override
    public void handleEventMessage(Message message) {
        try {
            // Deserialize event from message payload
            Object eventData = serializer.deserializeEventData(message.getPayload());
            
            // Create event from message
            Event event = new Event(
                message.getId().toString(),
                message.getSender(),
                "MESSAGE_EVENT",
                eventData,
                message.getTimestamp(),
                logicalClock.getTime(),
                Set.of(),
                message.getPriority()
            );
            
            submitEvent(event);
            
        } catch (Exception e) {
            logger.error("Failed to handle event message: {}", message.getId(), e);
        }
    }

    /**
     * Applies causal ordering constraints to a list of events.
     */
    private List<Event> applyCausalOrdering(List<Event> events) {
        if (events.isEmpty()) {
            return events;
        }
        
        // Build dependency graph
        Map<String, Set<String>> dependencies = new HashMap<>();
        Map<String, Event> eventMap = new HashMap<>();
        
        for (Event event : events) {
            eventMap.put(event.getEventId(), event);
            Set<String> deps = causalDependencies.getOrDefault(event.getEventId(), Set.of());
            dependencies.put(event.getEventId(), new HashSet<>(deps));
        }
        
        // Topological sort to respect causal ordering
        List<Event> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (Event event : events) {
            if (!visited.contains(event.getEventId())) {
                topologicalSort(event.getEventId(), dependencies, eventMap, 
                              visited, visiting, result);
            }
        }
        
        return result;
    }

    /**
     * Performs topological sort for causal ordering.
     */
    private void topologicalSort(String eventId, Map<String, Set<String>> dependencies,
                                Map<String, Event> eventMap, Set<String> visited,
                                Set<String> visiting, List<Event> result) {
        
        if (visiting.contains(eventId)) {
            logger.warn("Circular dependency detected involving event: {}", eventId);
            return;
        }
        
        if (visited.contains(eventId)) {
            return;
        }
        
        visiting.add(eventId);
        
        // Visit dependencies first
        Set<String> deps = dependencies.getOrDefault(eventId, Set.of());
        for (String depId : deps) {
            if (eventMap.containsKey(depId)) {
                topologicalSort(depId, dependencies, eventMap, visited, visiting, result);
            }
        }
        
        visiting.remove(eventId);
        visited.add(eventId);
        
        Event event = eventMap.get(eventId);
        if (event != null) {
            result.add(event);
        }
    }

    /**
     * Gets statistics about the event ordering service.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", eventsById.size());
        stats.put("currentLogicalClock", logicalClock.getTime());
        stats.put("causalDependencies", causalDependencies.size());
        stats.put("timeRanges", eventsByTimestamp.size());
        return stats;
    }

    /**
     * Clears old events to prevent memory leaks.
     */
    public void cleanupOldEvents(long beforeTimestamp) {
        NavigableMap<Long, List<Event>> oldEvents = eventsByTimestamp.headMap(beforeTimestamp, false);
        
        int removedCount = 0;
        for (List<Event> eventsAtTime : oldEvents.values()) {
            for (Event event : eventsAtTime) {
                eventsById.remove(event.getEventId());
                causalDependencies.remove(event.getEventId());
                removedCount++;
            }
        }
        
        oldEvents.clear();
        
        logger.info("Cleaned up {} old events before timestamp {}", removedCount, beforeTimestamp);
    }
}