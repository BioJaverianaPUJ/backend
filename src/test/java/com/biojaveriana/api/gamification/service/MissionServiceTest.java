package com.biojaveriana.api.gamification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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

import com.biojaveriana.api.gamification.dto.MissionCreateRequest;
import com.biojaveriana.api.gamification.dto.MissionResponse;
import com.biojaveriana.api.gamification.dto.MissionUpdateRequest;
import com.biojaveriana.api.gamification.dto.MyMissionsResponse;
import com.biojaveriana.api.gamification.dto.PredicateProgress;
import com.biojaveriana.api.gamification.dto.UserMissionResponse;
import com.biojaveriana.api.gamification.exception.NotFoundException;
import com.biojaveriana.api.gamification.exception.ValidationException;
import com.biojaveriana.api.gamification.metric.MetricRegistry;
import com.biojaveriana.api.gamification.metric.WindowedMetric;
import com.biojaveriana.api.gamification.model.Mission;
import com.biojaveriana.api.gamification.model.MissionStatus;
import com.biojaveriana.api.gamification.model.MissionType;
import com.biojaveriana.api.gamification.model.UserMission;
import com.biojaveriana.api.gamification.predicate.PredicateCodec;
import com.biojaveriana.api.gamification.predicate.PredicateProgressCalculator;
import com.biojaveriana.api.gamification.repository.MissionRepository;
import com.biojaveriana.api.gamification.repository.UserMissionRepository;

