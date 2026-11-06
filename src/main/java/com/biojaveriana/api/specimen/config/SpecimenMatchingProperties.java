package com.biojaveriana.api.specimen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.specimen-matching")
public class SpecimenMatchingProperties {
    /** Whether automatic specimen matching is enabled */
    private boolean enabled = false;

    /** Minimum classification certainty (0.0–1.0) to consider a match */
    private double minCertainty = 0.70;

    /** Maximum Haversine distance in meters for a valid match */
    private double maxDistanceMeters = 5000.0;
}
