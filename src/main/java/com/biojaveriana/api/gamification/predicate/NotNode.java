package com.biojaveriana.api.gamification.predicate;

/**
 * Logical NOT - satisfied when the child predicate is NOT satisfied.
 */
public record NotNode(PredicateNode child) implements PredicateNode {
}