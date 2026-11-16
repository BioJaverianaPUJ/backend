package com.biojaveriana.api.gamification.predicate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Registry of all leaf predicates. Allows dynamic lookup by type name.
 */
@Component
public class LeafPredicateRegistry {

    private final Map<String, LeafPredicate> predicates = new HashMap<>();

    public LeafPredicateRegistry(Map<String, LeafPredicate> predicateMap) {
        for (LeafPredicate predicate : predicateMap.values()) {
            predicates.put(predicate.getType(), predicate);
        }
    }

    public Optional<LeafPredicate> get(String type) {
        return Optional.ofNullable(predicates.get(type));
    }

    public void register(LeafPredicate predicate) {
        predicates.put(predicate.getType(), predicate);
    }
}
