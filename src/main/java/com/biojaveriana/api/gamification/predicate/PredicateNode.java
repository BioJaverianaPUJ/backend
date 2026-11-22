package com.biojaveriana.api.gamification.predicate;

/**
 * A node in an achievement predicate tree. Predicates are built from "logic
 * blocks": logical combinators (AND/OR/NOT) that compose metric conditions.
 */
public sealed interface PredicateNode permits AndNode, OrNode, NotNode, ConditionNode {
}