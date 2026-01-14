package com.telecom.distributed.core.model;

import java.util.Objects;

/**
 * Optimization suggestion for addressing performance bottlenecks.
 */
public class OptimizationSuggestion {
    private final String category;
    private final String description;

    public OptimizationSuggestion(String category, String description) {
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
    }

    public String getCategory() { return category; }
    public String getDescription() { return description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptimizationSuggestion that = (OptimizationSuggestion) o;
        return Objects.equals(category, that.category) &&
               Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, description);
    }

    @Override
    public String toString() {
        return "OptimizationSuggestion{" +
               "category='" + category + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}