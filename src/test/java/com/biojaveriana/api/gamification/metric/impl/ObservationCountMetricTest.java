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
class ObservationCountMetricTest {

    @Mock
    private ObservationRepository observationRepository;

    private ObservationCountMetric metric;
    private UUID userId;
    private Instant since;

    @BeforeEach
    void setUp() {
        metric = new ObservationCountMetric(observationRepository);
        userId = UUID.randomUUID();
        since = Instant.now();
    }

    @Test
    @DisplayName("type: reports the observations metric type")
    void type() {
        assertEquals("observations", metric.type());
    }

    @Test
    @DisplayName("value: counts all observations of the user")
    void value() {
        when(observationRepository.countByUserId(userId)).thenReturn(9L);

        assertEquals(9L, metric.value(userId));
    }

    @Test
    @DisplayName("valueSince: counts only observations created since the given instant")
    void valueSince() {
        when(observationRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, since)).thenReturn(4L);

        assertEquals(4L, metric.valueSince(userId, since));
    }
}
