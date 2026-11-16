package com.biojaveriana.api.gamification.predicate;

import java.util.UUID;

/**
 * Leaf predicate â€” the atomic unit of evaluation.
 * Registered implementations are looked up by type name.
 */
public interface LeafPredicate {
    /**
     * Returns the type identifier for this predicate (e.g., "post_count").
     */
    String getType();

    /**
     * Evaluates the predicate against the given context.
     */
    boolean evaluate(Predicate.Context context);
}

