package com.biojaveriana.api.gamification.predicate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry of all leaf predicates. Allows dynamic lookup by type name.
 * Detects and logs duplicate type registrations to prevent silent overwrites.
 */
@Component
public class LeafPredicateRegistry {
    private static final Logger log = LoggerFactory.getLogger(LeafPredicateRegistry.class);

    private final Map<String, LeafPredicate> predicates = new HashMap<>();

    public LeafPredicateRegistry(Map<String, LeafPredicate> predicateMap) {
        for (Map.Entry<String, LeafPredicate> entry : predicateMap.entrySet()) {
            String beanName = entry.getKey();
            LeafPredicate predicate = entry.getValue();
            String predicateType = predicate.getType();

            if (predicates.containsKey(predicateType)) {
                log.warn(
                    "Duplicate leaf predicate type '{}' detected. Bean '{}' will be ignored in favor of existing registration.",
                    predicateType, beanName
                );
            } else {
                predicates.put(predicateType, predicate);
                log.debug("Registered leaf predicate type '{}' from bean '{}'", predicateType, beanName);
            }
        }
    }

    public Optional<LeafPredicate> get(String type) {
        return Optional.ofNullable(predicates.get(type));
    }

    public void register(LeafPredicate predicate) {
        String type = predicate.getType();
        if (predicates.containsKey(type)) {
            log.warn("Overwriting existing predicate type '{}'", type);
        }
        predicates.put(type, predicate);
    }
}
