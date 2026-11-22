package com.biojaveriana.api.gamification.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import com.biojaveriana.api.gamification.metric.Metric;
import com.biojaveriana.api.gamification.metric.MetricRegistry;
import com.biojaveriana.api.gamification.model.Achievement;
import com.biojaveriana.api.gamification.predicate.PredicateCodec;
import com.biojaveriana.api.gamification.predicate.PredicateEvaluator;
import com.biojaveriana.api.gamification.repository.AchievementRepository;
import com.biojaveriana.api.gamification.repository.UserAchievementRepository;

@ExtendWith(MockitoExtension.class)
class AchievementEngineTest {

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private GamificationService gamificationService;

    private AchievementEngine engine;
    private final UUID userId = UUID.randomUUID();

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private Metric postsMetric() {
        return new Metric() {
            @Override
            public String type() {
                return "posts";
            }

            @Override
            public Long value(UUID id) {
                return 5L;
            }
        };
    }

    private AchievementEngine engineWith(MetricRegistry registry) {
        PredicateCodec codec = new PredicateCodec(jsonMapper);
        PredicateEvaluator evaluator = new PredicateEvaluator(registry);
        return new AchievementEngine(achievementRepository, userAchievementRepository,
            gamificationService, evaluator, codec, jsonMapper);
    }

    @BeforeEach
    void setUp() {
        engine = engineWith(new MetricRegistry(List.of(postsMetric())));
    }

    private Achievement achievement(String predicateJson) {
        return Achievement.builder()
            .id(UUID.randomUUID())
            .slug("first-post")
            .name("First Post")
            .xpReward(50)
            .predicateJson(predicateJson)
            .active(true)
            .build();
    }

    @Test
    @DisplayName("should award XP when satisfied and not yet earned")
    void shouldAwardXpWhenSatisfiedAndNotEarned() {
        Achievement a = achievement("{\"metric\":\"posts\",\"op\":\"gte\",\"value\":5}");
        when(achievementRepository.findByActiveTrue()).thenReturn(List.of(a));
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, a.getId())).thenReturn(false);

        engine.evaluateUser(userId);

        verify(gamificationService).awardXp(userId, 50L, "ACHIEVEMENT:first-post");
        verify(userAchievementRepository).save(any());
    }

    @Test
    @DisplayName("should not award when already earned")
    void shouldNotAwardWhenAlreadyEarned() {
        Achievement a = achievement("{\"metric\":\"posts\",\"op\":\"gte\",\"value\":5}");
        when(achievementRepository.findByActiveTrue()).thenReturn(List.of(a));
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, a.getId())).thenReturn(true);

        engine.evaluateUser(userId);

        verify(gamificationService, never()).awardXp(any(), any(), any());
        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    @DisplayName("should not award when predicate not satisfied")
    void shouldNotAwardWhenNotSatisfied() {
        Achievement a = achievement("{\"metric\":\"posts\",\"op\":\"gte\",\"value\":10}");
        when(achievementRepository.findByActiveTrue()).thenReturn(List.of(a));
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, a.getId())).thenReturn(false);

        engine.evaluateUser(userId);

        verify(gamificationService, never()).awardXp(any(), any(), any());
        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    @DisplayName("a failing achievement does not block the others")
    void shouldNotBlockOtherAchievementsWhenOneFails() {
        Metric throwing = new Metric() {
            @Override
            public String type() {
                return "broken";
            }

            @Override
            public Long value(UUID id) {
                throw new IllegalStateException("metric query failed");
            }
        };
        AchievementEngine localEngine = engineWith(new MetricRegistry(List.of(postsMetric(), throwing)));

        Achievement broken = achievement("{\"metric\":\"broken\",\"op\":\"gte\",\"value\":1}");
        Achievement good = achievement("{\"metric\":\"posts\",\"op\":\"gte\",\"value\":5}");
        when(achievementRepository.findByActiveTrue()).thenReturn(List.of(broken, good));
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, broken.getId())).thenReturn(false);
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, good.getId())).thenReturn(false);

        localEngine.evaluateUser(userId);

        verify(gamificationService).awardXp(userId, 50L, "ACHIEVEMENT:first-post");
        verify(userAchievementRepository, times(1)).save(any());
    }
}