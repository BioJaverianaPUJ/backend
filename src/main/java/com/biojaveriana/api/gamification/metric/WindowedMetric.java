package com.biojaveriana.api.gamification.metric;

import java.time.Instant;
import java.util.UUID;

/**
 * A metric that can be measured over a time window. Mission predicates are
 * always evaluated against windowed values (e.g. "posts since the start of
 * today"), so mission predicates may only reference windowed metrics.
 */
public interface WindowedMetric extends Metric {
    /**
     * Returns the value of this metric for the given user, counting only
     * activity from the given instant onward.
     */
    Long valueSince(UUID userId, Instant since);
}
