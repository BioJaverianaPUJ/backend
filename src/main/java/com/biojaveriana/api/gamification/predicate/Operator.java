package com.biojaveriana.api.gamification.predicate;

import com.biojaveriana.api.gamification.exception.ValidationException;

/**
 * Comparison operators usable in a metric condition.
 */
public enum Operator {
    EQ, GT, GTE, LT, LTE;

    public boolean test(long actual, long target) {
        return switch (this) {
            case EQ -> actual == target;
            case GT -> actual > target;
            case GTE -> actual >= target;
            case LT -> actual < target;
            case LTE -> actual <= target;
        };
    }

    /**
     * Parses an operator string (case-insensitive). Defaults to GTE when null/blank.
     */
    public static Operator parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return GTE;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown comparison operator: " + raw);
        }
    }
}