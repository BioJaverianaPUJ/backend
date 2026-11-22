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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Missions", description = "Recurring gamification tasks. DAILY and WEEKLY missions are " +
    "randomly assigned to each user per calendar period; EVENT missions are shared during a fixed " +
    "time window. Admins manage the mission catalogue; users see their assigned mission board.")
public class MissionController {
    private final MissionService missionService;
    private final MissionEngine missionEngine;
    private final GamificationService gamificationService;

    @Operation(summary = "Create a mission (admin)",
        description = "Adds a new mission. `startTime`/`endTime` are required for EVENT missions " +
            "and must be omitted for DAILY/WEEKLY missions (their window comes from the user " +
            "assignment). The `predicate` is a structured logic-block expression over windowed " +
            "metrics. Requires the ADMIN role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Mission created"),
        @ApiResponse(responseCode = "400", description = "Validation failed (bad predicate, missing window, ...)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @PostMapping
    @HasRole(Role.ADMIN)
    public ResponseEntity<MissionResponse> createMission(@RequestBody MissionCreateRequest request) {
        return ResponseEntity.status(201).body(missionService.createMission(request));
    }

    @Operation(summary = "Update a mission (admin)",
        description = "Partially updates a mission; only non-null fields are applied and the " +
            "type, window and predicate are re-validated after the merge. Requires the ADMIN " +
            "role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mission updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed (bad predicate, bad window, ...)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Mission not found"),
    })
    @PutMapping("/{id}")
    @HasRole(Role.ADMIN)
    public ResponseEntity<MissionResponse> updateMission(
            @Parameter(description = "ID of the mission to update", required = true) @PathVariable UUID id,
            @RequestBody MissionUpdateRequest request) {
        return ResponseEntity.ok(missionService.updateMission(id, request));
    }

    @Operation(summary = "Delete a mission (admin)",
        description = "Removes a mission from the catalogue. Requires the ADMIN role " +
            "(SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mission deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Mission not found"),
    })
    @DeleteMapping("/{id}")
    @HasRole(Role.ADMIN)
    public ResponseEntity<Void> deleteMission(
            @Parameter(description = "ID of the mission to delete", required = true) @PathVariable UUID id) {
        missionService.deleteMission(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List missions",
        description = "Paginated list of missions from the catalogue, newest first. Optionally " +
            "filters by type (`DAILY`, `WEEKLY`, `EVENT`) and active state.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of missions"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters or type"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping
    public ResponseEntity<MissionListResponse> listMissions(
        @Parameter(description = "Filter by mission type", required = false) @RequestParam(required = false) MissionType type,
        @Parameter(description = "Filter by active state", required = false) @RequestParam(required = false) Boolean active,
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (1..100)", example = "20")
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var missionPage = missionService.listMissions(type, active, pageable);
        return ResponseEntity.ok(MissionListResponse.from(missionPage));
    }

    @Operation(summary = "Get a mission by ID",
        description = "Fetches a single mission from the catalogue.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The mission"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Mission not found"),
    })
    @GetMapping("/{id}")
    public ResponseEntity<MissionResponse> getMission(
            @Parameter(description = "ID of the mission", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(missionService.getMission(id));
    }

    @Operation(summary = "Get a mission by slug",
        description = "Fetches a single mission by its unique slug.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The mission"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Mission not found"),
    })
    @GetMapping("/slug/{slug}")
    public ResponseEntity<MissionResponse> getMissionBySlug(
            @Parameter(description = "Slug of the mission", required = true) @PathVariable String slug) {
        return ResponseEntity.ok(missionService.getMissionBySlug(slug));
    }

    /**
     * The user's current mission board: daily, weekly and in-window event
     * missions with live progress.
     */
    @Operation(summary = "My mission board",
        description = "The authenticated user's current missions: today's daily, this week's " +
            "weekly and in-window event missions, each with live progress.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The user's mission board"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping("/me")
    public ResponseEntity<MyMissionsResponse> getMyMissions(@Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(missionService.getMyMissions(user.getId()));
    }

    /**
     * Mission history: missions the user has completed.
     */
    @Operation(summary = "My completed missions",
        description = "Mission history: missions the authenticated user has completed, " +
            "newest first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of completed missions"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping("/me/completed")
    public ResponseEntity<UserMissionListResponse> getMyCompletedMissions(
        @Parameter(hidden = true) @CurrentUser User user,
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (1..100)", example = "20")
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
    @Operation(summary = "Force-evaluate a user's missions (admin)",
        description = "Re-evaluates the given user's missions, awarding XP for anything newly " +
            "satisfied, and returns the user's updated gamification summary. Requires the ADMIN " +
            "role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated gamification summary of the target user"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "User not found"),
    })
    @PostMapping("/evaluate")
    @HasRole(Role.ADMIN)
    public ResponseEntity<UserGamificationResponse> evaluate(
            @Parameter(description = "ID of the user to evaluate", required = true)
            @RequestParam("userId") UUID userId) {
        missionEngine.evaluateUser(userId);
        return ResponseEntity.ok(gamificationService.getUserGamification(userId));
    }
}
