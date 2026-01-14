package com.telecom.distributed.core.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Evidence collected for Byzantine failure detection.
 * Contains proof of malicious or arbitrary behavior by a node.
 */
public class ByzantineEvidence {
    private final NodeId suspectedNode;
    private final EvidenceType type;
    private final Set<NodeId> witnesses;
    private final String description;
    private final Instant timestamp;
    private final byte[] proof; // Cryptographic proof or message logs
    private final double confidenceScore; // 0.0 to 1.0

    public ByzantineEvidence(NodeId suspectedNode, EvidenceType type, Set<NodeId> witnesses,
                           String description, Instant timestamp, byte[] proof, double confidenceScore) {
        this.suspectedNode = Objects.requireNonNull(suspectedNode, "Suspected node cannot be null");
        this.type = Objects.requireNonNull(type, "Evidence type cannot be null");
        this.witnesses = Objects.requireNonNull(witnesses, "Witnesses cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.proof = proof != null ? proof.clone() : new byte[0];
        this.confidenceScore = validateConfidenceScore(confidenceScore);
    }

    private double validateConfidenceScore(double score) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Confidence score must be between 0.0 and 1.0, got: " + score);
        }
        return score;
    }

    public enum EvidenceType {
        CONFLICTING_MESSAGES("Node sent conflicting messages to different nodes"),
        INVALID_SIGNATURE("Node sent message with invalid cryptographic signature"),
        PROTOCOL_VIOLATION("Node violated consensus protocol rules"),
        TIMING_ATTACK("Node exhibited suspicious timing patterns"),
        DATA_CORRUPTION("Node corrupted data during processing");

        private final String description;

        EvidenceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getters
    public NodeId getSuspectedNode() { return suspectedNode; }
    public EvidenceType getType() { return type; }
    public Set<NodeId> getWitnesses() { return witnesses; }
    public String getDescription() { return description; }
    public Instant getTimestamp() { return timestamp; }
    public byte[] getProof() { return proof.clone(); }
    public double getConfidenceScore() { return confidenceScore; }

    public boolean isHighConfidence() {
        return confidenceScore >= 0.8;
    }

    public boolean hasSufficientWitnesses(int minimumWitnesses) {
        return witnesses.size() >= minimumWitnesses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ByzantineEvidence that = (ByzantineEvidence) o;
        return Double.compare(that.confidenceScore, confidenceScore) == 0 &&
               Objects.equals(suspectedNode, that.suspectedNode) &&
               type == that.type &&
               Objects.equals(witnesses, that.witnesses) &&
               Objects.equals(description, that.description) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(suspectedNode, type, witnesses, description, timestamp, confidenceScore);
    }

    @Override
    public String toString() {
        return "ByzantineEvidence{" +
               "suspectedNode=" + suspectedNode +
               ", type=" + type +
               ", witnesses=" + witnesses.size() +
               ", description='" + description + '\'' +
               ", timestamp=" + timestamp +
               ", confidenceScore=" + confidenceScore +
               '}';
    }
}