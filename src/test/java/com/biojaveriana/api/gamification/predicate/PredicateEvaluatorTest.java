package com.biojaveriana.api.gamification.predicate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.biojaveriana.api.gamification.metric.Metric;
import com.biojaveriana.api.gamification.metric.MetricRegistry;

class PredicateEvaluatorTest {

    private final UUID userId = UUID.randomUUID();
    private MetricRegistry registry;
    private PredicateEvaluator evaluator;

    private Metric metric(String type, long value) {
        return new Metric() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public Long value(UUID id) {
                return value;
            }
        };
    }

    @BeforeEach
    void setUp() {
        registry = new MetricRegistry(List.of(
            metric("posts", 5L),
            metric("comments", 3L),
            metric("observations", 10L),
            metric("reactions_received", 7L),
            metric("poi_visited", 2L),
            metric("species_identified", 4L)
        ));
        evaluator = new PredicateEvaluator(registry);
    }

    @Nested
    @DisplayName("condition evaluation")
    class ConditionTests {
        private ConditionNode condition(String metric, Operator op, long value) {
            return new ConditionNode(metric, op, value);
        }

        @Test
        @DisplayName("should evaluate a single condition true")
        void shouldEvaluateSingleConditionTrue() {
            assertTrue(evaluator.evaluate(condition("posts", Operator.GTE, 5), userId));
        }

        @Test
        @DisplayName("should evaluate a single condition false")
        void shouldEvaluateSingleConditionFalse() {
            assertFalse(evaluator.evaluate(condition("posts", Operator.GTE, 6), userId));
        }

        @Test
        @DisplayName("should evaluate each operator")
        void shouldEvaluateEachOperator() {
            assertTrue(evaluator.evaluate(condition("posts", Operator.EQ, 5), userId));
            assertTrue(evaluator.evaluate(condition("posts", Operator.GT, 4), userId));
            assertFalse(evaluator.evaluate(condition("posts", Operator.GT, 5), userId));
            assertTrue(evaluator.evaluate(condition("posts", Operator.LT, 6), userId));
            assertTrue(evaluator.evaluate(condition("posts", Operator.LTE, 5), userId));
            assertFalse(evaluator.evaluate(condition("posts", Operator.LTE, 4), userId));
        }

        @Test
        @DisplayName("should return false for an unknown metric")
        void shouldReturnFalseForUnknownMetric() {
            assertFalse(evaluator.evaluate(condition("nonexistent", Operator.GTE, 1), userId));
        }
    }

    @Nested
    @DisplayName("logical combinators")
    class LogicTests {
        @Test
        @DisplayName("should evaluate AND")
        void shouldEvaluateAnd() {
            PredicateNode and = new AndNode(List.of(
                new ConditionNode("posts", Operator.GTE, 5),
                new ConditionNode("comments", Operator.GTE, 3)
            ));
            assertTrue(evaluator.evaluate(and, userId));

            PredicateNode andFalse = new AndNode(List.of(
                new ConditionNode("posts", Operator.GTE, 5),
                new ConditionNode("comments", Operator.GTE, 4)
            ));
            assertFalse(evaluator.evaluate(andFalse, userId));
        }

        @Test
        @DisplayName("should evaluate OR")
        void shouldEvaluateOr() {
            PredicateNode or = new OrNode(List.of(
                new ConditionNode("posts", Operator.GTE, 100),
                new ConditionNode("comments", Operator.GTE, 3)
            ));
            assertTrue(evaluator.evaluate(or, userId));

            PredicateNode orFalse = new OrNode(List.of(
                new ConditionNode("posts", Operator.GTE, 100),
                new ConditionNode("comments", Operator.GTE, 100)
            ));
            assertFalse(evaluator.evaluate(orFalse, userId));
        }

        @Test
        @DisplayName("should evaluate NOT")
        void shouldEvaluateNot() {
            assertFalse(evaluator.evaluate(new NotNode(new ConditionNode("posts", Operator.GTE, 5)), userId));
            assertTrue(evaluator.evaluate(new NotNode(new ConditionNode("posts", Operator.GTE, 100)), userId));
        }

        @Test
        @DisplayName("should evaluate a nested tree")
        void shouldEvaluateNestedTree() {
            PredicateNode tree = new AndNode(List.of(
                new OrNode(List.of(
                    new ConditionNode("poi_visited", Operator.GTE, 2),
                    new ConditionNode("observations", Operator.GTE, 100)
                )),
                new NotNode(new ConditionNode("comments", Operator.GTE, 10))
            ));
            assertTrue(evaluator.evaluate(tree, userId));
        }

        @Test
        @DisplayName("should return true for a null predicate")
        void shouldReturnTrueForNullPredicate() {
            assertTrue(evaluator.evaluate(null, userId));
        }
    }
}