package com.biojaveriana.api.gamification.dto;

import java.util.List;

/**
 * Live, computed progress for a single predicate node within an achievement.
 *
 * <p>A single record is used for every node type. The application's
 * {@code non_null} JSON inclusion serializes only the fields relevant to each
 * type: a leaf condition exposes {@code metric}/{@code current}/{@code target}/
 * {@code operator}; an AND/OR node exposes {@code metChildren}/
 * {@code totalChildren} and {@code children}; a NOT node exposes {@code child}.
 */
public record PredicateProgress(
    String type,
    boolean satisfied,
    String metric,
    Long current,
    Long target,
    String operator,
    Integer metChildren,
    Integer totalChildren,
    List<PredicateProgress> children,
    PredicateProgress child
) {
    public static PredicateProgress condition(String metric, Long target, Long current, String operator, boolean satisfied) {
        return new PredicateProgress("CONDITION", satisfied, metric, current, target, operator, null, null, null, null);
    }

    public static PredicateProgress and(List<PredicateProgress> children) {
        int met = (int) children.stream().filter(PredicateProgress::satisfied).count();
        return new PredicateProgress("AND", met == children.size() && !children.isEmpty(), null, null, null, null,
            met, children.size(), children, null);
    }

    public static PredicateProgress or(List<PredicateProgress> children) {
        int met = (int) children.stream().filter(PredicateProgress::satisfied).count();
        return new PredicateProgress("OR", met > 0, null, null, null, null, met, children.size(), children, null);
    }

    public static PredicateProgress not(PredicateProgress child) {
        return new PredicateProgress("NOT", !child.satisfied(), null, null, null, null, null, null, null, child);
    }
}