package com.biojaveriana.api.poi;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PoiTelemetryRepository extends JpaRepository<PoiTelemetry, UUID> {
    @Query("SELECT t FROM PoiTelemetry t WHERE t.user.id = :userId AND t.enteredAt > :after")
    List<PoiTelemetry> findByUserIdAndEnteredAfter(@Param("userId") UUID userId, @Param("after") Instant after);

    @Query("SELECT t FROM PoiTelemetry t WHERE t.poi.id = :poiId AND t.user.id = :userId AND t.enteredAt > :after")
    Optional<PoiTelemetry> findByPoiIdAndUserIdAndEnteredAfter(
        @Param("poiId") UUID poiId,
        @Param("userId") UUID userId,
        @Param("after") Instant after
    );
}
