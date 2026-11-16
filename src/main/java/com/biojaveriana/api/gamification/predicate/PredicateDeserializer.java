package com.biojaveriana.api.gamification.predicate;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Deserializes JSON representation of predicates back to Java objects.
 */
public class PredicateDeserializer {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Predicate deserialize(JsonNode node) {
        if (!node.isObject()) {
            throw new IllegalArgumentException("Predicate JSON must be an object");
        }

        String type = node.get("type").asText();
        JsonNode content = node.get("content");

        return switch (type) {
            case "and" -> {
                JsonNode left = content.get("left");
                JsonNode right = content.get("right");
                yield new AndPredicate(deserialize(left), deserialize(right));
            }
            case "or" -> {
                JsonNode left = content.get("left");
                JsonNode right = content.get("right");
                yield new OrPredicate(deserialize(left), deserialize(right));
            }
            case "not" -> {
                JsonNode child = content.get("child");
                yield new NotPredicate(deserialize(child));
            }
            case "leaf" -> {
                JsonNode leaf = content.get("leaf");
                yield deserializeLeaf(leaf);
            }
            default -> throw new IllegalArgumentException("Unknown predicate type: " + type);
        };
    }

    private static LeafPredicate deserializeLeaf(JsonNode leaf) {
        String leafType = leaf.get("type").asText();
        JsonNode predicate = leaf.get("predicate");
        Map<String, Object> args = predicateToMap(predicate);
        return new LeafPredicateDeserializer(leafType, args);
    }

    private static Map<String, Object> predicateToMap(JsonNode node) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                map.put(entry.getKey(), toJavaValue(entry.getValue()));
            });
        }
        return map;
    }

    private static Object toJavaValue(JsonNode node) {
        if (node.isNull()) return null;
        if (node.isBoolean()) return node.asBoolean();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isTextual()) return node.asText();
        if (node.isArray()) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            node.forEach(n -> list.add(toJavaValue(n)));
            return list;
        }
        return node.asText();
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }
}
