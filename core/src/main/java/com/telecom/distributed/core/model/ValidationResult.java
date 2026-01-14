package com.telecom.distributed.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Result of simulation validation.
 */
public class ValidationResult {
    private final boolean valid;
    private final List<ValidationIssue> issues;
    
    public ValidationResult(boolean valid, List<ValidationIssue> issues) {
        this.valid = valid;
        this.issues = Objects.requireNonNull(issues);
    }
    
    public boolean isValid() { return valid; }
    public List<ValidationIssue> getIssues() { return issues; }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
               "valid=" + valid +
               ", issues=" + issues.size() +
               '}';
    }
}