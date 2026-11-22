package com.biojaveriana.api.gamification.predicate;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.metric.Metric;
import com.biojaveriana.api.gamification.metric.MetricRegistry;

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
     * Evaluates the predicate tree for the given user. A null predicate is
     * considered trivially satisfied.
     */
    public boolean evaluate(PredicateNode node, UUID userId) {
        if (node == null) {
            return true;
        }
        return switch (node) {
            case AndNode and -> and.children().stream().allMatch(child -> evaluate(child, userId));
            case OrNode or -> or.children().stream().anyMatch(child -> evaluate(child, userId));
            case NotNode not -> !evaluate(not.child(), userId);
            case ConditionNode condition -> evaluateCondition(condition, userId);
        };
    }

    private boolean evaluateCondition(ConditionNode condition, UUID userId) {
        Metric metric = metricRegistry.get(condition.metric()).orElse(null);
        if (metric == null) {
            log.warn("Unknown metric '{}' in achievement predicate; treating as unsatisfied.", condition.metric());
            return false;
        }
        Long value = metric.value(userId);
        if (value == null) {
            return false;
        }
        return condition.operator().test(value, condition.value());
    }
}