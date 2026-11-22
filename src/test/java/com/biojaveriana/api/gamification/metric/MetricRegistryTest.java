package com.biojaveriana.api.gamification.metric;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetricRegistryTest {

    private MetricRegistry registry;
    private final UUID userId = UUID.randomUUID();

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
        registry = new MetricRegistry(List.of(metric("posts", 5L), metric("comments", 3L)));
    }

    @Test
    @DisplayName("should find a registered metric by type")
    void shouldFindRegisteredMetric() {
        Optional<Metric> found = registry.get("posts");
        assertTrue(found.isPresent());
        assertEquals(5L, found.get().value(userId).longValue());
    }

    @Test
    @DisplayName("should be case-insensitive and trim on lookup")
    void shouldBeCaseInsensitive() {
        assertTrue(registry.get("Posts").isPresent());
        assertTrue(registry.get("  comments ").isPresent());
    }

    @Test
    @DisplayName("should return empty for unknown type")
    void shouldReturnEmptyForUnknownType() {
        assertFalse(registry.get("nope").isPresent());
        assertFalse(registry.get(null).isPresent());
    }
}