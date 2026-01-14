package com.telecom.distributed.core.model;

/**
 * Response from a participant node during the prepare phase of 2PC/3PC protocol.
 */
public class PrepareResponse {
    private final NodeId nodeId;
    private final boolean success;
    private final String reason;
    private final long timestamp;

    public PrepareResponse(NodeId nodeId, boolean success, String reason) {
        this.nodeId = nodeId;
        this.success = success;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public static PrepareResponse success(NodeId nodeId) {
        return new PrepareResponse(nodeId, true, "Prepared successfully");
    }

    public static PrepareResponse failure(NodeId nodeId, String reason) {
        return new PrepareResponse(nodeId, false, reason);
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PrepareResponse{" +
                "nodeId=" + nodeId +
                ", success=" + success +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}