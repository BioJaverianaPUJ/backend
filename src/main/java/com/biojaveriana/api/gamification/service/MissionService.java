package com.biojaveriana.api.gamification.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.biojaveriana.api.gamification.dto.MissionCreateRequest;
import com.biojaveriana.api.gamification.dto.MissionResponse;
import com.biojaveriana.api.gamification.dto.MissionUpdateRequest;
import com.biojaveriana.api.gamification.dto.MyMissionsResponse;
import com.biojaveriana.api.gamification.dto.PredicateProgress;
import com.biojaveriana.api.gamification.dto.UserMissionResponse;
import com.biojaveriana.api.gamification.exception.NotFoundException;
import com.biojaveriana.api.gamification.exception.ValidationException;
import com.biojaveriana.api.gamification.metric.MetricRegistry;
import com.biojaveriana.api.gamification.model.Mission;
import com.biojaveriana.api.gamification.model.MissionStatus;
import com.biojaveriana.api.gamification.model.MissionType;
import com.biojaveriana.api.gamification.model.UserMission;
import com.biojaveriana.api.gamification.predicate.AndNode;
import com.biojaveriana.api.gamification.predicate.ConditionNode;
import com.biojaveriana.api.gamification.predicate.NotNode;
import com.biojaveriana.api.gamification.predicate.OrNode;
import com.biojaveriana.api.gamification.predicate.PredicateCodec;
import com.biojaveriana.api.gamification.predicate.PredicateNode;
import com.biojaveriana.api.gamification.predicate.PredicateProgressCalculator;
import com.biojaveriana.api.gamification.repository.MissionRepository;
import com.biojaveriana.api.gamification.repository.UserMissionRepository;

/**
 * Service for mission management (admin CRUD) and the per-user mission board.
 *
 * <p>Mission predicates must reference windowed metrics only; progress is
 * always computed inside the mission's time window.
 */
@Service
public class MissionService {

    private static final Logger log = LoggerFactory.getLogger(MissionService.class);
    private static final int MAX_PREDICATE_DEPTH = 16;

    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final PredicateCodec predicateCodec;
    private final MetricRegistry metricRegistry;
    private final JsonMapper jsonMapper;
    private final PredicateProgressCalculator progressCalculator;
    private final MissionAssignmentService assignmentService;

    public MissionService(
        MissionRepository missionRepository,
        UserMissionRepository userMissionRepository,
        PredicateCodec predicateCodec,
        MetricRegistry metricRegistry,
        JsonMapper jsonMapper,
        PredicateProgressCalculator progressCalculator,
        MissionAssignmentService assignmentService
    ) {
        this.missionRepository = missionRepository;
        this.userMissionRepository = userMissionRepository;
        this.predicateCodec = predicateCodec;
        this.metricRegistry = metricRegistry;
        this.jsonMapper = jsonMapper;
        this.progressCalculator = progressCalculator;
        this.assignmentService = assignmentService;
    }

    @Transactional
    public MissionResponse createMission(MissionCreateRequest request) {
        if (request.slug() == null || request.slug().isBlank()) {
            throw new ValidationException("Slug is required");
        }
        if (missionRepository.findBySlug(request.slug()).isPresent()) {
            throw new ValidationException("Mission with slug '" + request.slug() + "' already exists");
        }
        validateTypeFields(request.type(), request.startTime(), request.endTime());
        validateBase(request.name(), request.xpReward());
        PredicateNode predicate = parseAndValidate(request.predicate());

        Mission mission = Mission.builder()
            .slug(request.slug())
            .name(request.name())
            .description(request.description())
            .xpReward(request.xpReward())
            .predicateJson(predicateCodec.toNode(predicate).toString())
            .type(request.type())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .active(true)
            .build();

        mission = missionRepository.save(mission);
        return MissionResponse.from(mission, jsonMapper);
    }

