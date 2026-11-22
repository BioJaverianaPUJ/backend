package com.biojaveriana.api.gamification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import com.biojaveriana.api.gamification.metric.MetricRegistry;
import com.biojaveriana.api.gamification.metric.WindowedMetric;
import com.biojaveriana.api.gamification.model.Mission;
import com.biojaveriana.api.gamification.model.MissionStatus;
import com.biojaveriana.api.gamification.model.MissionType;
import com.biojaveriana.api.gamification.model.UserMission;
import com.biojaveriana.api.gamification.predicate.PredicateCodec;
import com.biojaveriana.api.gamification.predicate.PredicateEvaluator;
import com.biojaveriana.api.gamification.repository.MissionRepository;
import com.biojaveriana.api.gamification.repository.UserMissionRepository;

import static com.biojaveriana.api.gamification.model.MissionType.DAILY;
import static com.biojaveriana.api.gamification.model.MissionType.EVENT;

@ExtendWith(MockitoExtension.class)
class MissionEngineTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private UserMissionRepository userMissionRepository;

    @Mock
    private MissionAssignmentService assignmentService;

    @Mock
    private GamificationService gamificationService;

    private MissionEngine engine;
    private final UUID userId = UUID.randomUUID();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private WindowedMetric windowedPostsMetric(long valueSince) {
        return new WindowedMetric() {
            @Override
            public String type() {
                return "posts";
            }

            @Override
            public Long value(UUID id) {
                return valueSince;
            }

            @Override
            public Long valueSince(UUID id, Instant since) {
                return valueSince;
            }
        };
    }

    private WindowedMetric brokenMetric() {
        return new WindowedMetric() {
            @Override
            public String type() {
                return "broken";
            }

            @Override
            public Long value(UUID id) {
                throw new IllegalStateException("metric query failed");
            }

            @Override
            public Long valueSince(UUID id, Instant since) {
                throw new IllegalStateException("metric query failed");
            }
        };
    }

    private MissionEngine engineWith(MetricRegistry registry) {
        PredicateCodec codec = new PredicateCodec(jsonMapper);
        PredicateEvaluator evaluator = new PredicateEvaluator(registry);
        return new MissionEngine(missionRepository, userMissionRepository, assignmentService,
            gamificationService, evaluator, codec, jsonMapper);
    }

    @BeforeEach
    void setUp() {
        engine = engineWith(new MetricRegistry(List.of(windowedPostsMetric(3L))));
        // evaluateUser always re-checks the active user missions and in-window events.
        lenient().when(userMissionRepository.findByUserIdAndStatus(userId, MissionStatus.ACTIVE)).thenReturn(List.of());
        lenient().when(missionRepository.findByTypeAndActiveTrue(EVENT)).thenReturn(List.of());
    }

    private Mission mission(MissionType type, String slug, int xp, String predicateJson, Instant start, Instant end) {
        return Mission.builder()
            .id(UUID.randomUUID())
            .slug(slug)
            .name(slug)
            .xpReward(xp)
            .type(type)
            .predicateJson(predicateJson)
            .startTime(start)
            .endTime(end)
            .active(true)
            .build();
    }

    private UserMission userMission(Mission mission, Instant periodStart, Instant periodEnd) {
        return UserMission.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .mission(mission)
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .status(MissionStatus.ACTIVE)
            .build();
    }

    @Nested
    @DisplayName("assigned missions")
    class AssignedMissionTests {

        private final Instant periodStart = Instant.now().minus(1, ChronoUnit.HOURS);
        private final Instant periodEnd = Instant.now().plus(1, ChronoUnit.HOURS);

        @Test
        @DisplayName("awards XP and completes the mission when the windowed predicate is satisfied")
        void shouldAwardXpWhenPredicateSatisfiedWithinWindow() {
            Mission m = mission(DAILY, "daily-posts", 40, "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}", null, null);
            when(userMissionRepository.findByUserIdAndStatus(userId, MissionStatus.ACTIVE))
                .thenReturn(List.of(userMission(m, periodStart, periodEnd)));

            engine.evaluateUser(userId);

            verify(gamificationService).awardXp(userId, 40L, "MISSION:daily-posts");
            ArgumentCaptor<UserMission> captor = ArgumentCaptor.forClass(UserMission.class);
            verify(userMissionRepository).save(captor.capture());
            UserMission saved = captor.getValue();
            assertEquals(MissionStatus.COMPLETED, saved.getStatus());
            assertEquals(40L, saved.getXpAwarded());
            assertNotNull(saved.getCompletedAt());
        }

        @Test
        @DisplayName("does not award when the predicate is not satisfied within the window")
        void shouldNotAwardWhenPredicateNotSatisfied() {
            Mission m = mission(DAILY, "daily-posts", 40, "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":5}", null, null);
            when(userMissionRepository.findByUserIdAndStatus(userId, MissionStatus.ACTIVE))
                .thenReturn(List.of(userMission(m, periodStart, periodEnd)));

            engine.evaluateUser(userId);

            verify(gamificationService, never()).awardXp(any(), any(), any());
            verify(userMissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("marks the mission expired without awarding XP once the period has passed")
        void shouldExpireMissionsPastTheirPeriodEnd() {
            Instant start = Instant.now().minus(2, ChronoUnit.DAYS);
            Instant end = Instant.now().minus(1, ChronoUnit.DAYS);
            Mission m = mission(DAILY, "daily-posts", 40, "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}", null, null);
            when(userMissionRepository.findByUserIdAndStatus(userId, MissionStatus.ACTIVE))
                .thenReturn(List.of(userMission(m, start, end)));

            engine.evaluateUser(userId);

            verify(gamificationService, never()).awardXp(any(), any(), any());
            ArgumentCaptor<UserMission> captor = ArgumentCaptor.forClass(UserMission.class);
            verify(userMissionRepository).save(captor.capture());
            assertEquals(MissionStatus.EXPIRED, captor.getValue().getStatus());
            assertEquals(0L, captor.getValue().getXpAwarded());
        }

        @Test
        @DisplayName("skips missions with an unreadable predicate")
        void shouldSkipMissionsWithUnreadablePredicate() {
            Mission m = mission(DAILY, "daily-posts", 40, "{not-json", null, null);
            when(userMissionRepository.findByUserIdAndStatus(userId, MissionStatus.ACTIVE))
                .thenReturn(List.of(userMission(m, periodStart, periodEnd)));

            engine.evaluateUser(userId);

            verify(gamificationService, never()).awardXp(any(), any(), any());
            verify(userMissionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("event missions")
    class EventTests {

        private final Instant now = Instant.now();

        @Test
        @DisplayName("completes an in-window event mission and awards XP exactly once")
        void shouldCompleteEventMissionInWindow() {
            Mission e = mission(EVENT, "spring-migration", 75,
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}",
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
            when(missionRepository.findByTypeAndActiveTrue(EVENT)).thenReturn(List.of(e));
            when(userMissionRepository.existsByUserIdAndMissionIdAndPeriodStart(userId, e.getId(), e.getStartTime()))
                .thenReturn(false);

            engine.evaluateUser(userId);

            verify(gamificationService).awardXp(userId, 75L, "MISSION:spring-migration");
            ArgumentCaptor<UserMission> captor = ArgumentCaptor.forClass(UserMission.class);
            verify(userMissionRepository).save(captor.capture());
            UserMission saved = captor.getValue();
            assertEquals(MissionStatus.COMPLETED, saved.getStatus());
            assertEquals(e.getStartTime(), saved.getPeriodStart());
            assertEquals(e.getEndTime(), saved.getPeriodEnd());
            assertEquals(75L, saved.getXpAwarded());
        }

        @Test
        @DisplayName("does not award an event mission the user already completed")
        void shouldNotAwardEventMissionTwice() {
            Mission e = mission(EVENT, "spring-migration", 75,
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}",
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
            when(missionRepository.findByTypeAndActiveTrue(EVENT)).thenReturn(List.of(e));
            when(userMissionRepository.existsByUserIdAndMissionIdAndPeriodStart(userId, e.getId(), e.getStartTime()))
                .thenReturn(true);

            engine.evaluateUser(userId);

            verify(gamificationService, never()).awardXp(any(), any(), any());
            verify(userMissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("ignores event missions outside their window")
        void shouldIgnoreEventMissionOutsideWindow() {
            Mission e = mission(EVENT, "spring-migration", 75,
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}",
                now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));
            when(missionRepository.findByTypeAndActiveTrue(EVENT)).thenReturn(List.of(e));

            engine.evaluateUser(userId);

            verify(gamificationService, never()).awardXp(any(), any(), any());
            verify(userMissionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("a failing mission does not block the others")
    void shouldNotBlockOtherMissionsWhenOneFails() {
        MissionEngine localEngine = engineWith(new MetricRegistry(List.of(windowedPostsMetric(3L), brokenMetric())));
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
        Mission broken = mission(DAILY, "broken", 10, "{\"metric\":\"broken\",\"op\":\"gte\",\"value\":1}", start, end);
        Mission good = mission(DAILY, "good", 20, "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}", start, end);
        when(userMissionRepository.findByUserIdAndStatus(userId, MissionStatus.ACTIVE))
            .thenReturn(List.of(userMission(broken, start, end), userMission(good, start, end)));

        localEngine.evaluateUser(userId);

        verify(gamificationService).awardXp(userId, 20L, "MISSION:good");
        verify(userMissionRepository, times(1)).save(any());
    }
}
