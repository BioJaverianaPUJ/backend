package com.biojaveriana.api.gamification.metric.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.metric.Metric;
import com.biojaveriana.api.gamification.metric.WindowedMetric;
import com.biojaveriana.api.observation.ObservationRepository;

/**
 * Number of observations created by the user.
 */
@Component
public class ObservationCountMetric implements WindowedMetric {
    public static final String TYPE = "observations";

    private final ObservationRepository observationRepository;

    public ObservationCountMetric(ObservationRepository observationRepository) {
        this.observationRepository = observationRepository;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Long value(UUID userId) {
        return observationRepository.countByUserId(userId);
    }

    @Override
    public Long valueSince(UUID userId, Instant since) {
        return observationRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, since);
    }
}