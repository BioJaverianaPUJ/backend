package com.biojaveriana.api.gamification.predicate;

/**
 * Base interface for all predicates in the gamification system.
 * Predicates evaluate whether a given context satisfies a condition.
 */
public interface Predicate {
    boolean evaluate(Context context);

    /**
     * Holds the evaluation context.
     */
    interface Context {
        Object get(String key);
    }
}
