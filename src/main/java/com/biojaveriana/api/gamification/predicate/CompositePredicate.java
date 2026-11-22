package com.biojaveriana.api.gamification.predicate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base class for composite predicates (AND/OR/NOT).
 */
public abstract class CompositePredicate implements Predicate {

    protected final Predicate left;
    protected final Predicate right;

    protected CompositePredicate(Predicate left, Predicate right) {
        this.left = left;
        this.right = right;
    }

    public Predicate getLeft() {
        return left;
    }

    public Predicate getRight() {
        return right;
    }

    protected Predicate deserialize(JsonNode node) {
        return PredicateDeserializer.instance().deserialize(node);
    }

    protected JsonNode serialize(JsonMapper mapper) {
        return mapper.valueToTree(this);
    }
}
