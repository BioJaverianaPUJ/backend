package com.biojaveriana.api.gamification.predicate;

import java.util.List;

/**
 * Logical AND - satisfied when every child predicate is satisfied.
 */
public record AndNode(List<PredicateNode> children) implements PredicateNode {
    public AndNode {
        children = List.copyOf(children);
    }
}