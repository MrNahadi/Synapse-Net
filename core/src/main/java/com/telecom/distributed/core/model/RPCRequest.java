package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * RPC request model for remote procedure calls in the distributed telecom system.
 */
public class RPCRequest {
    private final String methodName;
    private final Object[] parameters;
    private final long timeoutMs;
    private final int retryCount;

    public RPCRequest(String methodName, Object[] parameters, long timeoutMs, int retryCount) {
        this.methodName = Objects.requireNonNull(methodName, "Method name cannot be null");
        this.parameters = parameters != null ? parameters.clone() : new Object[0];
        this.timeoutMs = validateTimeout(timeoutMs);
        this.retryCount = validateRetryCount(retryCount);
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

    // Getters
    public String getMethodName() { return methodName; }
    public Object[] getParameters() { return parameters.clone(); }
    public long getTimeoutMs() { return timeoutMs; }
    public int getRetryCount() { return retryCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RPCRequest that = (RPCRequest) o;
        return timeoutMs == that.timeoutMs &&
               retryCount == that.retryCount &&
               Objects.equals(methodName, that.methodName) &&
               java.util.Arrays.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(methodName, timeoutMs, retryCount);
        result = 31 * result + java.util.Arrays.hashCode(parameters);
        return result;
    }

    @Override
    public String toString() {
        return "RPCRequest{" +
               "methodName='" + methodName + '\'' +
               ", parameterCount=" + parameters.length +
               ", timeoutMs=" + timeoutMs +
               ", retryCount=" + retryCount +
               '}';
    }
}