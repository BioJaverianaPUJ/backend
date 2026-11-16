package com.biojaveriana.api.poi;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoiRepository extends JpaRepository<Poi, UUID> {
    Page<Poi> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Poi> findByActiveTrueAndVisibilityInOrderByCreatedAtDesc(
        List<PoiVisibility> visibility, Pageable pageable);

    List<Poi> findByActiveTrueAndVisibilityIn(List<PoiVisibility> visibility);

    List<Poi> findByActiveTrueAndVisibilityInAndGeometryType(
        List<PoiVisibility> visibility, PoiType geometryType);
}
