package com.biojaveriana.api.gamification.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    protected JsonNode serialize(ObjectMapper mapper) {
        return mapper.valueToTree(this);
    }
}
