package com.biojaveriana.api.gamification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.biojaveriana.api.gamification.model.UserAchievement;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {
    Optional<UserAchievement> findByUserIdAndAchievementId(UUID userId, UUID achievementId);
    boolean existsByUserIdAndAchievementId(UUID userId, UUID achievementId);
    List<UserAchievement> findByUserId(UUID userId);
    List<UserAchievement> findByUserIdAndCompletedTrue(UUID userId);

    @Modifying
    @Query("DELETE FROM UserAchievement ua WHERE ua.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}