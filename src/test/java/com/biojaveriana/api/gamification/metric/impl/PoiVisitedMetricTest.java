package com.biojaveriana.api.gamification.metric.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.poi.PoiTelemetryRepository;

@ExtendWith(MockitoExtension.class)
class PoiVisitedMetricTest {

    @Mock
    private PoiTelemetryRepository poiTelemetryRepository;

    private PoiVisitedMetric metric;
    private UUID userId;

    @BeforeEach
    void setUp() {
        metric = new PoiVisitedMetric(poiTelemetryRepository);
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("type: reports the poi_visited metric type")
    void type() {
        assertEquals("poi_visited", metric.type());
    }

    @Test
    @DisplayName("value: counts distinct POIs visited by the user")
    void value() {
        when(poiTelemetryRepository.countDistinctPoiByUserId(userId)).thenReturn(14L);

        assertEquals(14L, metric.value(userId));
    }
}
