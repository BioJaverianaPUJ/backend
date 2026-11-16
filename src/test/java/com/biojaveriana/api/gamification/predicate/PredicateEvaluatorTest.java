package com.biojaveriana.api.gamification.predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PredicateEvaluatorTest {

    @Mock
    private LeafPredicateRegistry registry;

    private PredicateEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new PredicateEvaluator(registry);
    }

    @Nested
    @DisplayName("evaluateLeaf")
    class EvaluateLeafTests {

        @Test
        @DisplayName("should return true when leaf predicate evaluates to true")
        void shouldReturnTrueWhenPredicateEvaluatesToTrue() {
            LeafPredicate mockPredicate = mock(LeafPredicate.class);
            when(mockPredicate.getType()).thenReturn("test_type");
            when(mockPredicate.evaluate(any())).thenReturn(true);
            when(registry.get("test_type")).thenReturn(Optional.of(mockPredicate));

            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            boolean result = evaluator.evaluateLeaf("test_type", context);

            assertTrue(result);
            verify(mockPredicate).evaluate(context);
        }

        @Test
        @DisplayName("should return false when leaf predicate evaluates to false")
        void shouldReturnFalseWhenPredicateEvaluatesToFalse() {
            LeafPredicate mockPredicate = mock(LeafPredicate.class);
            when(mockPredicate.getType()).thenReturn("test_type");
            when(mockPredicate.evaluate(any())).thenReturn(false);
            when(registry.get("test_type")).thenReturn(Optional.of(mockPredicate));

            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            boolean result = evaluator.evaluateLeaf("test_type", context);

            assertFalse(result);
            verify(mockPredicate).evaluate(context);
        }

        @Test
        @DisplayName("should return false when predicate type is not found")
        void shouldReturnFalseWhenPredicateTypeNotFound() {
            when(registry.get("unknown_type")).thenReturn(Optional.empty());
            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            boolean result = evaluator.evaluateLeaf("unknown_type", context);
            assertFalse(result);
        }

        @Test
        @DisplayName("should return false when predicate type is null")
        void shouldReturnFalseWhenPredicateTypeIsNull() {
            when(registry.get(null)).thenReturn(Optional.empty());
            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            boolean result = evaluator.evaluateLeaf(null, context);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("evaluate with JSON")
    class EvaluateJsonTests {

        @Test
        @DisplayName("should evaluate a simple leaf predicate from JSON")
        void shouldEvaluateSimpleLeafPredicateFromJson() {
            String predicateJson = """
                {
                    "type": "leaf",
                    "content": {
                        "leaf": {
                            "type": "post_count",
                            "predicate": {
                                "target": 5,
                                "operator": "gte"
                            }
                        }
                    }
                }
                """;
            Map<String, Object> contextMap = new LinkedHashMap<>();
            contextMap.put("post_count", 10L);
            contextMap.put("predicate_args", new LinkedHashMap<String, Object>());
            Predicate.Context context = new MapContext(contextMap);

            boolean result = evaluator.evaluate(predicateJson, context);
            assertTrue(result);
        }

        @Test
        @DisplayName("should return false for invalid JSON")
        void shouldReturnFalseForInvalidJson() {
            String invalidJson = "{ invalid json }";
            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            boolean result = evaluator.evaluate(invalidJson, context);
            assertFalse(result);
        }

        @Test
        @DisplayName("should handle JSON with missing type field")
        void shouldHandleJsonWithMissingTypeField() {
            String invalidJson = """
                {
                    "content": {
                        "leaf": {
                            "type": "post_count"
                        }
                    }
                }
                """;
            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            boolean result = evaluator.evaluate(invalidJson, context);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("evaluate with Predicate object")
    class EvaluatePredicateTests {

        @Test
        @DisplayName("should return true for null predicate")
        void shouldReturnTrueForNullPredicate() {
            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            boolean result = evaluator.evaluate((Predicate) null, context);
            assertTrue(result);
        }

        @Test
        @DisplayName("should evaluate AndPredicate correctly")
        void shouldEvaluateAndPredicateCorrectly() {
            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            Predicate left = ctx -> true;
            Predicate right = ctx -> false;
            Predicate andPredicate = new AndPredicate(left, right);

            boolean result = evaluator.evaluate(andPredicate, context);
            assertFalse(result);
        }

        @Test
        @DisplayName("should evaluate OrPredicate correctly")
        void shouldEvaluateOrPredicateCorrectly() {
            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            Predicate left = ctx -> false;
            Predicate right = ctx -> true;
            Predicate orPredicate = new OrPredicate(left, right);

            boolean result = evaluator.evaluate(orPredicate, context);
            assertTrue(result);
        }

        @Test
        @DisplayName("should evaluate NotPredicate correctly")
        void shouldEvaluateNotPredicateCorrectly() {
            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            Predicate child = ctx -> true;
            Predicate notPredicate = new NotPredicate(child);

            boolean result = evaluator.evaluate(notPredicate, context);
            assertFalse(result);
        }

        @Test
        @DisplayName("should evaluate nested predicates correctly")
        void shouldEvaluateNestedPredicatesCorrectly() {
            Predicate.Context context = new MapContext(new LinkedHashMap<>());
            Predicate a = ctx -> true;
            Predicate b = ctx -> false;
            Predicate c = ctx -> true;

            Predicate andPredicate = new AndPredicate(a, b);
            Predicate orPredicate = new OrPredicate(andPredicate, c);

            boolean result = evaluator.evaluate(orPredicate, context);
            assertTrue(result);
        }
    }
}
