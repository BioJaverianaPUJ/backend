package com.biojaveriana.api.gamification.metric.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.observation.ObservationRepository;

@ExtendWith(MockitoExtension.class)
class SpeciesIdentifiedMetricTest {

    @Mock
    private ObservationRepository observationRepository;

    private SpeciesIdentifiedMetric metric;
    private UUID userId;
    private Instant since;

    @BeforeEach
    void setUp() {
        metric = new SpeciesIdentifiedMetric(observationRepository);
        userId = UUID.randomUUID();
        since = Instant.now();
    }

    @Test
    @DisplayName("type: reports the species_identified metric type")
    void type() {
        assertEquals("species_identified", metric.type());
    }

    @Test
    @DisplayName("value: counts distinct species identified by the user")
    void value() {
        when(observationRepository.countDistinctSpeciesByUserId(userId)).thenReturn(21L);

        assertEquals(21L, metric.value(userId));
    }

    @Test
    @DisplayName("valueSince: counts only species identified since the given instant")
    void valueSince() {
        when(observationRepository.countDistinctSpeciesByUserIdSince(userId, since)).thenReturn(8L);

        assertEquals(8L, metric.valueSince(userId, since));
    }
}
