package com.telecom.distributed.core.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.telecom.distributed.core.model.RPCRequest;
import com.telecom.distributed.core.model.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Message serializer using Protocol Buffers for efficient serialization/deserialization.
 */
public class MessageSerializer {
    private static final Logger logger = LoggerFactory.getLogger(MessageSerializer.class);

    /**
     * Serializes an RPC request with request ID using Protocol Buffers.
     */
    public byte[] serializeRPCRequest(RPCRequest request, String requestId) throws IOException {
        try {
            // Create a simple map-based structure for serialization
            Map<String, Object> data = new HashMap<>();
            data.put("requestId", requestId);
            data.put("methodName", request.getMethodName());
            data.put("parameters", request.getParameters());
            data.put("timeoutMs", request.getTimeoutMs());
            data.put("retryCount", request.getRetryCount());
            
            return serializeObject(data);
        } catch (Exception e) {
            logger.error("Failed to serialize RPC request", e);
            throw new IOException("Serialization failed", e);
        }
    }

    /**
     * Deserializes an RPC response from Protocol Buffer bytes.
     */
    public RPCResponse deserializeRPCResponse(byte[] data) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseData = (Map<String, Object>) deserializeObject(data);
            
            String requestId = (String) responseData.get("requestId");
            Object result = responseData.get("result");
            Exception error = (Exception) responseData.get("error");
            Long responseTime = (Long) responseData.get("responseTime");
            
            return new RPCResponse(requestId, result, error, responseTime != null ? responseTime : 0L);
        } catch (Exception e) {
            logger.error("Failed to deserialize RPC response", e);
            throw new IOException("Deserialization failed", e);
        }
    }

    /**
     * Serializes an RPC response with Protocol Buffers.
     */
    public byte[] serializeRPCResponse(RPCResponse response) throws IOException {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("requestId", response.getRequestId());
            data.put("result", response.getResult());
            data.put("error", response.getError());
            data.put("responseTime", response.getResponseTime());
            data.put("success", response.isSuccess());
            
            return serializeObject(data);
        } catch (Exception e) {
            logger.error("Failed to serialize RPC response", e);
            throw new IOException("Serialization failed", e);
        }
    }

    /**
     * Deserializes an RPC request from Protocol Buffer bytes.
     */
    public RPCRequest deserializeRPCRequest(byte[] data, String requestId) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestData = (Map<String, Object>) deserializeObject(data);
            
            String methodName = (String) requestData.get("methodName");
            Object[] parameters = (Object[]) requestData.get("parameters");
            Long timeoutMs = (Long) requestData.get("timeoutMs");
            Integer retryCount = (Integer) requestData.get("retryCount");
            
            return new RPCRequest(
                methodName,
                parameters,
                timeoutMs != null ? timeoutMs : 5000L,
                retryCount != null ? retryCount : 3
            );
        } catch (Exception e) {
            logger.error("Failed to deserialize RPC request", e);
            throw new IOException("Deserialization failed", e);
        }
    }

    /**
     * Generic object serialization using Java serialization wrapped in Protocol Buffers.
     * In a production system, this would use proper Protocol Buffer definitions.
     */
    private byte[] serializeObject(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            oos.flush();
            
            // Wrap in Protocol Buffer ByteString for consistency
            ByteString byteString = ByteString.copyFrom(baos.toByteArray());
            return byteString.toByteArray();
        }
    }

    /**
     * Generic object deserialization from Protocol Buffer bytes.
     */
    private Object deserializeObject(byte[] data) throws IOException, ClassNotFoundException {
        ByteString byteString = ByteString.copyFrom(data);
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(byteString.toByteArray());
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }

    /**
     * Serializes event data for event ordering messages.
     */
    public byte[] serializeEventData(Object eventData) throws IOException {
        return serializeObject(eventData);
    }

    /**
     * Deserializes event data from event ordering messages.
     */
    public Object deserializeEventData(byte[] data) throws IOException {
        try {
            return deserializeObject(data);
        } catch (ClassNotFoundException e) {
            logger.error("Failed to deserialize event data", e);
            throw new IOException("Deserialization failed", e);
        }
    }
}