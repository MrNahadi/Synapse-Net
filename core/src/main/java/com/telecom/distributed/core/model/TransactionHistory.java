package com.telecom.distributed.core.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tracks historical transaction data for pattern analysis.
 */
public class TransactionHistory {
    private final ConcurrentLinkedQueue<DataPoint> dataPoints;
    
    public TransactionHistory() {
        this.dataPoints = new ConcurrentLinkedQueue<>();
    }
    
    public void addDataPoint(LocalDateTime timestamp, int transactionRate) {
        dataPoints.offer(new DataPoint(timestamp, transactionRate));
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
        final int value;
        
        DataPoint(LocalDateTime timestamp, int value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}