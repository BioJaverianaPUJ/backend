package com.biojaveriana.api.gamification.metric.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.metric.Metric;
import com.biojaveriana.api.gamification.metric.WindowedMetric;
import com.biojaveriana.api.observation.ObservationRepository;

/**
 * Number of distinct species the user has identified through their observations.
 */
@Component
public class SpeciesIdentifiedMetric implements WindowedMetric {
    public static final String TYPE = "species_identified";

    private final ObservationRepository observationRepository;

    public SpeciesIdentifiedMetric(ObservationRepository observationRepository) {
        this.observationRepository = observationRepository;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Long value(UUID userId) {
        return observationRepository.countDistinctSpeciesByUserId(userId);
    }

    @Override
    public Long valueSince(UUID userId, Instant since) {
        return observationRepository.countDistinctSpeciesByUserIdSince(userId, since);
    }
}