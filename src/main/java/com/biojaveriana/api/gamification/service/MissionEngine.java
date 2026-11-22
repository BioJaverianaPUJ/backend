package com.biojaveriana.api.gamification.service;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.gamification.model.Mission;
import com.biojaveriana.api.gamification.model.MissionStatus;
import com.biojaveriana.api.gamification.model.MissionType;
import com.biojaveriana.api.gamification.model.UserMission;
import com.biojaveriana.api.gamification.predicate.PredicateCodec;
import com.biojaveriana.api.gamification.predicate.PredicateEvaluator;
import com.biojaveriana.api.gamification.predicate.PredicateNode;
import com.biojaveriana.api.gamification.repository.MissionRepository;
import com.biojaveriana.api.gamification.repository.UserMissionRepository;

/**
 * Evaluates a user's active missions (daily, weekly and event) against their
 * windowed metrics whenever a gamification event fires, awarding XP exactly
 * once per mission per period.
 */
@Service
public class MissionEngine {
    private static final Logger log = LoggerFactory.getLogger(MissionEngine.class);

    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final MissionAssignmentService assignmentService;
    private final GamificationService gamificationService;
    private final PredicateEvaluator predicateEvaluator;
    private final PredicateCodec predicateCodec;
    private final JsonMapper jsonMapper;

    public MissionEngine(
        MissionRepository missionRepository,
        UserMissionRepository userMissionRepository,
        MissionAssignmentService assignmentService,
        GamificationService gamificationService,
        PredicateEvaluator predicateEvaluator,
        PredicateCodec predicateCodec,
        JsonMapper jsonMapper
    ) {
        this.missionRepository = missionRepository;
        this.userMissionRepository = userMissionRepository;
        this.assignmentService = assignmentService;
        this.gamificationService = gamificationService;
        this.predicateEvaluator = predicateEvaluator;
        this.predicateCodec = predicateCodec;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Re-evaluates the user's missions and awards XP for anything newly
     * satisfied. Per-user processing is serialized via a pessimistic lock on
     * the gamification record (same as the achievement engine).
     *
     * <p>{@code REQUIRES_NEW} is required: this runs from an
     * {@code @TransactionalEventListener} after the triggering transaction has
     * already committed, at which point {@code REQUIRED} would "join" the dead
     * transaction context instead of starting a real one.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateUser(UUID userId) {
        log.debug("Evaluating active missions for user '{}'", userId);
        gamificationService.getOrCreateUserGamificationWithLock(userId);
        assignmentService.ensureAssignments(userId);

        Instant now = Instant.now();
        for (UserMission userMission : userMissionRepository.findByUserIdAndStatus(userId, MissionStatus.ACTIVE)) {
            processUserMission(userId, userMission, now);
        }
        processEventMissions(userId, now);
    }

    private void processUserMission(UUID userId, UserMission userMission, Instant now) {
        try {
            doProcessUserMission(userId, userMission, now);
        } catch (Exception e) {
            log.error("Failed to process user mission '{}' for user '{}': {}",
                userMission.getId(), userId, e.getMessage(), e);
        }
    }

    private void doProcessUserMission(UUID userId, UserMission userMission, Instant now) {
        if (now.isAfter(userMission.getPeriodEnd())) {
            userMission.setStatus(MissionStatus.EXPIRED);
            userMissionRepository.save(userMission);
            log.info("Expired mission '{}' for user '{}'", userMission.getMission().getSlug(), userId);
            return;
        }

        Mission mission = userMission.getMission();
        PredicateNode predicate = readPredicate(mission);
        if (predicate == null) {
            return;
        }
        if (!predicateEvaluator.evaluate(predicate, userId, userMission.getPeriodStart())) {
            return;
        }

        completeMission(userMission, mission, now, userId);
    }

    private void processEventMissions(UUID userId, Instant now) {
        for (Mission mission : missionRepository.findByTypeAndActiveTrue(MissionType.EVENT)) {
            try {
                doProcessEventMission(userId, mission, now);
            } catch (Exception e) {
                log.error("Failed to process event mission '{}' for user '{}': {}",
                    mission.getId(), userId, e.getMessage(), e);
            }
        }
    }

    private void doProcessEventMission(UUID userId, Mission mission, Instant now) {
        if (mission.getStartTime() == null || mission.getEndTime() == null) {
            return;
        }
        if (now.isBefore(mission.getStartTime()) || now.isAfter(mission.getEndTime())) {
            return;
        }
        if (userMissionRepository.existsByUserIdAndMissionIdAndPeriodStart(userId, mission.getId(), mission.getStartTime())) {
            return;
        }

        PredicateNode predicate = readPredicate(mission);
        if (predicate == null) {
            return;
        }
        if (!predicateEvaluator.evaluate(predicate, userId, mission.getStartTime())) {
            return;
        }

        long xp = mission.getXpReward().longValue();
        gamificationService.awardXp(userId, xp, "MISSION:" + mission.getSlug());

        UserMission userMission = UserMission.builder()
            .userId(userId)
            .mission(mission)
            .periodStart(mission.getStartTime())
            .periodEnd(mission.getEndTime())
            .status(MissionStatus.COMPLETED)
            .completedAt(now)
            .xpAwarded(xp)
            .build();
        userMissionRepository.save(userMission);

        log.info("Awarded event mission '{}' ({} xp) to user '{}'", mission.getName(), xp, userId);
    }

    private void completeMission(UserMission userMission, Mission mission, Instant now, UUID userId) {
        long xp = mission.getXpReward().longValue();
        gamificationService.awardXp(userId, xp, "MISSION:" + mission.getSlug());

        userMission.setStatus(MissionStatus.COMPLETED);
        userMission.setCompletedAt(now);
        userMission.setXpAwarded(xp);
        userMissionRepository.save(userMission);

        log.info("Awarded mission '{}' ({} xp) to user '{}'", mission.getName(), xp, userId);
    }

    private PredicateNode readPredicate(Mission mission) {
        if (mission.getPredicateJson() == null) {
            return null;
        }
        try {
            JsonNode node = jsonMapper.readTree(mission.getPredicateJson());
            return predicateCodec.fromNode(node);
        } catch (Exception e) {
            log.error("Failed to read predicate for mission '{}': {}", mission.getId(), e.getMessage());
            return null;
        }
    }
}
