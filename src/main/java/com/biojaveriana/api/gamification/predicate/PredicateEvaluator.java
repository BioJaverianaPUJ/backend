package com.biojaveriana.api.gamification.predicate;

import java.util.Map;
import java.util.Optional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Evaluates predicates by resolving leaf types from the registry.
 * Handles the full evaluation flow: deserializing JSON predicates
 * and delegating leaf evaluation to registered implementations.
 */
@Component
public class PredicateEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PredicateEvaluator.class);

    private final LeafPredicateRegistry registry;
    private final JsonMapper JsonMapper;

    public PredicateEvaluator(LeafPredicateRegistry registry, JsonMapper JsonMapper) {
        this.registry = registry;
        this.JsonMapper = JsonMapper;
    }

    /**
     * Evaluates a leaf predicate by type name and arguments.
     * Looks up the registered implementation and evaluates it with the given context.
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
     * Deserializes the JSON and evaluates leaf nodes by delegating
     * to the LeafPredicateRegistry for type-specific resolution.
     */
    public boolean evaluate(String predicateJson, Predicate.Context context) {
        try {
            JsonNode node = JsonMapper.readTree(predicateJson);
            return evaluateNode(node, context);
        } catch (JacksonException e) {
            log.error("Failed to evaluate predicate: {}", predicateJson, e);
            return false;
        }
    }

    /**
     * Recursively evaluates a JsonNode as a predicate.
     * For leaf nodes, delegates to the LeafPredicateRegistry.
     */
    private boolean evaluateNode(JsonNode node, Predicate.Context context) {
        if (!node.isObject()) {
            log.warn("Predicate JSON must be an object, got: {}", node.getNodeType());
            return false;
        }

        JsonNode typeNode = node.get("type");
        if (typeNode == null || typeNode.isNull()) {
            log.warn("Predicate JSON missing 'type' field");
            return false;
        }
        String type = typeNode.asText();
        JsonNode content = node.get("content");

        if (content == null || !content.isObject()) {
            log.warn("Predicate JSON missing valid 'content' field");
            return false;
        }

        return switch (type) {
            case "and" -> {
                JsonNode left = content.get("left");
                JsonNode right = content.get("right");
                if (left == null || right == null) {
                    yield false;
                }
                yield evaluateNode(left, context) && evaluateNode(right, context);
            }
            case "or" -> {
                JsonNode left = content.get("left");
                JsonNode right = content.get("right");
                if (left == null || right == null) {
                    yield false;
                }
                yield evaluateNode(left, context) || evaluateNode(right, context);
            }
            case "not" -> {
                JsonNode child = content.get("child");
                if (child == null) {
                    yield false;
                }
                yield !evaluateNode(child, context);
            }
            case "leaf" -> evaluateLeafNode(content.get("leaf"), context);
            default -> {
                log.warn("Unknown predicate type: {}", type);
                yield false;
            }
        };
    }

    /**
     * Evaluates a leaf node by looking up the registered predicate implementation.
     */
    private boolean evaluateLeafNode(JsonNode leafNode, Predicate.Context context) {
        if (leafNode == null || !leafNode.isObject()) {
            log.warn("Leaf node is not a valid object");
            return false;
        }

        JsonNode typeNode = leafNode.get("type");
        if (typeNode == null || typeNode.isNull()) {
            log.warn("Leaf node missing 'type' field");
            return false;
        }
        String leafType = typeNode.asText();
        JsonNode predicateNode = leafNode.get("predicate");

        // Build a context map with the leaf's arguments
        Map<String, Object> args = predicateToMap(predicateNode);

        // Add args to context if it's a MapContext
        if (context instanceof MapContext mapContext) {
            args.forEach(mapContext.getMap()::put);
        }

        // Delegate to the registry for the actual evaluation
        return evaluateLeaf(leafType, context);
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

    /**
     * Converts a JsonNode to a Map of String to Object.
     */
    private Map<String, Object> predicateToMap(JsonNode node) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.properties().forEach(entry -> {
                map.put(entry.getKey(), toJavaValue(entry.getValue()));
            });
        }
        return map;
    }

    /**
     * Converts a JsonNode value to a Java object.
     */
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
}