import static com.biojaveriana.api.gamification.model.MissionType.DAILY;
import static com.biojaveriana.api.gamification.model.MissionType.EVENT;
import static com.biojaveriana.api.gamification.model.MissionType.WEEKLY;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private UserMissionRepository userMissionRepository;

    @Mock
    private MetricRegistry metricRegistry;

    @Mock
    private MissionAssignmentService assignmentService;

    private MissionService service;
    private final UUID userId = UUID.randomUUID();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        service = new MissionService(missionRepository, userMissionRepository,
            new PredicateCodec(jsonMapper), metricRegistry, jsonMapper,
            new PredicateProgressCalculator(), assignmentService);
    }

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

    private MissionCreateRequest createRequest(
        String slug, MissionType type, int xp, Instant start, Instant end, String predicateJson
    ) {
        return new MissionCreateRequest(
            slug, slug, "description", xp, type, start, end,
            predicateJson == null ? null : jsonMapper.readTree(predicateJson)
        );
    }
    @Nested
    @DisplayName("createMission")
    class CreateTests {

        @Test
        @DisplayName("creates an event mission with a validated predicate")
        void shouldCreateEventMission() {
            MissionCreateRequest request = createRequest("spring-migration", EVENT, 75,
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS),
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}");
            when(missionRepository.findBySlug("spring-migration")).thenReturn(Optional.empty());
            when(metricRegistry.isWindowed("posts")).thenReturn(true);
            when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

            MissionResponse response = service.createMission(request);

            assertEquals("spring-migration", response.slug());
            assertEquals(EVENT, response.type());
            assertEquals(75, response.xpReward());
            ArgumentCaptor<Mission> captor = ArgumentCaptor.forClass(Mission.class);
            verify(missionRepository).save(captor.capture());
            assertTrue(captor.getValue().getActive());
            assertTrue(captor.getValue().getPredicateJson().contains("posts"));
        }

        @Test
        @DisplayName("rejects a duplicate slug")
        void shouldRejectDuplicateSlug() {
            MissionCreateRequest request = createRequest("existing", EVENT, 10,
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS),
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}");
            when(missionRepository.findBySlug("existing"))
                .thenReturn(Optional.of(mission(EVENT, "existing", 10, null, null, null)));

            assertThrows(ValidationException.class, () -> service.createMission(request));
            verify(missionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects an event mission without a window")
        void shouldRejectEventWithoutWindow() {
            MissionCreateRequest request = createRequest("spring-migration", EVENT, 10, null, null,
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}");

            assertThrows(ValidationException.class, () -> service.createMission(request));
            verify(missionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects an event mission whose start is not before its end")
        void shouldRejectEventWithStartAfterEnd() {
            MissionCreateRequest request = createRequest("spring-migration", EVENT, 10,
                Instant.now().plus(1, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS),
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}");

            assertThrows(ValidationException.class, () -> service.createMission(request));
            verify(missionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a daily mission with an explicit window")
        void shouldRejectDailyWithExplicitWindow() {
            MissionCreateRequest request = createRequest("daily-posts", DAILY, 10,
                Instant.now().minus(1, ChronoUnit.HOURS), null,
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}");

            assertThrows(ValidationException.class, () -> service.createMission(request));
            verify(missionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects predicates that reference non-windowed metrics")
        void shouldRejectNonWindowedMetricPredicate() {
            MissionCreateRequest request = createRequest("spring-migration", EVENT, 10,
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS),
                "{\"metric\":\"poi_visited\",\"op\":\"gte\",\"value\":1}");
            when(missionRepository.findBySlug("spring-migration")).thenReturn(Optional.empty());
            when(metricRegistry.isWindowed("poi_visited")).thenReturn(false);

            assertThrows(ValidationException.class, () -> service.createMission(request));
            verify(missionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a non-positive XP reward")
        void shouldRejectNonPositiveXpReward() {
            MissionCreateRequest request = createRequest("spring-migration", EVENT, 0,
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS),
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}");
            when(missionRepository.findBySlug("spring-migration")).thenReturn(Optional.empty());

            assertThrows(ValidationException.class, () -> service.createMission(request));
            verify(missionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateMission")
    class UpdateTests {

        @Test
        @DisplayName("updates the XP reward (admin-adjustable)")
        void shouldUpdateXpReward() {
            Mission m = mission(DAILY, "daily-posts", 10, "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":1}", null, null);
            when(missionRepository.findById(m.getId())).thenReturn(Optional.of(m));
            when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

            MissionResponse response = service.updateMission(m.getId(),
                new MissionUpdateRequest(null, null, 50, null, null, null, null, null));

            assertEquals(50, response.xpReward());
        }

        @Test
        @DisplayName("rejects a non-positive XP reward")
        void shouldRejectNonPositiveXpReward() {
            Mission m = mission(DAILY, "daily-posts", 10, "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":1}", null, null);
            when(missionRepository.findById(m.getId())).thenReturn(Optional.of(m));

            assertThrows(ValidationException.class, () -> service.updateMission(m.getId(),
                new MissionUpdateRequest(null, null, 0, null, null, null, null, null)));
        }

        @Test
        @DisplayName("re-validates type and window after merging the update")
        void shouldRejectTypeChangeToEventWithoutWindow() {
            Mission m = mission(DAILY, "daily-posts", 10, "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":1}", null, null);
            when(missionRepository.findById(m.getId())).thenReturn(Optional.of(m));

            assertThrows(ValidationException.class, () -> service.updateMission(m.getId(),
                new MissionUpdateRequest(null, null, null, EVENT, null, null, null, null)));
            verify(missionRepository, never()).save(any());
        }

        @Test
        @DisplayName("replaces the predicate when provided")
        void shouldReplacePredicate() {
            Mission m = mission(DAILY, "daily-posts", 10, "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":1}", null, null);
            when(missionRepository.findById(m.getId())).thenReturn(Optional.of(m));
            when(metricRegistry.isWindowed("comments")).thenReturn(true);
            when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

            MissionResponse response = service.updateMission(m.getId(),
                new MissionUpdateRequest(null, null, null, null, null, null,
                    jsonMapper.readTree("{\"metric\":\"comments\",\"op\":\"gte\",\"value\":2}"), null));

            assertTrue(response.predicate().toString().contains("comments"));
        }

        @Test
        @DisplayName("throws when the mission does not exist")
        void shouldThrowWhenMissionNotFound() {
            UUID id = UUID.randomUUID();
            when(missionRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.updateMission(id,
                new MissionUpdateRequest(null, null, 50, null, null, null, null, null)));
        }
    }

    @Nested
    @DisplayName("deleteMission")
    class DeleteTests {

        @Test
        @DisplayName("deletes the mission and all of its user assignments")
        void shouldDeleteMissionAndAssignments() {
            UUID id = UUID.randomUUID();
            when(missionRepository.existsById(id)).thenReturn(true);

            service.deleteMission(id);

            verify(userMissionRepository).deleteByMissionId(id);
            verify(missionRepository).deleteById(id);
        }

        @Test
        @DisplayName("throws when the mission does not exist")
        void shouldThrowWhenMissionNotFound() {
            UUID id = UUID.randomUUID();
            when(missionRepository.existsById(id)).thenReturn(false);

            assertThrows(NotFoundException.class, () -> service.deleteMission(id));
            verify(missionRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getMyMissions")
    class BoardTests {

        private static final String PREDICATE = "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":3}";

        @Test
        @DisplayName("groups the board by type, includes only in-window events and reports live progress")
        void shouldGroupMissionsByTypeAndComputeProgress() {
            Instant now = Instant.now();
            String predicate = PREDICATE;
            Mission daily = mission(DAILY, "d1", 10, predicate, null, null);
            Mission weekly = mission(WEEKLY, "w1", 20, predicate, null, null);
            UserMission dailyUm = userMission(daily, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
            UserMission weeklyUm = userMission(weekly, now.minus(3, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS));
            when(userMissionRepository.findByUserIdAndPeriodEndGreaterThanEqual(eq(userId), any(Instant.class)))
                .thenReturn(List.of(dailyUm, weeklyUm));

            Mission inWindowEvent = mission(EVENT, "ev-in", 30, predicate,
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
            Mission endedEvent = mission(EVENT, "ev-out", 40, predicate,
                now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));
            when(missionRepository.findByTypeAndActiveTrue(EVENT)).thenReturn(List.of(inWindowEvent, endedEvent));
            when(metricRegistry.getWindowed("posts")).thenReturn(Optional.of(windowedPostsMetric(3L)));

            MyMissionsResponse response = service.getMyMissions(userId);

            assertEquals(1, response.daily().size());
            assertEquals(1, response.weekly().size());
            assertEquals(1, response.event().size());
            assertEquals("ev-in", response.event().get(0).slug());

            UserMissionResponse dailyResponse = response.daily().get(0);
            assertEquals("d1", dailyResponse.slug());
            assertEquals(MissionStatus.ACTIVE, dailyResponse.status());
            assertNull(dailyResponse.completedAt());
            PredicateProgress progress = dailyResponse.progress();
            assertEquals("CONDITION", progress.type());
            assertEquals("posts", progress.metric());
            assertEquals(3L, progress.current());
            assertTrue(progress.satisfied());

            verify(assignmentService).ensureAssignments(userId);
        }
    }
}

