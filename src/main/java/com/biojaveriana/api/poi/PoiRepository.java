package com.biojaveriana.api.poi;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PoiRepository extends JpaRepository<Poi, UUID> {
    Page<Poi> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Poi> findByActiveTrueAndVisibilityInOrderByCreatedAtDesc(
        List<PoiVisibility> visibility, Pageable pageable);

    List<Poi> findByActiveTrueAndVisibilityIn(List<PoiVisibility> visibility);

    List<Poi> findByActiveTrueAndVisibilityInAndGeometryType(
        List<PoiVisibility> visibility, PoiType geometryType);

    @Query("SELECT p.id FROM Poi p WHERE p.creator.id = :creatorId")
    List<UUID> findIdsByCreatorId(@Param("creatorId") UUID creatorId);

    @Modifying
    @Query("DELETE FROM Poi p WHERE p.creator.id = :creatorId")
    void deleteByCreatorId(@Param("creatorId") UUID creatorId);
}
