package com.biojaveriana.api.gamification.predicate;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for leaf predicates that need JSON serialization/deserialization.
 */
public abstract class AbstractLeafPredicate implements LeafPredicate {

    private static final ObjectMapper mapper = new ObjectMapper();

    protected abstract void serialize(ObjectNode node);

    protected abstract void deserialize(JsonNode node);

    protected JsonNode serialize() {
        return PredicateDeserializer.getMapper().valueToTree(this);
    }

    protected <T> T get(String key, Class<T> type) {
        throw new UnsupportedOperationException("Implement in subclass");
    }
}
