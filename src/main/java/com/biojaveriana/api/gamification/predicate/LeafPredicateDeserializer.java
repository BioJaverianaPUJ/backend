package com.biojaveriana.api.gamification.predicate;

import java.util.Map;

/**
 * Handles deserialization of leaf predicates from JSON.
 */
public class LeafPredicateDeserializer {
    private final LeafPredicateRegistry registry;

    public LeafPredicateDeserializer(LeafPredicateRegistry registry) {
        this.registry = registry;
    }

    public LeafPredicate deserialize(String type, Map<String, Object> args) {
        return new SimpleLeafPredicate(type, args, registry);
    }
}
