package com.biojaveriana.api.gamification.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.gamification.config.GamificationConfig;
import com.biojaveriana.api.gamification.model.Mission;
import com.biojaveriana.api.gamification.model.MissionStatus;
import com.biojaveriana.api.gamification.model.MissionType;
import com.biojaveriana.api.gamification.model.UserMission;
import com.biojaveriana.api.gamification.repository.MissionRepository;
import com.biojaveriana.api.gamification.repository.UserMissionRepository;

/**
 * Assigns a user's daily and weekly missions for the current periods.
 *
 * <p>Assignments are created lazily and idempotently: within a period, the
 * same user always receives the same random selection (the draw is seeded by
 * user id + period start), so repeated calls are cheap and repeat-safe.
 */
@Service
public class MissionAssignmentService {
    private static final Logger log = LoggerFactory.getLogger(MissionAssignmentService.class);

    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final GamificationConfig gamificationConfig;

    public MissionAssignmentService(
        MissionRepository missionRepository,
        UserMissionRepository userMissionRepository,
        GamificationConfig gamificationConfig
    ) {
        this.missionRepository = missionRepository;
        this.userMissionRepository = userMissionRepository;
        this.gamificationConfig = gamificationConfig;
    }

    /**
     * Ensures the user has their daily and weekly mission assignments for the
     * current day/week in the configured timezone. Existing assignments are
     * left untouched; if a previous run only managed to create part of the
     * expected assignments, the remaining slots are filled.
     */
    @Transactional
    public void ensureAssignments(UUID userId) {
        ZoneId zone = gamificationConfig.getMissionTimezone();
        LocalDate today = LocalDate.now(zone);

        Instant dayStart = today.atStartOfDay(zone).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();
        ensurePeriodAssignments(userId, MissionType.DAILY, dayStart, dayEnd,
            gamificationConfig.getDailyMissionCount());

        Instant weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant();
        Instant weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant();
        ensurePeriodAssignments(userId, MissionType.WEEKLY, weekStart, weekEnd,
            gamificationConfig.getWeeklyMissionCount());
    }

    // Package-private for testing with explicit periods.
    void ensurePeriodAssignments(UUID userId, MissionType type, Instant periodStart, Instant periodEnd, int count) {
        if (count <= 0) {
            return;
        }
        // The period start of a WEEKLY window can equal the start of the DAILY
        // window for the same day (the week starts on Monday), so restrict the
        // "existing" check to assignments of the same mission type.
        List<UserMission> existing = userMissionRepository.findByUserIdAndPeriodStart(userId, periodStart).stream()
            .filter(um -> um.getMission().getType() == type)
            .toList();
        int remaining = count - existing.size();
        if (remaining <= 0) {
            return;
        }
        Set<UUID> assignedIds = existing.stream()
            .map(um -> um.getMission().getId())
            .collect(Collectors.toSet());
        List<Mission> pool = missionRepository.findByTypeAndActiveTrue(type).stream()
            .filter(mission -> !assignedIds.contains(mission.getId()))
            .toList();
        if (pool.isEmpty()) {
            return;
        }

        Random random = new Random(Objects.hash(userId, periodStart.toEpochMilli()));
        List<Mission> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        List<Mission> selected = shuffled.stream().limit(remaining).toList();

        for (Mission mission : selected) {
            UserMission userMission = UserMission.builder()
                .userId(userId)
                .mission(mission)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .status(MissionStatus.ACTIVE)
                .build();
            userMissionRepository.save(userMission);
            log.info("Assigned {} mission '{}' to user '{}'", type, mission.getSlug(), userId);
        }
    }
}
