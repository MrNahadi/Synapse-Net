package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * RPC response model for remote procedure call responses in the distributed telecom system.
 */
public class RPCResponse {
    private final String requestId;
    private final Object result;
    private final Exception error;
    private final long responseTime;
    private final boolean success;

    public RPCResponse(String requestId, Object result, Exception error, long responseTime) {
        this.requestId = Objects.requireNonNull(requestId, "Request ID cannot be null");
        this.result = result;
        this.error = error;
        this.responseTime = responseTime;
        this.success = error == null;
    }

    public static RPCResponse success(String requestId, Object result, long responseTime) {
        return new RPCResponse(requestId, result, null, responseTime);
    }

    public static RPCResponse error(String requestId, Exception error, long responseTime) {
        return new RPCResponse(requestId, null, error, responseTime);
    }
    
    // Constructor for message-based responses
    public RPCResponse(MessageId messageId, byte[] payload, boolean success, Exception error) {
        this(messageId.toString(), payload, error, System.currentTimeMillis());
    }

    // Getters
    public String getRequestId() { return requestId; }
    public Object getResult() { return result; }
    public Exception getError() { return error; }
    public long getResponseTime() { return responseTime; }
    public boolean isSuccess() { return success; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RPCResponse that = (RPCResponse) o;
        return responseTime == that.responseTime &&
               success == that.success &&
               Objects.equals(requestId, that.requestId) &&
               Objects.equals(result, that.result) &&
               Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, result, error, responseTime, success);
    }

    @Override
    public String toString() {
        return "RPCResponse{" +
               "requestId='" + requestId + '\'' +
               ", success=" + success +
               ", responseTime=" + responseTime +
               (success ? ", result=" + result : ", error=" + error) +
               '}';
    }
}