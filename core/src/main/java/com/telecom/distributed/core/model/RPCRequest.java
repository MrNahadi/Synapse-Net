package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * RPC request model for remote procedure calls in the distributed telecom system.
 */
public class RPCRequest {
    private final NodeId sourceNode;
    private final String methodName;
    private final Object[] parameters;
    private final long timeoutMs;
    private final int retryCount;
    private final int priority;

    public RPCRequest(NodeId sourceNode, String methodName, Object[] parameters, 
                     long timeoutMs, int retryCount, int priority) {
        this.sourceNode = Objects.requireNonNull(sourceNode, "Source node cannot be null");
        this.methodName = Objects.requireNonNull(methodName, "Method name cannot be null");
        this.parameters = parameters != null ? parameters.clone() : new Object[0];
        this.timeoutMs = validateTimeout(timeoutMs);
        this.retryCount = validateRetryCount(retryCount);
        this.priority = validatePriority(priority);
    }
    
    // Convenience constructor with default priority
    public RPCRequest(NodeId sourceNode, String methodName, Object[] parameters, 
                     long timeoutMs, int retryCount) {
        this(sourceNode, methodName, parameters, timeoutMs, retryCount, 5);
    }
    
    // Backward compatibility constructor (uses Edge1 as default source)
    public RPCRequest(String methodName, Object[] parameters, long timeoutMs, int retryCount) {
        this(NodeId.EDGE1, methodName, parameters, timeoutMs, retryCount, 5);
    }
    
    // Constructor for service-based requests
    public RPCRequest(ServiceId serviceId, String operation, byte[] payload, TransactionId txId) {
        this(NodeId.EDGE1, operation, new Object[]{serviceId, payload, txId}, 5000, 3, 5);
    }

    private long validateTimeout(long timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive, got: " + timeout);
        }
        return timeout;
    }

    private int validateRetryCount(int retryCount) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("Retry count cannot be negative, got: " + retryCount);
        }
        return retryCount;
    }
    
    private int validatePriority(int priority) {
        if (priority < 0 || priority > 10) {
            throw new IllegalArgumentException("Priority must be between 0-10, got: " + priority);
        }
        return priority;
    }

    // Getters
    public NodeId getSourceNode() { return sourceNode; }
    public String getMethodName() { return methodName; }
    public Object[] getParameters() { return parameters.clone(); }
    public long getTimeoutMs() { return timeoutMs; }
    public int getRetryCount() { return retryCount; }
    public int getPriority() { return priority; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RPCRequest that = (RPCRequest) o;
        return timeoutMs == that.timeoutMs &&
               retryCount == that.retryCount &&
               priority == that.priority &&
               Objects.equals(sourceNode, that.sourceNode) &&
               Objects.equals(methodName, that.methodName) &&
               java.util.Arrays.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(sourceNode, methodName, timeoutMs, retryCount, priority);
        result = 31 * result + java.util.Arrays.hashCode(parameters);
        return result;
    }

    @Override
    public String toString() {
        return "RPCRequest{" +
               "sourceNode=" + sourceNode +
               ", methodName='" + methodName + '\'' +
               ", parameterCount=" + parameters.length +
               ", timeoutMs=" + timeoutMs +
               ", retryCount=" + retryCount +
               ", priority=" + priority +
               '}';
    }
}