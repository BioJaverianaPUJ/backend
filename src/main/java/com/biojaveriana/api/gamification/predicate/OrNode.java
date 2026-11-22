package com.biojaveriana.api.gamification.predicate;

import java.util.List;

/**
 * Logical OR - satisfied when at least one child predicate is satisfied.
 */
public record OrNode(List<PredicateNode> children) implements PredicateNode {
    public OrNode {
        children = List.copyOf(children);
    }
}