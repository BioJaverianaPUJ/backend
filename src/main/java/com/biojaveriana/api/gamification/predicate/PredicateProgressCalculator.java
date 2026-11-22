package com.biojaveriana.api.gamification.predicate;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.dto.PredicateProgress;

/**
 * Computes the live {@link PredicateProgress} tree for a predicate, given a
 * pre-fetched map of metric type to current user value. Each distinct metric
 * should be fetched once per user (see {@link #referencedMetrics}) and passed
 * in, so the calculation itself performs no I/O. Conditions referencing
 * unknown or unavailable metrics are reported as unsatisfied with a null
 * current value.
 */
@Component
public class PredicateProgressCalculator {

    /**
     * Collects the normalized metric types referenced anywhere in the tree.
     */
    public Set<String> referencedMetrics(PredicateNode node, Set<String> acc) {
        return switch (node) {
            case AndNode and -> {
                and.children().forEach(child -> referencedMetrics(child, acc));
                yield acc;
            }
            case OrNode or -> {
                or.children().forEach(child -> referencedMetrics(child, acc));
                yield acc;
            }
            case NotNode not -> {
                referencedMetrics(not.child(), acc);
                yield acc;
            }
            case ConditionNode condition -> {
                acc.add(normalize(condition.metric()));
                yield acc;
            }
        };
    }

    /**
     * Builds the progress tree for the given predicate.
     */
    public PredicateProgress calculate(PredicateNode node, Map<String, Long> metricValues) {
        return switch (node) {
            case AndNode and -> PredicateProgress.and(and.children().stream()
                .map(child -> calculate(child, metricValues)).toList());
            case OrNode or -> PredicateProgress.or(or.children().stream()
                .map(child -> calculate(child, metricValues)).toList());
            case NotNode not -> PredicateProgress.not(calculate(not.child(), metricValues));
            case ConditionNode condition -> calculateCondition(condition, metricValues);
        };
    }

    private PredicateProgress calculateCondition(ConditionNode condition, Map<String, Long> metricValues) {
        Long current = metricValues.get(normalize(condition.metric()));
        boolean satisfied = current != null && condition.operator().test(current, condition.value());
        return PredicateProgress.condition(
            condition.metric(),
            condition.value(),
            current,
            condition.operator().name().toLowerCase(Locale.ROOT),
            satisfied
        );
    }

    private String normalize(String metric) {
        return metric.trim().toLowerCase(Locale.ROOT);
    }
}
