package com.biojaveriana.api.gamification.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.biojaveriana.api.gamification.dto.AchievementCreateRequest;
import com.biojaveriana.api.gamification.dto.AchievementProgressResponse;
import com.biojaveriana.api.gamification.dto.AchievementPreviewRequest;
import com.biojaveriana.api.gamification.dto.AchievementPreviewResponse;
import com.biojaveriana.api.gamification.dto.AchievementResponse;
import com.biojaveriana.api.gamification.dto.AchievementUpdateRequest;
import com.biojaveriana.api.gamification.dto.PredicateProgress;
import com.biojaveriana.api.gamification.dto.UserAchievementResponse;
import com.biojaveriana.api.gamification.exception.NotFoundException;
import com.biojaveriana.api.gamification.exception.ValidationException;
import com.biojaveriana.api.gamification.metric.MetricRegistry;
import com.biojaveriana.api.gamification.model.Achievement;
import com.biojaveriana.api.gamification.model.UserAchievement;
import com.biojaveriana.api.gamification.predicate.AndNode;
import com.biojaveriana.api.gamification.predicate.ConditionNode;
import com.biojaveriana.api.gamification.predicate.NotNode;
import com.biojaveriana.api.gamification.predicate.OrNode;
import com.biojaveriana.api.gamification.predicate.PredicateCodec;
import com.biojaveriana.api.gamification.predicate.PredicateEvaluator;
import com.biojaveriana.api.gamification.predicate.PredicateNode;
import com.biojaveriana.api.gamification.predicate.PredicateProgressCalculator;
import com.biojaveriana.api.gamification.repository.AchievementRepository;
import com.biojaveriana.api.gamification.repository.UserAchievementRepository;

@Service
public class AchievementService {

    private static final Logger log = LoggerFactory.getLogger(AchievementService.class);
    private static final int MAX_PREDICATE_DEPTH = 16;

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final PredicateCodec predicateCodec;
    private final MetricRegistry metricRegistry;
    private final PredicateEvaluator predicateEvaluator;
    private final JsonMapper jsonMapper;
    private final PredicateProgressCalculator progressCalculator;

