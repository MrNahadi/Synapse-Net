package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Represents a service request that needs to be allocated to a node.
 */
public class ServiceRequest {
    private final ServiceId serviceId;
    private final ServiceType serviceType;
    private final double cpuRequirement;      // CPU percentage required
    private final double memoryRequirement;   // Memory in GB required
    private final int transactionLoad;        // Expected transactions per second
    private final int priority;               // Request priority (1-10, higher is more important)

    public ServiceRequest(ServiceId serviceId, ServiceType serviceType, 
                         double cpuRequirement, double memoryRequirement, 
                         int transactionLoad, int priority) {
        this.serviceId = Objects.requireNonNull(serviceId, "Service ID cannot be null");
        this.serviceType = Objects.requireNonNull(serviceType, "Service type cannot be null");
        this.cpuRequirement = validateCpuRequirement(cpuRequirement);
        this.memoryRequirement = validateMemoryRequirement(memoryRequirement);
        this.transactionLoad = validateTransactionLoad(transactionLoad);
        this.priority = validatePriority(priority);
    }

    private double validateCpuRequirement(double cpuRequirement) {
        if (cpuRequirement < 0.0 || cpuRequirement > 100.0) {
            throw new IllegalArgumentException("CPU requirement must be between 0-100%, got: " + cpuRequirement);
        }
        return cpuRequirement;
    }

    private double validateMemoryRequirement(double memoryRequirement) {
        if (memoryRequirement < 0.0) {
            throw new IllegalArgumentException("Memory requirement must be non-negative, got: " + memoryRequirement);
        }
        return memoryRequirement;
    }

    private int validateTransactionLoad(int transactionLoad) {
        if (transactionLoad < 0) {
            throw new IllegalArgumentException("Transaction load must be non-negative, got: " + transactionLoad);
        }
        return transactionLoad;
    }

    private int validatePriority(int priority) {
        if (priority < 1 || priority > 10) {
            throw new IllegalArgumentException("Priority must be between 1-10, got: " + priority);
        }
        return priority;
    }

    // Getters
    public ServiceId getServiceId() { return serviceId; }
    public ServiceType getServiceType() { return serviceType; }
    public double getCpuRequirement() { return cpuRequirement; }
    public double getMemoryRequirement() { return memoryRequirement; }
    public int getTransactionLoad() { return transactionLoad; }
    public int getPriority() { return priority; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceRequest that = (ServiceRequest) o;
        return Double.compare(that.cpuRequirement, cpuRequirement) == 0 &&
               Double.compare(that.memoryRequirement, memoryRequirement) == 0 &&
               transactionLoad == that.transactionLoad &&
               priority == that.priority &&
               Objects.equals(serviceId, that.serviceId) &&
               serviceType == that.serviceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, serviceType, cpuRequirement, memoryRequirement, transactionLoad, priority);
    }

    @Override
    public String toString() {
        return "ServiceRequest{" +
               "serviceId=" + serviceId +
               ", serviceType=" + serviceType +
               ", cpuRequirement=" + cpuRequirement + "%" +
               ", memoryRequirement=" + memoryRequirement + "GB" +
               ", transactionLoad=" + transactionLoad +
               ", priority=" + priority +
               '}';
    }
}