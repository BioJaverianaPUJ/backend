package com.biojaveriana.api.gamification.predicate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.biojaveriana.api.gamification.dto.PredicateProgress;

class PredicateProgressCalculatorTest {

    private final PredicateProgressCalculator calculator = new PredicateProgressCalculator();

    private ConditionNode condition(String metric, String op, long value) {
        return new ConditionNode(metric, Operator.parse(op), value);
    }

    @Test
    @DisplayName("satisfied condition reports current, target and operator")
    void satisfiedCondition() {
        PredicateProgress progress = calculator.calculate(condition("posts", "gte", 5), Map.of("posts", 7L));
        assertTrue(progress.satisfied());
        assertEquals("CONDITION", progress.type());
        assertEquals("posts", progress.metric());
        assertEquals(7L, progress.current());
        assertEquals(5L, progress.target());
        assertEquals("gte", progress.operator());
    }

    @Test
    @DisplayName("unsatisfied condition reports satisfied=false with live current value")
    void unsatisfiedCondition() {
        PredicateProgress progress = calculator.calculate(condition("posts", "gte", 10), Map.of("posts", 3L));
        assertFalse(progress.satisfied());
        assertEquals(3L, progress.current());
        assertEquals(10L, progress.target());
    }

    @Test
    @DisplayName("unknown or unavailable metric is reported as unsatisfied with null current")
    void unknownMetric() {
        PredicateProgress progress = calculator.calculate(condition("nope", "gte", 1), Map.of());
        assertFalse(progress.satisfied());
        assertNull(progress.current());
        assertEquals(1L, progress.target());
    }

    @Test
    @DisplayName("AND aggregates met and total children")
    void andAggregation() {
        AndNode and = new AndNode(List.of(
            condition("posts", "gte", 5),
            condition("comments", "gte", 2)
        ));
        PredicateProgress progress = calculator.calculate(and, Map.of("posts", 5L, "comments", 1L));
        assertEquals("AND", progress.type());
        assertFalse(progress.satisfied());
        assertEquals(1, progress.metChildren());
        assertEquals(2, progress.totalChildren());
        assertEquals(2, progress.children().size());
    }

    @Test
    @DisplayName("OR is satisfied when any child is satisfied")
    void orAggregation() {
        OrNode or = new OrNode(List.of(
            condition("posts", "gte", 99),
            condition("comments", "gte", 2)
        ));
        PredicateProgress progress = calculator.calculate(or, Map.of("posts", 1L, "comments", 3L));
        assertTrue(progress.satisfied());
        assertEquals(1, progress.metChildren());
        assertEquals(2, progress.totalChildren());
    }

    @Test
    @DisplayName("NOT inverts its child")
    void notInversion() {
        NotNode not = new NotNode(condition("posts", "gte", 5));
        PredicateProgress progress = calculator.calculate(not, Map.of("posts", 1L));
        assertTrue(progress.satisfied());
        assertNotNull(progress.child());
        assertFalse(progress.child().satisfied());
    }

    @Test
    @DisplayName("referencedMetrics collects all metric types in the tree (normalized)")
    void referencedMetrics() {
        AndNode and = new AndNode(List.of(
            condition("Posts", "gte", 5),
            new OrNode(List.of(condition("comments", "gte", 1), new NotNode(condition("posts", "lt", 2))))
        ));
        Set<String> acc = new HashSet<>();
        calculator.referencedMetrics(and, acc);
        assertEquals(Set.of("posts", "comments"), acc);
    }
}
