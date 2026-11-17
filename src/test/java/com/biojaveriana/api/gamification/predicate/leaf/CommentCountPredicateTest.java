package com.biojaveriana.api.gamification.predicate.leaf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.biojaveriana.api.gamification.predicate.MapContext;

class CommentCountPredicateTest {

    private CommentCountPredicate predicate;

    @BeforeEach
    void setUp() {
        predicate = new CommentCountPredicate();
    }

    @Test
    @DisplayName("should return true when comment count equals target with eq operator")
    void shouldReturnTrueWhenCommentCountEqualsTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "eq");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", 10L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when comment count does not equal target")
    void shouldReturnFalseWhenCommentCountDoesNotEqualTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "eq");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", 8L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return true when comment count >= target with gte operator")
    void shouldReturnTrueWhenCommentCountGteTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "gte");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", 10L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when comment count < target with gte operator")
    void shouldReturnFalseWhenCommentCountLtTargetForGte() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "gte");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", 9L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return true when comment count > target with gt operator")
    void shouldReturnTrueWhenCommentCountGtTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "gt");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", 11L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when comment count == target with gt operator")
    void shouldReturnFalseWhenCommentCountEqualsTargetForGt() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "gt");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", 10L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return true when comment count <= target with lte operator")
    void shouldReturnTrueWhenCommentCountLteTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "lte");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", 10L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when comment count > target with lte operator")
    void shouldReturnFalseWhenCommentCountGtTargetForLte() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "lte");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", 11L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return true when comment count < target with lt operator")
    void shouldReturnTrueWhenCommentCountLtTarget() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "lt");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", 9L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when comment count == target with lt operator")
    void shouldReturnFalseWhenCommentCountEqualsTargetForLt() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "lt");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", 10L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when comment count is null")
    void shouldReturnFalseWhenCommentCountIsNull() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("target", 10); args.put("operator", "eq");
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("comment_count", null);
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
        contextMap.put("comment_count", 10L);
        MapContext context = new MapContext(contextMap);
        context.getMap().put("predicate_args", args);
        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return correct type")
    void shouldReturnCorrectType() {
        assertEquals("comment_count", predicate.getType());
    }
}
