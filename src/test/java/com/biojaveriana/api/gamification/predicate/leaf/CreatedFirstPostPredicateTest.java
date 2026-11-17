package com.biojaveriana.api.gamification.predicate.leaf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.biojaveriana.api.gamification.predicate.MapContext;

class CreatedFirstPostPredicateTest {

    private CreatedFirstPostPredicate predicate;

    @BeforeEach
    void setUp() {
        predicate = new CreatedFirstPostPredicate();
    }

    @Test
    @DisplayName("should return true when post count is 1 or more")
    void shouldReturnTrueWhenPostCountIsOneOrMore() {
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 1L);

        MapContext context = new MapContext(contextMap);

        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return true when post count is greater than 1")
    void shouldReturnTrueWhenPostCountIsGreaterThanOne() {
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 5L);

        MapContext context = new MapContext(contextMap);

        assertTrue(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when post count is 0")
    void shouldReturnFalseWhenPostCountIsZero() {
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", 0L);

        MapContext context = new MapContext(contextMap);

        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return false when post count is null")
    void shouldReturnFalseWhenPostCountIsNull() {
        Map<String, Object> contextMap = new LinkedHashMap<>();
        contextMap.put("post_count", null);

        MapContext context = new MapContext(contextMap);

        assertFalse(predicate.evaluate(context));
    }

    @Test
    @DisplayName("should return correct type")
    void shouldReturnCorrectType() {
        assertEquals("created_first_post", predicate.getType());
    }
}