package com.biojaveriana.api.gamification.predicate;

import java.util.Map;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Deserializes JSON representation of predicates back to Java objects.
 * Uses the injected ObjectMapper to respect Spring's Jackson configuration.
 */
@Component
public class PredicateDeserializer {

    private static ApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LeafPredicateRegistry registry;

    public PredicateDeserializer() {
        // Required for Spring proxy creation
    }

    @Autowired
    public PredicateDeserializer(ObjectMapper objectMapper, LeafPredicateRegistry registry) {
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        PredicateDeserializer.applicationContext = applicationContext;
    }

    /**
     * Returns the Spring-managed instance of PredicateDeserializer.
     * Used by CompositePredicate and other non-Spring contexts.
     */
    public static PredicateDeserializer instance() {
        if (applicationContext == null) {
            throw new IllegalStateException("PredicateDeserializer not initialized");
        }
        return applicationContext.getBean(PredicateDeserializer.class);
    }

    public Predicate deserialize(JsonNode node) {
        if (!node.isObject()) {
            return null;
        }

        JsonNode typeNode = node.get("type");
        if (typeNode == null || typeNode.isNull()) {
            return null;
        }
        String type = typeNode.asText();
        JsonNode content = node.get("content");

        if (content == null || content.isNull()) {
            return null;
        }

        return switch (type) {
            case "and" -> {
                JsonNode left = content.get("left");
                JsonNode right = content.get("right");
                if (left == null || right == null) {
                    yield null;
                }
                yield new AndPredicate(deserialize(left), deserialize(right));
            }
            case "or" -> {
                JsonNode left = content.get("left");
                JsonNode right = content.get("right");
                if (left == null || right == null) {
                    yield null;
                }
                yield new OrPredicate(deserialize(left), deserialize(right));
            }
            case "not" -> {
                JsonNode child = content.get("child");
                if (child == null) {
                    yield null;
                }
                yield new NotPredicate(deserialize(child));
            }
            case "leaf" -> {
                JsonNode leaf = content.get("leaf");
                if (leaf == null) {
                    yield null;
                }
                yield deserializeLeaf(leaf);
            }
            default -> null;
        };
    }

    private LeafPredicate deserializeLeaf(JsonNode leaf) {
        JsonNode typeNode = leaf.get("type");
        if (typeNode == null || typeNode.isNull()) {
            return null;
        }
        String leafType = typeNode.asText();
        JsonNode predicate = leaf.get("predicate");
        if (predicate == null || predicate.isNull()) {
            return null;
        }
        Map<String, Object> args = predicateToMap(predicate);
        return new SimpleLeafPredicate(leafType, args, registry);
    }

    private Map<String, Object> predicateToMap(JsonNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                map.put(entry.getKey(), toJavaValue(entry.getValue()));
            });
        }
        return map;
    }

    private Object toJavaValue(JsonNode node) {
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

    /**
     * Convenience method to deserialize JSON string to JsonNode.
     */
    public JsonNode readTree(String json) throws JacksonException {
        return objectMapper.readTree(json);
    }
}
