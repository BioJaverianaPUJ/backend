package com.biojaveriana.api.gamification.predicate;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.metric.Metric;
import com.biojaveriana.api.gamification.metric.MetricRegistry;
import com.biojaveriana.api.gamification.metric.WindowedMetric;

/**
 * Evaluates a {@link PredicateNode} tree against a user's current metrics.
 */
@Component
public class PredicateEvaluator {
    private static final Logger log = LoggerFactory.getLogger(PredicateEvaluator.class);

    private final MetricRegistry metricRegistry;

    public PredicateEvaluator(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    /**
     * Evaluates the predicate tree for the given user using lifetime metric
     * values. A null predicate is considered trivially satisfied.
     */
    public boolean evaluate(PredicateNode node, UUID userId) {
        return evaluate(node, userId, null);
    }

    /**
     * Evaluates the predicate tree for the given user, counting only activity
     * from {@code since} onward for metrics that support time windows
     * ({@link WindowedMetric}). Non-windowed metrics (or a null
     * {@code since}) fall back to their lifetime value. A null predicate is
     * considered trivially satisfied.
     */
    public boolean evaluate(PredicateNode node, UUID userId, Instant since) {
        if (node == null) {
            return true;
        }
        return switch (node) {
            case AndNode and -> and.children().stream().allMatch(child -> evaluate(child, userId, since));
            case OrNode or -> or.children().stream().anyMatch(child -> evaluate(child, userId, since));
            case NotNode not -> !evaluate(not.child(), userId, since);
            case ConditionNode condition -> evaluateCondition(condition, userId, since);
        };
    }

    private boolean evaluateCondition(ConditionNode condition, UUID userId, Instant since) {
        Metric metric = metricRegistry.get(condition.metric()).orElse(null);
        if (metric == null) {
            log.warn("Unknown metric '{}' in achievement predicate; treating as unsatisfied.", condition.metric());
            return false;
        }
        Long value = (metric instanceof WindowedMetric windowed && since != null)
            ? windowed.valueSince(userId, since)
            : metric.value(userId);
        if (value == null) {
            return false;
        }
        return condition.operator().test(value, condition.value());
    }
}