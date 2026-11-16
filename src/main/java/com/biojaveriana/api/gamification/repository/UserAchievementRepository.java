package com.biojaveriana.api.gamification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.biojaveriana.api.gamification.model.UserAchievement;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {
    Optional<UserAchievement> findByUserIdAndAchievementId(UUID userId, UUID achievementId);
    List<UserAchievement> findByUserId(UUID userId);
    List<UserAchievement> findByUserIdAndCompletedTrue(UUID userId);
}
