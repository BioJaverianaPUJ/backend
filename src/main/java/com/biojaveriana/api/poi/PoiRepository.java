package com.biojaveriana.api.poi;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PoiRepository extends JpaRepository<Poi, UUID> {
    Page<Poi> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Poi> findByActiveTrueAndVisibilityInOrderByCreatedAtDesc(List<PoiVisibility> visibility, Pageable pageable);

    @Query("SELECT p FROM Poi p WHERE p.active = true AND p.visibility IN :visibility " +
           "AND ST_Contains(ST_GeomFromGeoJSON(:polygon), ST_MakePoint(:longitude, :latitude)) = true")
    List<Poi> findPoisContainingPoint(
        @Param("visibility") List<PoiVisibility> visibility,
        @Param("polygon") String polygon,
        @Param("longitude") Double longitude,
        @Param("latitude") Double latitude
    );

    @Query("SELECT p FROM Poi p WHERE p.active = true AND p.visibility IN :visibility " +
           "AND p.geometryType = 'POINT' AND " +
           "ST_DWithin(ST_MakePoint(p.center_longitude, p.center_latitude), ST_MakePoint(:longitude, :latitude), :radiusMeters)")
    List<Poi> findPointPoisNearLocation(
        @Param("visibility") List<PoiVisibility> visibility,
        @Param("longitude") Double longitude,
        @Param("latitude") Double latitude,
        @Param("radiusMeters") Double radiusMeters
    );
}
