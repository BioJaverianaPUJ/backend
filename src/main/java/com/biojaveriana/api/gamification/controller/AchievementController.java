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

import com.biojaveriana.api.gamification.dto.AchievementCreateRequest;
import com.biojaveriana.api.gamification.dto.AchievementListResponse;
import com.biojaveriana.api.gamification.dto.AchievementPreviewRequest;
import com.biojaveriana.api.gamification.dto.AchievementPreviewResponse;
import com.biojaveriana.api.gamification.dto.AchievementProgressListResponse;
import com.biojaveriana.api.gamification.dto.AchievementResponse;
import com.biojaveriana.api.gamification.dto.AchievementUpdateRequest;
import com.biojaveriana.api.gamification.dto.UserAchievementListResponse;
import com.biojaveriana.api.gamification.dto.UserGamificationResponse;
import com.biojaveriana.api.gamification.service.AchievementEngine;
import com.biojaveriana.api.gamification.service.AchievementService;
import com.biojaveriana.api.gamification.service.GamificationService;
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
 * Controller for achievement management (admin CRUD + predicate dry-run) and
 * per-user achievement views.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/achievements")
@Validated
@RequiredArgsConstructor
@Tag(name = "Achievements", description = "One-off gamification rewards. Admins manage the achievement " +
    "catalogue (CRUD, predicate dry-run, forced evaluation); users see their own progress and " +
    "completed achievements.")
public class AchievementController {
    private final AchievementService achievementService;
    private final AchievementEngine achievementEngine;
    private final GamificationService gamificationService;

    @Operation(summary = "Create an achievement (admin)",
        description = "Adds a new achievement. The `predicate` is a structured logic-block " +
            "expression over the user metrics; when it is satisfied the user earns the " +
            "`xpReward` once. Requires the ADMIN role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Achievement created"),
        @ApiResponse(responseCode = "400", description = "Validation failed (bad predicate, ...)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @PostMapping
    @HasRole(Role.ADMIN)
    public ResponseEntity<AchievementResponse> createAchievement(@RequestBody AchievementCreateRequest request) {
        return ResponseEntity.status(201).body(achievementService.createAchievement(request));
    }

    @Operation(summary = "Update an achievement (admin)",
        description = "Partially updates an achievement; only non-null fields are applied. " +
            "Requires the ADMIN role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Achievement updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed (bad predicate, ...)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Achievement not found"),
    })
    @PutMapping("/{id}")
    @HasRole(Role.ADMIN)
    public ResponseEntity<AchievementResponse> updateAchievement(
            @Parameter(description = "ID of the achievement to update", required = true) @PathVariable UUID id,
            @RequestBody AchievementUpdateRequest request) {
        return ResponseEntity.ok(achievementService.updateAchievement(id, request));
    }

    @Operation(summary = "Delete an achievement (admin)",
        description = "Removes an achievement from the catalogue. " +
            "Requires the ADMIN role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Achievement deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Achievement not found"),
    })
    @DeleteMapping("/{id}")
    @HasRole(Role.ADMIN)
    public ResponseEntity<Void> deleteAchievement(
            @Parameter(description = "ID of the achievement to delete", required = true) @PathVariable UUID id) {
        achievementService.deleteAchievement(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin dry-run: evaluate a candidate predicate against a user's current metrics.
     */
    @Operation(summary = "Preview a candidate predicate (admin)",
        description = "Dry-run: evaluates a candidate predicate against the given user's current " +
            "metrics without saving anything. Returns whether the achievement would be unlocked " +
            "and the current metric values. Requires the ADMIN role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dry-run evaluation result"),
        @ApiResponse(responseCode = "400", description = "Validation failed (bad predicate, ...)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "User not found"),
    })
    @PostMapping("/preview")
    @HasRole(Role.ADMIN)
    public ResponseEntity<AchievementPreviewResponse> preview(@RequestBody AchievementPreviewRequest request) {
        return ResponseEntity.ok(achievementService.preview(request));
    }

    @Operation(summary = "Get an achievement by ID",
        description = "Fetches a single achievement from the catalogue.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The achievement"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Achievement not found"),
    })
    @GetMapping("/{id}")
    public ResponseEntity<AchievementResponse> getAchievement(
            @Parameter(description = "ID of the achievement", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(achievementService.getAchievement(id));
    }

    @Operation(summary = "Get an achievement by slug",
        description = "Fetches a single achievement by its unique slug.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The achievement"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Achievement not found"),
    })
    @GetMapping("/slug/{slug}")
    public ResponseEntity<AchievementResponse> getAchievementBySlug(
            @Parameter(description = "Slug of the achievement", required = true) @PathVariable String slug) {
        return ResponseEntity.ok(achievementService.getAchievementBySlug(slug));
    }

    @Operation(summary = "List achievements",
        description = "Paginated list of achievements from the catalogue, newest first. " +
            "Optionally filters by active state.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of achievements"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping
    public ResponseEntity<AchievementListResponse> listAchievements(
        @Parameter(description = "Filter by active state", required = false) @RequestParam(required = false) Boolean active,
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (1..100)", example = "20")
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var achievementPage = achievementService.listAchievements(active, pageable);
        return ResponseEntity.ok(AchievementListResponse.from(achievementPage));
    }

    /**
     * Progression view: every active achievement with the user's live progress
     * and completion state.
     */
    @Operation(summary = "My achievement progress",
        description = "Every active achievement with the authenticated user's live progress and " +
            "completion state.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of achievement progress entries"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping("/me")
    public ResponseEntity<AchievementProgressListResponse> getMyAchievementProgress(
        @Parameter(hidden = true) @CurrentUser User user,
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (1..100)", example = "20")
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var progressPage = achievementService.getUserAchievementProgress(user.getId(), pageable);
        return ResponseEntity.ok(AchievementProgressListResponse.from(progressPage));
    }

    /**
     * Earned-only view: achievements the user has completed.
     */
    @Operation(summary = "My completed achievements",
        description = "Achievements the authenticated user has already completed, " +
            "newest first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of completed achievements"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping("/me/completed")
    public ResponseEntity<UserAchievementListResponse> getMyCompletedAchievements(
        @Parameter(hidden = true) @CurrentUser User user,
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (1..100)", example = "20")
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("completedAt").descending());
        var completedPage = achievementService.getCompletedAchievements(user.getId(), pageable);
        return ResponseEntity.ok(UserAchievementListResponse.from(completedPage));
    }

    /**
     * Admin: re-evaluates every active achievement for a user (awarding XP for
     * anything newly satisfied) and returns the user's updated gamification
     * summary. Use this to backfill XP for activity that predates an
     * achievement.
     */
    @Operation(summary = "Force-evaluate a user's achievements (admin)",
        description = "Re-evaluates every active achievement for the given user, awarding XP for " +
            "anything newly satisfied, and returns the user's updated gamification summary. " +
            "Useful to backfill XP for activity that predates an achievement. " +
            "Requires the ADMIN role (SUPERADMIN bypasses).")
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
        achievementEngine.evaluateUser(userId);
        return ResponseEntity.ok(gamificationService.getUserGamification(userId));
    }
}