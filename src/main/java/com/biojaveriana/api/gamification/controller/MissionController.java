package com.biojaveriana.api.gamification.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.gamification.dto.MissionCreateRequest;
import com.biojaveriana.api.gamification.dto.MissionListResponse;
import com.biojaveriana.api.gamification.dto.MissionResponse;
import com.biojaveriana.api.gamification.dto.MissionUpdateRequest;
import com.biojaveriana.api.gamification.dto.MyMissionsResponse;
import com.biojaveriana.api.gamification.dto.UserGamificationResponse;
import com.biojaveriana.api.gamification.dto.UserMissionListResponse;
import com.biojaveriana.api.gamification.model.MissionType;
import com.biojaveriana.api.gamification.service.GamificationService;
import com.biojaveriana.api.gamification.service.MissionEngine;
import com.biojaveriana.api.gamification.service.MissionService;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for mission management (admin CRUD + re-evaluation) and the
 * per-user mission board (daily, weekly and event missions).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/missions")
@Validated
@RequiredArgsConstructor
public class MissionController {
    private final MissionService missionService;
    private final MissionEngine missionEngine;
    private final GamificationService gamificationService;

    @PostMapping
    @HasRole(Role.ADMIN)
    public ResponseEntity<MissionResponse> createMission(@RequestBody MissionCreateRequest request) {
        return ResponseEntity.status(201).body(missionService.createMission(request));
    }

    @PutMapping("/{id}")
    @HasRole(Role.ADMIN)
    public ResponseEntity<MissionResponse> updateMission(@PathVariable UUID id, @RequestBody MissionUpdateRequest request) {
        return ResponseEntity.ok(missionService.updateMission(id, request));
    }

    @DeleteMapping("/{id}")
    @HasRole(Role.ADMIN)
    public ResponseEntity<Void> deleteMission(@PathVariable UUID id) {
        missionService.deleteMission(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<MissionListResponse> listMissions(
        @RequestParam(required = false) MissionType type,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var missionPage = missionService.listMissions(type, active, pageable);
        return ResponseEntity.ok(MissionListResponse.from(missionPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MissionResponse> getMission(@PathVariable UUID id) {
        return ResponseEntity.ok(missionService.getMission(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<MissionResponse> getMissionBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(missionService.getMissionBySlug(slug));
    }

    /**
     * The user's current mission board: daily, weekly and in-window event
     * missions with live progress.
     */
    @GetMapping("/me")
    public ResponseEntity<MyMissionsResponse> getMyMissions(@CurrentUser User user) {
        return ResponseEntity.ok(missionService.getMyMissions(user.getId()));
    }

    /**
     * Mission history: missions the user has completed.
     */
    @GetMapping("/me/completed")
    public ResponseEntity<UserMissionListResponse> getMyCompletedMissions(
        @CurrentUser User user,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("completedAt").descending());
        var completedPage = missionService.getMyCompletedMissions(user.getId(), pageable);
        return ResponseEntity.ok(UserMissionListResponse.from(completedPage));
    }

    /**
     * Admin: re-evaluates the user's missions (awarding XP for anything newly
     * satisfied) and returns the user's updated gamification summary.
     */
    @PostMapping("/evaluate")
    @HasRole(Role.ADMIN)
    public ResponseEntity<UserGamificationResponse> evaluate(@RequestParam("userId") UUID userId) {
        missionEngine.evaluateUser(userId);
        return ResponseEntity.ok(gamificationService.getUserGamification(userId));
    }
}
