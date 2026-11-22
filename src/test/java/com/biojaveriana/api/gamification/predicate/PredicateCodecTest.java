package com.biojaveriana.api.gamification.predicate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.biojaveriana.api.gamification.exception.ValidationException;

class PredicateCodecTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private PredicateCodec codec;

    @BeforeEach
    void setUp() {
        codec = new PredicateCodec(jsonMapper);
    }

    private JsonNode node(String json) throws Exception {
        return jsonMapper.readTree(json);
    }

    @Test
    @DisplayName("should decode a metric condition")
    void shouldDecodeMetricCondition() throws Exception {
        PredicateNode node = codec.fromNode(node("{\"metric\":\"posts\",\"op\":\"gte\",\"value\":5}"));
        assertInstanceOf(ConditionNode.class, node);
        ConditionNode c = (ConditionNode) node;
        assertEquals("posts", c.metric());
        assertEquals(Operator.GTE, c.operator());
        assertEquals(5L, c.value());
    }

    @Test
    @DisplayName("should default operator to GTE when omitted")
    void shouldDefaultOperatorToGteWhenOmitted() throws Exception {
        PredicateNode node = codec.fromNode(node("{\"metric\":\"posts\",\"value\":5}"));
        assertEquals(Operator.GTE, ((ConditionNode) node).operator());
    }

    @Test
    @DisplayName("should decode a nested AND/OR/NOT tree")
    void shouldDecodeNestedTree() throws Exception {
        String json = """
            {
              "and": [
                { "or": [
                    { "metric": "posts", "op": "gte", "value": 5 },
                    { "metric": "observations", "op": "gte", "value": 10 }
                ] },
                { "not": { "metric": "comments", "op": "lt", "value": 2 } }
              ]
            }
            """;
        PredicateNode node = codec.fromNode(node(json));
        assertInstanceOf(AndNode.class, node);
        AndNode and = (AndNode) node;
        assertEquals(2, and.children().size());
        assertInstanceOf(OrNode.class, and.children().get(0));
        assertInstanceOf(NotNode.class, and.children().get(1));
    }

    @Test
    @DisplayName("should round-trip a predicate through toNode/fromNode")
    void shouldRoundTripPredicate() throws Exception {
        PredicateNode original = new AndNode(List.of(
            new ConditionNode("posts", Operator.GTE, 3),
            new NotNode(new ConditionNode("comments", Operator.EQ, 0))
        ));
        JsonNode encoded = codec.toNode(original);
        PredicateNode decoded = codec.fromNode(encoded);
        assertInstanceOf(AndNode.class, decoded);
    }

    @Test
    @DisplayName("should throw for a predicate with no known key")
    void shouldThrowForUnknownKey() throws Exception {
        assertThrows(ValidationException.class, () -> codec.fromNode(node("{\"foo\":1}")));
    }

    @Test
    @DisplayName("should throw for an empty AND array")
    void shouldThrowForEmptyAndArray() throws Exception {
        assertThrows(ValidationException.class, () -> codec.fromNode(node("{\"and\":[]}")));
    }

    @Test
    @DisplayName("should throw for a metric condition without a value")
    void shouldThrowForMetricWithoutValue() throws Exception {
        assertThrows(ValidationException.class, () -> codec.fromNode(node("{\"metric\":\"posts\",\"op\":\"gte\"}")));
    }

    @Test
    @DisplayName("should throw for an invalid operator")
    void shouldThrowForInvalidOperator() throws Exception {
        assertThrows(ValidationException.class,
            () -> codec.fromNode(node("{\"metric\":\"posts\",\"op\":\"nope\",\"value\":1}")));
    }
}