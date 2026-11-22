package com.biojaveriana.api.gamification.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.gamification.model.Achievement;
import com.biojaveriana.api.gamification.model.UserAchievement;
import com.biojaveriana.api.gamification.predicate.PredicateCodec;
import com.biojaveriana.api.gamification.predicate.PredicateEvaluator;
import com.biojaveriana.api.gamification.predicate.PredicateNode;
import com.biojaveriana.api.gamification.repository.AchievementRepository;
import com.biojaveriana.api.gamification.repository.UserAchievementRepository;

/**
 * Evaluates active achievements against a user's metrics whenever a
 * gamification event fires, awarding XP exactly once per achievement per user.
 */
@Service
public class AchievementEngine {
    private static final Logger log = LoggerFactory.getLogger(AchievementEngine.class);

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final GamificationService gamificationService;
    private final PredicateEvaluator predicateEvaluator;
    private final PredicateCodec predicateCodec;
    private final JsonMapper jsonMapper;

    public AchievementEngine(
        AchievementRepository achievementRepository,
        UserAchievementRepository userAchievementRepository,
        GamificationService gamificationService,
        PredicateEvaluator predicateEvaluator,
        PredicateCodec predicateCodec,
        JsonMapper jsonMapper
    ) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.gamificationService = gamificationService;
        this.predicateEvaluator = predicateEvaluator;
        this.predicateCodec = predicateCodec;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Re-evaluates every active achievement for the affected user and awards XP
     * for any newly-satisfied achievement. Per-user processing is serialized
     * via a pessimistic lock to prevent duplicate awards under concurrency.
     *
     * <p>{@code REQUIRES_NEW} is required: this runs from an
     * {@code @TransactionalEventListener} after the triggering transaction has
     * already committed, at which point {@code REQUIRED} would "join" the dead
     * transaction context instead of starting a real one.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateUser(UUID userId) {
        log.debug("Evaluating active achievements for user '{}'", userId);
        gamificationService.getOrCreateUserGamificationWithLock(userId);

        List<Achievement> candidates = achievementRepository.findByActiveTrue();
        for (Achievement achievement : candidates) {
            processAchievement(userId, achievement);
        }
    }

    private void processAchievement(UUID userId, Achievement achievement) {
        try {
            doProcessAchievement(userId, achievement);
        } catch (Exception e) {
            log.error("Failed to process achievement '{}' for user '{}': {}",
                achievement.getId(), userId, e.getMessage(), e);
        }
    }

    private void doProcessAchievement(UUID userId, Achievement achievement) {
        if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId())) {
            return;
        }

        PredicateNode predicate = readPredicate(achievement);
        if (predicate == null) {
            return;
        }
        if (!predicateEvaluator.evaluate(predicate, userId)) {
            return;
        }

        long xp = achievement.getXpReward().longValue();
        gamificationService.awardXp(userId, xp, "ACHIEVEMENT:" + achievement.getSlug());

        UserAchievement userAchievement = UserAchievement.builder()
            .userId(userId)
            .achievement(achievement)
            .completed(true)
            .completedAt(Instant.now())
            .xpAwarded(xp)
            .build();
        userAchievementRepository.save(userAchievement);

        log.info("Awarded achievement '{}' ({} xp) to user '{}'", achievement.getName(), xp, userId);
    }

    private PredicateNode readPredicate(Achievement achievement) {
        try {
            JsonNode node = jsonMapper.readTree(achievement.getPredicateJson());
            return predicateCodec.fromNode(node);
        } catch (Exception e) {
            log.error("Failed to read predicate for achievement '{}': {}", achievement.getId(), e.getMessage());
            return null;
        }
    }
}