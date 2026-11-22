package com.biojaveriana.api.gamification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.biojaveriana.api.gamification.model.Mission;
import com.biojaveriana.api.gamification.model.MissionType;

public interface MissionRepository extends JpaRepository<Mission, UUID> {
    Optional<Mission> findBySlug(String slug);
    List<Mission> findByTypeAndActiveTrue(MissionType type);
    Page<Mission> findByActiveTrue(Pageable pageable);
    Page<Mission> findByType(MissionType type, Pageable pageable);
    Page<Mission> findByTypeAndActiveTrue(MissionType type, Pageable pageable);
}
