package com.biojaveriana.api.gamification.metric;

import java.util.UUID;

/**
 * A measurable user statistic used by achievement metric conditions.
 * Implementations query the database for the current value of a user.
 */
public interface Metric {
    /**
     * The unique type identifier referenced by metric conditions (e.g. "posts").
     */
    String type();

    /**
     * Returns the current value of this metric for the given user.
     */
    Long value(UUID userId);
}