package com.biojaveriana.api.gamification.predicate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Base class for leaf predicates that need JSON serialization/deserialization.
 * Uses JsonMapper for JSON operations.
 */
public abstract class AbstractLeafPredicate implements LeafPredicate {

    protected abstract void serialize(ObjectNode node);

    protected abstract void deserialize(JsonNode node);

    /**
     * Serializes this predicate to a JsonNode.
     */
    protected JsonNode serialize(JsonMapper mapper) {
        return mapper.valueToTree(this);
    }

    protected <T> T get(String key, Class<T> type) {
        throw new UnsupportedOperationException("Implement in subclass");
    }
}
