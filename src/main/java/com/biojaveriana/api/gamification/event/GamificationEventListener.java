package com.biojaveriana.api.gamification.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.biojaveriana.api.gamification.model.Achievement;
import com.biojaveriana.api.gamification.model.UserAchievement;
import com.biojaveriana.api.gamification.predicate.LeafPredicate;
import com.biojaveriana.api.gamification.predicate.MapContext;
import com.biojaveriana.api.gamification.predicate.Predicate;
import com.biojaveriana.api.gamification.predicate.PredicateEvaluator;
import com.biojaveriana.api.gamification.predicate.PredicateDeserializer;
import com.biojaveriana.api.gamification.repository.AchievementRepository;
import com.biojaveriana.api.gamification.repository.UserAchievementRepository;
import com.biojaveriana.api.gamification.repository.UserGamificationRepository;
import com.biojaveriana.api.gamification.service.GamificationService;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Listens for gamification events and processes achievements.
 * Uses @TransactionalEventListener to ensure data consistency.
 */
@Component
public class GamificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(GamificationEventListener.class);

    private final GamificationService gamificationService;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final PredicateEvaluator predicateEvaluator;

    public GamificationEventListener(
        GamificationService gamificationService,
        AchievementRepository achievementRepository,
        UserAchievementRepository userAchievementRepository,
        UserGamificationRepository userGamificationRepository,
        PredicateEvaluator predicateEvaluator
    ) {
        this.gamificationService = gamificationService;
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.userGamificationRepository = userGamificationRepository;
        this.predicateEvaluator = predicateEvaluator;
    }

    @TransactionalEventListener
    public void handlePostCreated(PostCreatedEvent event) {
        handleEvent(event);
    }

    @TransactionalEventListener
    public void handleCommentCreated(CommentCreatedEvent event) {
        handleEvent(event);
    }

    @TransactionalEventListener
    public void handleObservationCreated(ObservationCreatedEvent event) {
        handleEvent(event);
    }

    @TransactionalEventListener
    public void handleCommentReactionReceived(CommentReactionReceivedEvent event) {
        handleEvent(event);
    }

    @TransactionalEventListener
    public void handlePoiVisited(PoiVisitedEvent event) {
        handleEvent(event);
    }

    private void handleEvent(GamificationEvent event) {
        String eventType = event.getEventType();
        log.debug("Processing gamification event: {} for user: {}", eventType, event.getUserId());

        List<Achievement> achievements = achievementRepository.findByEventTypeAndActiveTrue(eventType);
        if (achievements.isEmpty()) {
            return;
        }

        for (Achievement achievement : achievements) {
            processAchievement(event, achievement);
        }
    }

    private void processAchievement(GamificationEvent event, Achievement achievement) {
        UUID userId = event.getUserId();
        UUID achievementId = achievement.getId();

        // Check if already completed
        userAchievementRepository.findByUserIdAndAchievementId(userId, achievementId)
            .ifPresent(existing -> {
                if (Boolean.TRUE.equals(existing.getCompleted())) {
                    return;
                }
            });

        // Build context for predicate evaluation
        Map<String, Object> context = buildContext(event);

        // Evaluate predicate
        String predicateJson = achievement.getPredicateJson();
        boolean satisfied = predicateEvaluator.evaluate(predicateJson, new MapContext(context));

        if (!satisfied) {
            return;
        }

        // Award XP
        gamificationService.awardXp(userId, achievement.getXpReward(), "ACHIEVEMENT: " + achievement.getName());

        // Create or update UserAchievement
        UserAchievement userAchievement = userAchievementRepository
            .findByUserIdAndAchievementId(userId, achievementId)
            .orElse(UserAchievement.builder()
                .userId(userId)
                .achievement(achievement)
                .progress(0)
                .completed(false)
                .xpAwarded(0L)
                .build());

        userAchievement.setCompleted(true);
        userAchievement.setCompletedAt(Instant.now());
        userAchievement.setXpAwarded((long) achievement.getXpReward());
        userAchievement.setProgress(1);

        userAchievementRepository.save(userAchievement);

        log.info("Awarded achievement '{}' to user '{}'", achievement.getName(), userId);
    }

    private Map<String, Object> buildContext(GamificationEvent event) {
        Map<String, Object> context = event.getAdditionalContext();
        context.put("user_id", event.getUserId());
        context.put("event_type", event.getEventType());
        return context;
    }
}
