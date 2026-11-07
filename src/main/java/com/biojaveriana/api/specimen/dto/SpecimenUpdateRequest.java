package com.biojaveriana.api.specimen.dto;

import com.biojaveriana.api.specimen.HealthStatus;
import java.time.Instant;

public record SpecimenUpdateRequest(
    Double latitude,
    Double longitude,
    Double circumference,
    Double height,
    HealthStatus healthStatus,
    String notes,
    Instant observationDate
) {}
