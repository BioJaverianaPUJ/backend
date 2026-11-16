package com.biojaveriana.api.gamification.predicate;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Evaluates predicates by resolving leaf types from the registry.
 */
@Component
public class PredicateEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PredicateEvaluator.class);

    private final LeafPredicateRegistry registry;

    public PredicateEvaluator(LeafPredicateRegistry registry) {
        this.registry = registry;
    }

    /**
     * Evaluates a leaf predicate by type name and arguments.
     */
    public boolean evaluateLeaf(String type, Predicate.Context context) {
        Optional<LeafPredicate> optPredicate = registry.get(type);
        if (optPredicate.isEmpty()) {
            log.warn("Unknown leaf predicate type: {}", type);
            return false;
        }
        return optPredicate.get().evaluate(context);
    }

    /**
     * Evaluates a predicate tree from JSON.
     */
    public boolean evaluate(String predicateJson, Predicate.Context context) {
        try {
            JsonNode node = PredicateDeserializer.getMapper().readTree(predicateJson);
            Predicate predicate = PredicateDeserializer.deserialize(node);
            return predicate.evaluate(context);
        } catch (Exception e) {
            log.error("Failed to evaluate predicate: {}", predicateJson, e);
            return false;
        }
    }

    /**
     * Evaluates a predicate tree directly from a Predicate object.
     */
    public boolean evaluate(Predicate predicate, Predicate.Context context) {
        if (predicate == null) {
            return true;
        }
        return predicate.evaluate(context);
    }
}

