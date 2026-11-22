package com.biojaveriana.api.gamification.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.biojaveriana.api.gamification.model.MissionStatus;
import com.biojaveriana.api.gamification.model.UserMission;

public interface UserMissionRepository extends JpaRepository<UserMission, UUID> {
    List<UserMission> findByUserIdAndPeriodStart(UUID userId, Instant periodStart);
    List<UserMission> findByUserIdAndPeriodEndGreaterThanEqual(UUID userId, Instant periodEnd);
    List<UserMission> findByUserIdAndStatus(UUID userId, MissionStatus status);
    Page<UserMission> findByUserIdAndStatusOrderByCompletedAtDesc(UUID userId, MissionStatus status, Pageable pageable);
    boolean existsByUserIdAndMissionIdAndPeriodStart(UUID userId, UUID missionId, Instant periodStart);

    @Modifying
    @Query("DELETE FROM UserMission um WHERE um.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserMission um WHERE um.mission.id = :missionId")
    void deleteByMissionId(@Param("missionId") UUID missionId);
}
