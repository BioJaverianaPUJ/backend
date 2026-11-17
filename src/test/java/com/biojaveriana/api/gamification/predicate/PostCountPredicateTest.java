package com.biojaveriana.api.gamification.predicate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.biojaveriana.api.gamification.predicate.leaf.PostCountPredicate;

class PostCountPredicateTest {
    private PostCountPredicate predicate;

    @BeforeEach
    void setUp() {
        predicate = new PostCountPredicate();
    }

    @Test
    @DisplayName("should return true when post count equals target with eq operator")
    void shouldReturnTrueWhenPostCountEqualsTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "eq");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 5L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when post count does not equal target")
    void shouldReturnFalseWhenPostCountDoesNotEqualTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "eq");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 3L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return true when post count >= target with gte operator")
    void shouldReturnTrueWhenPostCountGteTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "gte");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 5L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when post count < target with gte operator")
    void shouldReturnFalseWhenPostCountLtTargetForGte() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "gte");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 4L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return true when post count > target with gt operator")
    void shouldReturnTrueWhenPostCountGtTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "gt");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 6L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when post count == target with gt operator")
    void shouldReturnFalseWhenPostCountEqualsTargetForGt() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "gt");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 5L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return true when post count <= target with lte operator")
    void shouldReturnTrueWhenPostCountLteTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "lte");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 5L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when post count > target with lte operator")
    void shouldReturnFalseWhenPostCountGtTargetForLte() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "lte");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 6L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return true when post count < target with lt operator")
    void shouldReturnTrueWhenPostCountLtTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "lt");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 4L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when post count == target with lt operator")
    void shouldReturnFalseWhenPostCountEqualsTargetForLt() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "lt");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 5L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when post count is null")
    void shouldReturnFalseWhenPostCountIsNull() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 5); args.put("operator", "eq");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", null);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when target is null")
    void shouldReturnFalseWhenTargetIsNull() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("operator", "eq");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 5L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return correct type")
    void shouldReturnCorrectType() {
        assertEquals("post_count", predicate.getType());
    }
}
