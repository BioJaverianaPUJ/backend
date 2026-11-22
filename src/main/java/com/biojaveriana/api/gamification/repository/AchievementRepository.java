package com.biojaveriana.api.gamification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.biojaveriana.api.gamification.model.Achievement;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    Optional<Achievement> findBySlug(String slug);
    List<Achievement> findByActiveTrue();
    Page<Achievement> findByActiveTrue(Pageable pageable);
    List<Achievement> findByActiveOrderByCreatedAtDesc(Boolean active);
    Page<Achievement> findByActiveOrderByCreatedAtDesc(Boolean active, Pageable pageable);
}