package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Represents a resource constraint violation.
 */
public class ConstraintViolation {
    private final ConstraintType type;
    private final String description;
    
    public ConstraintViolation(ConstraintType type, String description) {
        this.type = Objects.requireNonNull(type);
        this.description = Objects.requireNonNull(description);
    }
    
    public ConstraintType getType() { return type; }
    public String getDescription() { return description; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstraintViolation that = (ConstraintViolation) o;
        return type == that.type && Objects.equals(description, that.description);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, description);
    }
    
    @Override
    public String toString() {
        return "ConstraintViolation{" +
               "type=" + type +
               ", description='" + description + '\'' +
               '}';
    }
}