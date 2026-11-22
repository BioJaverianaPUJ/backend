package com.biojaveriana.api.gamification.predicate;

/**
 * A leaf metric condition, e.g. "posts >= 5".
 */
public record ConditionNode(String metric, Operator operator, long value) implements PredicateNode {
}