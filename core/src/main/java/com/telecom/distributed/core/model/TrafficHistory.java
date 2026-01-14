package com.telecom.distributed.core.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tracks historical traffic data for pattern analysis.
 */
public class TrafficHistory {
    private final ConcurrentLinkedQueue<DataPoint> dataPoints;
    
    public TrafficHistory() {
        this.dataPoints = new ConcurrentLinkedQueue<>();
    }
    
    public void addDataPoint(LocalDateTime timestamp, double trafficRate) {
        dataPoints.offer(new DataPoint(timestamp, trafficRate));
    }
    
    public double getAverageRate(int windowMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minus(windowMinutes, ChronoUnit.MINUTES);
        
        return dataPoints.stream()
            .filter(dp -> dp.timestamp.isAfter(cutoff))
            .mapToDouble(dp -> dp.value)
            .average()
            .orElse(0.0);
    }
    
    public List<DataPoint> getRecentDataPoints(int count) {
        List<DataPoint> recent = new ArrayList<>();
        int added = 0;
        
        for (DataPoint dp : dataPoints) {
            if (added >= count) break;
            recent.add(dp);
            added++;
        }
        
        return recent;
    }
    
    public void cleanOldData(LocalDateTime cutoffTime) {
        dataPoints.removeIf(dp -> dp.timestamp.isBefore(cutoffTime));
    }
    
    private static class DataPoint {
        final LocalDateTime timestamp;
        final double value;
        
        DataPoint(LocalDateTime timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}