package com.biojaveriana.api.gamification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
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

import com.biojaveriana.api.gamification.config.GamificationConfig;
import com.biojaveriana.api.gamification.model.Mission;
import com.biojaveriana.api.gamification.model.MissionStatus;
import com.biojaveriana.api.gamification.model.MissionType;
import com.biojaveriana.api.gamification.model.UserMission;
import com.biojaveriana.api.gamification.repository.MissionRepository;
import com.biojaveriana.api.gamification.repository.UserMissionRepository;

import static com.biojaveriana.api.gamification.model.MissionType.DAILY;
import static com.biojaveriana.api.gamification.model.MissionType.WEEKLY;

@ExtendWith(MockitoExtension.class)
class MissionAssignmentServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private UserMissionRepository userMissionRepository;

    @Mock
    private GamificationConfig gamificationConfig;

    private MissionAssignmentService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MissionAssignmentService(missionRepository, userMissionRepository, gamificationConfig);
        lenient().when(gamificationConfig.getMissionTimezone()).thenReturn(ZoneId.of("UTC"));
        lenient().when(gamificationConfig.getDailyMissionCount()).thenReturn(2);
        lenient().when(gamificationConfig.getWeeklyMissionCount()).thenReturn(1);
    }

    private Mission mission(MissionType type, String slug) {
        return Mission.builder()
            .id(UUID.randomUUID())
            .slug(slug)
            .name(slug)
            .xpReward(10)
            .type(type)
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
    @DisplayName("ensureAssignments")
    class EnsureAssignmentsTests {

        @Test
        @DisplayName("assigns the configured number of daily and weekly missions when none exist")
        void shouldAssignDailyAndWeeklyWhenNoneExist() {
            when(userMissionRepository.findByUserIdAndPeriodStart(eq(userId), any(Instant.class))).thenReturn(List.of());
            when(missionRepository.findByTypeAndActiveTrue(DAILY))
                .thenReturn(List.of(mission(DAILY, "d1"), mission(DAILY, "d2"), mission(DAILY, "d3")));
            when(missionRepository.findByTypeAndActiveTrue(WEEKLY))
                .thenReturn(List.of(mission(WEEKLY, "w1"), mission(WEEKLY, "w2")));

            service.ensureAssignments(userId);

            ArgumentCaptor<UserMission> captor = ArgumentCaptor.forClass(UserMission.class);
            verify(userMissionRepository, times(3)).save(captor.capture());
            List<MissionType> types = captor.getAllValues().stream().map(um -> um.getMission().getType()).toList();
            assertEquals(2, types.stream().filter(DAILY::equals).count());
            assertEquals(1, types.stream().filter(WEEKLY::equals).count());
            for (UserMission um : captor.getAllValues()) {
                assertEquals(userId, um.getUserId());
                assertEquals(MissionStatus.ACTIVE, um.getStatus());
                assertNotNull(um.getPeriodStart());
                assertTrue(um.getPeriodStart().isBefore(um.getPeriodEnd()));
            }
        }

        @Test
        @DisplayName("is idempotent: does not assign again once the period is fully assigned")
        void shouldNotAssignAgainWhenAlreadyFullyAssigned() {
            Instant now = Instant.now();
            List<UserMission> existing = List.of(
                userMission(mission(DAILY, "d1"), now, now.plus(Duration.ofDays(1))),
                userMission(mission(DAILY, "d2"), now, now.plus(Duration.ofDays(1))),
                userMission(mission(WEEKLY, "w1"), now, now.plus(Duration.ofDays(7)))
            );
            when(userMissionRepository.findByUserIdAndPeriodStart(eq(userId), any(Instant.class)))
                .thenReturn(existing);

            service.ensureAssignments(userId);

            verify(userMissionRepository, never()).save(any());
        }
        @Test
        @DisplayName("tops up a period that was only partially assigned")
        void shouldTopUpPartiallyAssignedPeriod() {
            Instant now = Instant.now();
            Mission d1 = mission(DAILY, "d1");
            when(userMissionRepository.findByUserIdAndPeriodStart(eq(userId), any(Instant.class)))
                .thenReturn(List.of(userMission(d1, now, now.plus(Duration.ofDays(1)))));
            when(missionRepository.findByTypeAndActiveTrue(DAILY))
                .thenReturn(List.of(mission(DAILY, "d2"), mission(DAILY, "d3")));
            when(missionRepository.findByTypeAndActiveTrue(WEEKLY))
                .thenReturn(List.of(mission(WEEKLY, "w1")));

            service.ensureAssignments(userId);

            ArgumentCaptor<UserMission> captor = ArgumentCaptor.forClass(UserMission.class);
            verify(userMissionRepository, times(2)).save(captor.capture());
            List<MissionType> types = captor.getAllValues().stream().map(um -> um.getMission().getType()).toList();
            assertEquals(1, types.stream().filter(DAILY::equals).count());
            assertEquals(1, types.stream().filter(WEEKLY::equals).count());
            // The already-assigned mission is not selected again
            for (UserMission um : captor.getAllValues()) {
                assertNotEquals(d1.getId(), um.getMission().getId());
            }
        }
    }

    @Nested
    @DisplayName("ensurePeriodAssignments")
    class PeriodTests {

        @Test
        @DisplayName("does not assign when the configured count is zero")
        void shouldNotAssignWhenCountIsZero() {
            Instant start = Instant.parse("2026-11-23T00:00:00Z");

            service.ensurePeriodAssignments(userId, DAILY, start, start.plus(Duration.ofDays(1)), 0);

            verifyNoInteractions(missionRepository, userMissionRepository);
        }

        @Test
        @DisplayName("ignores existing weekly assignments when assigning dailies for a week-start day")
        void shouldIgnoreWeeklyAssignmentsWhenAssigningDailies() {
            // A Monday: the daily period and the weekly period share the same start.
            Instant mondayStart = Instant.parse("2026-11-23T00:00:00Z");
            Instant mondayEnd = mondayStart.plus(Duration.ofDays(1));
            UserMission weekly = userMission(mission(WEEKLY, "w1"), mondayStart, mondayStart.plus(Duration.ofDays(7)));

            when(userMissionRepository.findByUserIdAndPeriodStart(userId, mondayStart)).thenReturn(List.of(weekly));
            when(missionRepository.findByTypeAndActiveTrue(DAILY)).thenReturn(List.of(
                mission(DAILY, "d1"), mission(DAILY, "d2"), mission(DAILY, "d3")));

            service.ensurePeriodAssignments(userId, DAILY, mondayStart, mondayEnd, 2);

            ArgumentCaptor<UserMission> captor = ArgumentCaptor.forClass(UserMission.class);
            verify(userMissionRepository, times(2)).save(captor.capture());
            assertTrue(captor.getAllValues().stream()
                .allMatch(um -> um.getMission().getType() == DAILY));
        }

        @Test
        @DisplayName("assigns at most as many missions as are available")
        void shouldNotAssignMoreThanThePoolAllows() {
            Instant start = Instant.parse("2026-11-23T00:00:00Z");
            when(userMissionRepository.findByUserIdAndPeriodStart(userId, start)).thenReturn(List.of());
            when(missionRepository.findByTypeAndActiveTrue(DAILY)).thenReturn(List.of(mission(DAILY, "d1")));

            service.ensurePeriodAssignments(userId, DAILY, start, start.plus(Duration.ofDays(1)), 2);

            verify(userMissionRepository, times(1)).save(any());
        }
    }
}

