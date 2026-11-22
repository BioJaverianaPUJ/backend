package com.biojaveriana.api.gamification.predicate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.exception.ValidationException;

/**
 * Converts between the structured {@link PredicateNode} tree and its canonical
 * JSON representation (stored in the database and accepted in API requests).
 */
@Component
public class PredicateCodec {

    private final JsonMapper jsonMapper;

    public PredicateCodec(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Decodes a predicate from its JSON representation, validating its shape.
     */
    public PredicateNode fromNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new ValidationException("Predicate must be a JSON object");
        }
        JsonNode and = node.get("and");
        if (and != null) {
            return new AndNode(children(and, "and"));
        }
        JsonNode or = node.get("or");
        if (or != null) {
            return new OrNode(children(or, "or"));
        }
        JsonNode not = node.get("not");
        if (not != null) {
            return new NotNode(fromNode(not));
        }
        JsonNode metric = node.get("metric");
        if (metric != null) {
            return condition(node);
        }
        throw new ValidationException("Predicate must use one of: 'and', 'or', 'not' or 'metric'");
    }

    /**
     * Encodes a predicate to its canonical JSON representation.
     */
    public JsonNode toNode(PredicateNode node) {
        return jsonMapper.valueToTree(toMap(node));
    }

    private List<PredicateNode> children(JsonNode array, String key) {
        if (array == null || !array.isArray() || array.size() == 0) {
            throw new ValidationException("'" + key + "' must be a non-empty array of predicates");
        }
        List<PredicateNode> result = new ArrayList<>();
        for (JsonNode child : array) {
            result.add(fromNode(child));
        }
        return result;
    }

    private ConditionNode condition(JsonNode node) {
        JsonNode metricNode = node.get("metric");
        if (metricNode == null || metricNode.isNull() || metricNode.asText().isBlank()) {
            throw new ValidationException("A metric condition requires a non-empty 'metric'");
        }
        Operator operator = Operator.parse(node.get("op") == null ? null : node.get("op").asText());
        JsonNode valueNode = node.get("value");
        if (valueNode == null || !valueNode.isNumber()) {
            throw new ValidationException("A metric condition requires a numeric 'value'");
        }
        return new ConditionNode(metricNode.asText().trim(), operator, valueNode.asLong());
    }

    private Object toMap(PredicateNode node) {
        return switch (node) {
            case AndNode and -> single("and", and.children());
            case OrNode or -> single("or", or.children());
            case NotNode not -> Map.of("not", toMap(not.child()));
            case ConditionNode condition -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("metric", condition.metric());
                map.put("op", condition.operator().name().toLowerCase());
                map.put("value", condition.value());
                yield map;
            }
        };
    }

    private Map<String, Object> single(String key, List<PredicateNode> children) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, children.stream().map(this::toMap).toList());
        return map;
    }
}