    public AchievementService(
        AchievementRepository achievementRepository,
        UserAchievementRepository userAchievementRepository,
        PredicateCodec predicateCodec,
        MetricRegistry metricRegistry,
        PredicateEvaluator predicateEvaluator,
        JsonMapper jsonMapper,
        PredicateProgressCalculator progressCalculator
    ) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.predicateCodec = predicateCodec;
        this.metricRegistry = metricRegistry;
        this.predicateEvaluator = predicateEvaluator;
        this.jsonMapper = jsonMapper;
        this.progressCalculator = progressCalculator;
    }

    @Transactional
    public AchievementResponse createAchievement(AchievementCreateRequest request) {
        if (request.slug() == null || request.slug().isBlank()) {
            throw new ValidationException("Slug is required");
        }
        if (achievementRepository.findBySlug(request.slug()).isPresent()) {
            throw new ValidationException("Achievement with slug '" + request.slug() + "' already exists");
        }
        validateBase(request.name(), request.xpReward());
        PredicateNode predicate = parseAndValidate(request.predicate());

        Achievement achievement = Achievement.builder()
            .slug(request.slug())
            .name(request.name())
            .description(request.description())
            .xpReward(request.xpReward())
            .predicateJson(predicateCodec.toNode(predicate).toString())
            .active(true)
            .build();

        achievement = achievementRepository.save(achievement);
        return AchievementResponse.from(achievement, jsonMapper);
    }

    @Transactional
    public AchievementResponse updateAchievement(UUID id, AchievementUpdateRequest request) {
        Achievement achievement = achievementRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Achievement not found: " + id));

        if (request.name() != null) {
            if (request.name().isBlank()) throw new ValidationException("Name cannot be blank");
            achievement.setName(request.name());
        }
        if (request.description() != null) achievement.setDescription(request.description());
        if (request.xpReward() != null) {
            if (request.xpReward() <= 0) throw new ValidationException("XP reward must be positive");
            achievement.setXpReward(request.xpReward());
        }
        if (request.active() != null) achievement.setActive(request.active());
        if (request.predicate() != null) {
            PredicateNode predicate = parseAndValidate(request.predicate());
            achievement.setPredicateJson(predicateCodec.toNode(predicate).toString());
        }

        achievement = achievementRepository.save(achievement);
        return AchievementResponse.from(achievement, jsonMapper);
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
        return AchievementResponse.from(achievement, jsonMapper);
    }

    @Transactional(readOnly = true)
    public AchievementResponse getAchievementBySlug(String slug) {
        Achievement achievement = achievementRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Achievement not found: " + slug));
        return AchievementResponse.from(achievement, jsonMapper);
    }

    @Transactional(readOnly = true)
    public Page<AchievementResponse> listAchievements(Boolean active, Pageable pageable) {
        Page<Achievement> page = active != null
            ? achievementRepository.findByActiveOrderByCreatedAtDesc(active, pageable)
            : achievementRepository.findAll(pageable);
        return page.map(a -> AchievementResponse.from(a, jsonMapper));
    }

    /**
     * Progression view for a user: every active achievement, with the user's
     * completion state and live progress (current metric values vs the
     * predicate targets). Each distinct metric is fetched at most once per
     * user. Achievements whose stored predicate cannot be read are listed
     * with a null progress.
     */
    @Transactional(readOnly = true)
    public Page<AchievementProgressResponse> getUserAchievementProgress(UUID userId, Pageable pageable) {
        Page<Achievement> achievementsPage = achievementRepository.findByActiveTrue(pageable);
        List<Achievement> achievements = achievementsPage.getContent();

        Map<UUID, UserAchievement> completedByAchievement = userAchievementRepository.findByUserId(userId).stream()
            .filter(ua -> Boolean.TRUE.equals(ua.getCompleted()))
            .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), ua -> ua, (a, b) -> a));

        Map<String, Long> metricValues = new HashMap<>();
        for (Achievement achievement : achievements) {
            PredicateNode predicate = readPredicate(achievement);
            if (predicate == null) {
                continue;
            }
            for (String type : progressCalculator.referencedMetrics(predicate, new HashSet<>())) {
                metricRegistry.get(type).ifPresent(metric ->
                    metricValues.putIfAbsent(metric.type().toLowerCase(Locale.ROOT), metric.value(userId)));
            }
        }

        return achievementsPage.map(achievement -> {
            UserAchievement ua = completedByAchievement.get(achievement.getId());
            PredicateProgress progress = null;
            PredicateNode predicate = readPredicate(achievement);
            if (predicate != null) {
                progress = progressCalculator.calculate(predicate, metricValues);
            }
            return new AchievementProgressResponse(
                achievement.getId(), achievement.getSlug(), achievement.getName(), achievement.getDescription(),
                achievement.getXpReward(),
                ua != null,
                ua != null ? ua.getCompletedAt() : null,
                ua != null ? ua.getXpAwarded() : null,
                progress
            );
        });
    }

    @Transactional(readOnly = true)
    public Page<UserAchievementResponse> getCompletedAchievements(UUID userId, Pageable pageable) {
        return userAchievementRepository.findByUserIdAndCompletedTrue(userId, pageable)
            .map(this::toResponse);
    }

    /**
     * Admin dry-run: evaluates a candidate predicate against a user's current
     * metrics without awarding anything.
     */
    @Transactional(readOnly = true)
    public AchievementPreviewResponse preview(AchievementPreviewRequest request) {
        PredicateNode predicate = parseAndValidate(request.predicate());
        Map<String, Long> values = collectMetricValues(predicate, request.userId(), new HashMap<>());
        boolean satisfied = predicateEvaluator.evaluate(predicate, request.userId());
        return new AchievementPreviewResponse(satisfied, values);
    }

    private void validateBase(String name, Integer xpReward) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Name is required");
        }
        if (xpReward == null || xpReward <= 0) {
            throw new ValidationException("XP reward must be positive");
        }
    }

    private PredicateNode parseAndValidate(JsonNode predicate) {
        if (predicate == null) {
            throw new ValidationException("Predicate is required");
        }
        PredicateNode node = predicateCodec.fromNode(predicate);
        assertMetricsKnown(node, 0);
        return node;
    }

    private void assertMetricsKnown(PredicateNode node, int depth) {
        if (depth > MAX_PREDICATE_DEPTH) {
            throw new ValidationException("Predicate is too deeply nested");
        }
        switch (node) {
            case AndNode and -> and.children().forEach(child -> assertMetricsKnown(child, depth + 1));
            case OrNode or -> or.children().forEach(child -> assertMetricsKnown(child, depth + 1));
            case NotNode not -> assertMetricsKnown(not.child(), depth + 1);
            case ConditionNode condition -> {
                if (metricRegistry.get(condition.metric()).isEmpty()) {
                    throw new ValidationException("Unknown metric: " + condition.metric());
                }
            }
        }
    }

    private Map<String, Long> collectMetricValues(PredicateNode node, UUID userId, Map<String, Long> acc) {
        switch (node) {
            case AndNode and -> and.children().forEach(child -> collectMetricValues(child, userId, acc));
            case OrNode or -> or.children().forEach(child -> collectMetricValues(child, userId, acc));
            case NotNode not -> collectMetricValues(not.child(), userId, acc);
            case ConditionNode condition -> metricRegistry.get(condition.metric()).ifPresent(metric ->
                acc.putIfAbsent(metric.type().toLowerCase(), metric.value(userId)));
        }
        return acc;
    }

    private PredicateNode readPredicate(Achievement achievement) {
        if (achievement.getPredicateJson() == null) {
            return null;
        }
        try {
            JsonNode node = jsonMapper.readTree(achievement.getPredicateJson());
            return predicateCodec.fromNode(node);
        } catch (Exception e) {
            log.warn("Skipping unreadable predicate for achievement '{}': {}", achievement.getId(), e.getMessage());
            return null;
        }
    }

    private UserAchievementResponse toResponse(UserAchievement ua) {
        Achievement a = ua.getAchievement();
        return new UserAchievementResponse(
            a.getId(), a.getSlug(), a.getName(), a.getDescription(),
            a.getXpReward(), ua.getCompleted(), ua.getCompletedAt(), ua.getXpAwarded()
        );
    }
}
