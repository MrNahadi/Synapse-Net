package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Recommended action to address a constraint violation.
 */
public class ConstraintAction {
    private final ActionType actionType;
    private final String description;
    
    public ConstraintAction(ActionType actionType, String description) {
        this.actionType = Objects.requireNonNull(actionType);
        this.description = Objects.requireNonNull(description);
    }
    
    public ActionType getActionType() { return actionType; }
    public String getDescription() { return description; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstraintAction that = (ConstraintAction) o;
        return actionType == that.actionType && Objects.equals(description, that.description);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(actionType, description);
    }
    
    @Override
    public String toString() {
        return "ConstraintAction{" +
               "actionType=" + actionType +
               ", description='" + description + '\'' +
               '}';
    }
}