    @Transactional
    public MissionResponse updateMission(UUID id, MissionUpdateRequest request) {
        Mission mission = missionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Mission not found: " + id));

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new ValidationException("Name cannot be blank");
            }
            mission.setName(request.name());
        }
        if (request.description() != null) {
            mission.setDescription(request.description());
        }
        if (request.xpReward() != null) {
            if (request.xpReward() <= 0) {
                throw new ValidationException("XP reward must be positive");
            }
            mission.setXpReward(request.xpReward());
        }
        if (request.type() != null) {
            mission.setType(request.type());
        }
        if (request.startTime() != null) {
            mission.setStartTime(request.startTime());
        }
        if (request.endTime() != null) {
            mission.setEndTime(request.endTime());
        }
        if (request.active() != null) {
            mission.setActive(request.active());
        }
        if (request.predicate() != null) {
            PredicateNode predicate = parseAndValidate(request.predicate());
            mission.setPredicateJson(predicateCodec.toNode(predicate).toString());
        }
        validateTypeFields(mission.getType(), mission.getStartTime(), mission.getEndTime());

        mission = missionRepository.save(mission);
        return MissionResponse.from(mission, jsonMapper);
    }

    @Transactional
    public void deleteMission(UUID id) {
        if (!missionRepository.existsById(id)) {
            throw new NotFoundException("Mission not found: " + id);
        }
        userMissionRepository.deleteByMissionId(id);
        missionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public MissionResponse getMission(UUID id) {
        Mission mission = missionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Mission not found: " + id));
        return MissionResponse.from(mission, jsonMapper);
    }

    @Transactional(readOnly = true)
    public MissionResponse getMissionBySlug(String slug) {
        Mission mission = missionRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Mission not found: " + slug));
        return MissionResponse.from(mission, jsonMapper);
    }

    @Transactional(readOnly = true)
    public Page<MissionResponse> listMissions(MissionType type, Boolean active, Pageable pageable) {
        Page<Mission> page;
        if (type != null && Boolean.TRUE.equals(active)) {
            page = missionRepository.findByTypeAndActiveTrue(type, pageable);
        } else if (type != null) {
            page = missionRepository.findByType(type, pageable);
        } else if (Boolean.TRUE.equals(active)) {
            page = missionRepository.findByActiveTrue(pageable);
        } else {
            page = missionRepository.findAll(pageable);
        }
        return page.map(m -> MissionResponse.from(m, jsonMapper));
    }

    /**
     * The user's current mission board: their daily/weekly assignments that
     * are still within their period, plus every in-window event mission (with
     * its completion state). Live progress is computed from windowed metrics,
     * each distinct (metric, window) pair fetched at most once.
     */
    @Transactional
    public MyMissionsResponse getMyMissions(UUID userId) {
        assignmentService.ensureAssignments(userId);
        Instant now = Instant.now();
        Map<String, Long> metricCache = new HashMap<>();

        List<UserMission> assigned = new ArrayList<>(userMissionRepository.findByUserIdAndPeriodEndGreaterThanEqual(userId, now));
        assigned.sort(Comparator.comparing(UserMission::getPeriodStart).reversed());

        List<UserMissionResponse> daily = new ArrayList<>();
        List<UserMissionResponse> weekly = new ArrayList<>();
        List<UserMissionResponse> event = new ArrayList<>();

        for (UserMission userMission : assigned) {
            Mission mission = userMission.getMission();
            UserMissionResponse response = toResponse(userMission, metricCache);
            switch (mission.getType()) {
                case DAILY -> daily.add(response);
                case WEEKLY -> weekly.add(response);
                case EVENT -> event.add(response);
            }
        }

        Set<UUID> shownEventIds = event.stream().map(UserMissionResponse::id).collect(Collectors.toSet());
        for (Mission mission : missionRepository.findByTypeAndActiveTrue(MissionType.EVENT)) {
            if (mission.getStartTime() == null || mission.getEndTime() == null) {
                continue;
            }
            if (now.isBefore(mission.getStartTime()) || now.isAfter(mission.getEndTime())) {
                continue;
            }
            if (shownEventIds.contains(mission.getId())) {
                continue;
            }
            event.add(toEventResponse(mission, userId, metricCache));
        }

        return new MyMissionsResponse(daily, weekly, event);
    }

    @Transactional(readOnly = true)
    public Page<UserMissionResponse> getMyCompletedMissions(UUID userId, Pageable pageable) {
        return userMissionRepository
            .findByUserIdAndStatusOrderByCompletedAtDesc(userId, MissionStatus.COMPLETED, pageable)
            .map(userMission -> toResponse(userMission, new HashMap<>()));
    }

    private void validateBase(String name, Integer xpReward) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Name is required");
        }
        if (xpReward == null || xpReward <= 0) {
            throw new ValidationException("XP reward must be positive");
        }
    }

    private void validateTypeFields(MissionType type, Instant startTime, Instant endTime) {
        if (type == null) {
            throw new ValidationException("Type is required");
        }
        switch (type) {
            case EVENT -> {
                if (startTime == null || endTime == null) {
                    throw new ValidationException("Event missions require startTime and endTime");
                }
                if (!startTime.isBefore(endTime)) {
                    throw new ValidationException("Event mission startTime must be before endTime");
                }
            }
            case DAILY, WEEKLY -> {
                if (startTime != null || endTime != null) {
                    throw new ValidationException(type + " missions must not have startTime/endTime; the period is derived from the assignment");
                }
            }
        }
    }

    private PredicateNode parseAndValidate(JsonNode predicate) {
        if (predicate == null) {
            throw new ValidationException("Predicate is required");
        }
        PredicateNode node = predicateCodec.fromNode(predicate);
        assertMetricsWindowed(node, 0);
        return node;
    }

    /**
     * Mission predicates must reference windowed metrics only, since mission
     * progress is measured inside a time window.
     */
    private void assertMetricsWindowed(PredicateNode node, int depth) {
        if (depth > MAX_PREDICATE_DEPTH) {
            throw new ValidationException("Predicate is too deeply nested");
        }
        switch (node) {
            case AndNode and -> and.children().forEach(child -> assertMetricsWindowed(child, depth + 1));
            case OrNode or -> or.children().forEach(child -> assertMetricsWindowed(child, depth + 1));
            case NotNode not -> assertMetricsWindowed(not.child(), depth + 1);
            case ConditionNode condition -> {
                if (!metricRegistry.isWindowed(condition.metric())) {
                    throw new ValidationException("Metric '" + condition.metric() + "' does not support time windows; missions require windowed metrics");
                }
            }
        }
    }

    private UserMissionResponse toResponse(UserMission userMission, Map<String, Long> metricCache) {
        Mission mission = userMission.getMission();
        return new UserMissionResponse(
            mission.getId(), mission.getSlug(), mission.getName(), mission.getDescription(),
            mission.getXpReward(), mission.getType(),
            userMission.getPeriodStart(), userMission.getPeriodEnd(),
            userMission.getStatus(), userMission.getCompletedAt(), userMission.getXpAwarded(),
            computeProgress(mission, userMission.getUserId(), userMission.getPeriodStart(), metricCache)
        );
    }

    private UserMissionResponse toEventResponse(Mission mission, UUID userId, Map<String, Long> metricCache) {
        return new UserMissionResponse(
            mission.getId(), mission.getSlug(), mission.getName(), mission.getDescription(),
            mission.getXpReward(), mission.getType(),
            mission.getStartTime(), mission.getEndTime(),
            MissionStatus.ACTIVE, null, null,
            computeProgress(mission, userId, mission.getStartTime(), metricCache)
        );
    }

    private PredicateProgress computeProgress(Mission mission, UUID userId, Instant since, Map<String, Long> metricCache) {
        PredicateNode predicate = readPredicate(mission);
        if (predicate == null) {
            return null;
        }
        Map<String, Long> values = new HashMap<>();
        for (String type : progressCalculator.referencedMetrics(predicate, new HashSet<>())) {
            values.put(type, windowedValue(type, userId, since, metricCache));
        }
        return progressCalculator.calculate(predicate, values);
    }

    private PredicateNode readPredicate(Mission mission) {
        if (mission.getPredicateJson() == null) {
            return null;
        }
        try {
            JsonNode node = jsonMapper.readTree(mission.getPredicateJson());
            return predicateCodec.fromNode(node);
        } catch (Exception e) {
            log.warn("Skipping unreadable predicate for mission '{}': {}", mission.getId(), e.getMessage());
            return null;
        }
    }

    private Long windowedValue(String type, UUID userId, Instant since, Map<String, Long> cache) {
        String key = type + "|" + since.toEpochMilli();
        return cache.computeIfAbsent(key, k ->
            metricRegistry.getWindowed(type).map(m -> m.valueSince(userId, since)).orElse(null));
    }
}
