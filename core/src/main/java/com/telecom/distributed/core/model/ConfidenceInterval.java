package com.telecom.distributed.core.model;

/**
 * Represents a confidence interval for statistical analysis.
 */
public class ConfidenceInterval {
    private final double lowerBound;
    private final double upperBound;
    
    public ConfidenceInterval(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }
    
    public double getLowerBound() { return lowerBound; }
    public double getUpperBound() { return upperBound; }
    public double getWidth() { return upperBound - lowerBound; }
    
    public boolean contains(double value) {
        return value >= lowerBound && value <= upperBound;
    }
    
    @Override
    public String toString() {
        return "[" + lowerBound + ", " + upperBound + "]";
    }
}