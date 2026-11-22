package com.biojaveriana.api.gamification.metric.impl;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.metric.Metric;
import com.biojaveriana.api.poi.PoiTelemetryRepository;

/**
 * Number of distinct POIs the user has visited.
 */
@Component
public class PoiVisitedMetric implements Metric {
    public static final String TYPE = "poi_visited";

    private final PoiTelemetryRepository poiTelemetryRepository;

    public PoiVisitedMetric(PoiTelemetryRepository poiTelemetryRepository) {
        this.poiTelemetryRepository = poiTelemetryRepository;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Long value(UUID userId) {
        return poiTelemetryRepository.countDistinctPoiByUserId(userId);
    }
}