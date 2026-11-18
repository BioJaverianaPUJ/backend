package com.biojaveriana.api.gamification.predicate;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for leaf predicates that need JSON serialization/deserialization.
 * Uses ObjectMapper for JSON operations.
 */
public abstract class AbstractLeafPredicate implements LeafPredicate {

    protected abstract void serialize(ObjectNode node);

    protected abstract void deserialize(JsonNode node);

    /**
     * Serializes this predicate to a JsonNode.
     */
    protected JsonNode serialize(ObjectMapper mapper) {
        return mapper.valueToTree(this);
    }

    protected <T> T get(String key, Class<T> type) {
        throw new UnsupportedOperationException("Implement in subclass");
    }
}
