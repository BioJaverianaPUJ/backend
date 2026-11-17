package com.biojaveriana.api.gamification.predicate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.biojaveriana.api.gamification.predicate.leaf.PostCountPredicate;
import com.biojaveriana.api.gamification.predicate.leaf.CommentCountPredicate;

class LeafPredicateRegistryTest {
    private LeafPredicateRegistry registry;

    @BeforeEach
    void setUp() {
        Map<String, LeafPredicate> predicates = new LinkedHashMap<>();
        predicates.put("post_count", new PostCountPredicate());
        predicates.put("comment_count", new CommentCountPredicate());
        registry = new LeafPredicateRegistry(predicates);
    }

    @Test
    @DisplayName("should return predicate when type exists")
    void shouldReturnPredicateWhenTypeExists() {
        Optional<LeafPredicate> result = registry.get("post_count");
        assertTrue(result.isPresent());
        assertEquals("post_count", result.get().getType());
    }

    @Test
    @DisplayName("should return empty when type does not exist")
    void shouldReturnEmptyWhenTypeDoesNotExist() {
        Optional<LeafPredicate> result = registry.get("unknown_type");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty when type is null")
    void shouldReturnEmptyWhenTypeIsNull() {
        Optional<LeafPredicate> result = registry.get(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should register new predicate dynamically")
    void shouldRegisterNewPredicate() {
        LeafPredicate newPredicate = new LeafPredicate() {
            @Override
            public String getType() { return "new_type"; }
            @Override
            public boolean evaluate(Predicate.Context context) { return true; }
        };
        registry.register(newPredicate);
        Optional<LeafPredicate> result = registry.get("new_type");
        assertTrue(result.isPresent());
        assertEquals("new_type", result.get().getType());
    }
}
