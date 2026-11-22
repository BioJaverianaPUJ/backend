package com.biojaveriana.api.gamification.predicate;

public interface LeafPredicate extends Predicate {
    /**
     * Returns the type identifier for this predicate (e.g., "post_count").
     */
    String getType();

    /**
     * Evaluates the predicate against the given context.
     */
    boolean evaluate(Predicate.Context context);
}

