package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Represents a validation issue in simulation results.
 */
public class ValidationIssue {
    private final ValidationIssueType type;
    private final String description;
    private final ValidationSeverity severity;
    
    public ValidationIssue(ValidationIssueType type, String description, ValidationSeverity severity) {
        this.type = Objects.requireNonNull(type);
        this.description = Objects.requireNonNull(description);
        this.severity = Objects.requireNonNull(severity);
    }
    
    public ValidationIssueType getType() { return type; }
    public String getDescription() { return description; }
    public ValidationSeverity getSeverity() { return severity; }
    
    @Override
    public String toString() {
        return "ValidationIssue{" +
               "type=" + type +
               ", description='" + description + '\'' +
               ", severity=" + severity +
               '}';
    }
}