package com.biojaveriana.api.gamification.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.gamification.dto.AchievementCreateRequest;
import com.biojaveriana.api.gamification.dto.AchievementResponse;
import com.biojaveriana.api.gamification.dto.AchievementUpdateRequest;
import com.biojaveriana.api.gamification.dto.UserAchievementResponse;
import com.biojaveriana.api.gamification.model.Achievement;
import com.biojaveriana.api.gamification.model.UserAchievement;
import com.biojaveriana.api.gamification.repository.AchievementRepository;
import com.biojaveriana.api.gamification.repository.UserAchievementRepository;
import com.biojaveriana.api.gamification.exception.NotFoundException;
import com.biojaveriana.api.gamification.exception.ValidationException;

@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    public AchievementService(
        AchievementRepository achievementRepository,
        UserAchievementRepository userAchievementRepository
    ) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
    }

    @Transactional
    public AchievementResponse createAchievement(AchievementCreateRequest request) {
        if (achievementRepository.findBySlug(request.slug()).isPresent()) {
            throw new ValidationException("Achievement with slug '" + request.slug() + "' already exists");
        }
        validateAchievementRequest(request);

        Achievement achievement = Achievement.builder()
            .slug(request.slug())
            .name(request.name())
            .description(request.description())
            .xpReward(request.xpReward())
            .eventType(request.eventType())
            .predicateJson(request.predicateJson())
            .active(true)
            .build();

        achievement = achievementRepository.save(achievement);
        return AchievementResponse.from(achievement);
    }

    @Transactional
    public AchievementResponse updateAchievement(UUID id, AchievementUpdateRequest request) {
        Achievement achievement = achievementRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Achievement not found: " + id));

        if (request.name() != null) achievement.setName(request.name());
        if (request.description() != null) achievement.setDescription(request.description());
        if (request.xpReward() != null) achievement.setXpReward(request.xpReward());
        if (request.eventType() != null) achievement.setEventType(request.eventType());
        if (request.predicateJson() != null) achievement.setPredicateJson(request.predicateJson());
        if (request.active() != null) achievement.setActive(request.active());

        achievement = achievementRepository.save(achievement);
        return AchievementResponse.from(achievement);
    }

    @Transactional
    public void deleteAchievement(UUID id) {
        if (!achievementRepository.existsById(id)) {
            throw new NotFoundException("Achievement not found: " + id);
        }
        achievementRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public AchievementResponse getAchievement(UUID id) {
        Achievement achievement = achievementRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Achievement not found: " + id));
        return AchievementResponse.from(achievement);
    }

    @Transactional(readOnly = true)
    public AchievementResponse getAchievementBySlug(String slug) {
        Achievement achievement = achievementRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Achievement not found: " + slug));
        return AchievementResponse.from(achievement);
    }
    @Transactional(readOnly = true)
    public List<AchievementResponse> listAchievements(Boolean active) {
        List<Achievement> achievements;
        if (active != null) {
            achievements = achievementRepository.findByActiveOrderByCreatedAtDesc(active);
        } else {
            achievements = achievementRepository.findAll();
        }
        return achievements.stream().map(AchievementResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<UserAchievementResponse> getUserAchievements(UUID userId) {
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(userId);
        return userAchievements.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserAchievementResponse> getCompletedAchievements(UUID userId) {
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserIdAndCompletedTrue(userId);
        return userAchievements.stream().map(this::toResponse).toList();
    }

    private void validateAchievementRequest(AchievementCreateRequest request) {
        if (request.slug() == null || request.slug().isBlank()) {
            throw new ValidationException("Slug is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Name is required");
        }
        if (request.xpReward() == null || request.xpReward() <= 0) {
            throw new ValidationException("XP reward must be positive");
        }
        if (request.eventType() == null || request.eventType().isBlank()) {
            throw new ValidationException("Event type is required");
        }
    }

    private UserAchievementResponse toResponse(UserAchievement ua) {
        Achievement a = ua.getAchievement();
        return UserAchievementResponse.from(
            a.getId(), a.getSlug(), a.getName(), a.getDescription(),
            a.getXpReward(), ua.getCompleted(), ua.getCompletedAt(),
            ua.getXpAwarded(), ua.getProgress(), null
        );
    }
}
