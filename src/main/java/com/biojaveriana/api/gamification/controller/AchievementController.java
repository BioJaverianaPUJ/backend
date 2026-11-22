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
public class AchievementController {
    private final AchievementService achievementService;
    private final AchievementEngine achievementEngine;
    private final GamificationService gamificationService;

    @PostMapping
    @HasRole(Role.ADMIN)
    public ResponseEntity<AchievementResponse> createAchievement(@RequestBody AchievementCreateRequest request) {
        return ResponseEntity.status(201).body(achievementService.createAchievement(request));
    }

    @PutMapping("/{id}")
    @HasRole(Role.ADMIN)
    public ResponseEntity<AchievementResponse> updateAchievement(@PathVariable UUID id, @RequestBody AchievementUpdateRequest request) {
        return ResponseEntity.ok(achievementService.updateAchievement(id, request));
    }

    @DeleteMapping("/{id}")
    @HasRole(Role.ADMIN)
    public ResponseEntity<Void> deleteAchievement(@PathVariable UUID id) {
        achievementService.deleteAchievement(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin dry-run: evaluate a candidate predicate against a user's current metrics.
     */
    @PostMapping("/preview")
    @HasRole(Role.ADMIN)
    public ResponseEntity<AchievementPreviewResponse> preview(@RequestBody AchievementPreviewRequest request) {
        return ResponseEntity.ok(achievementService.preview(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AchievementResponse> getAchievement(@PathVariable UUID id) {
        return ResponseEntity.ok(achievementService.getAchievement(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<AchievementResponse> getAchievementBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(achievementService.getAchievementBySlug(slug));
    }

    @GetMapping
    public ResponseEntity<AchievementListResponse> listAchievements(
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") @Min(0) int page,
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
    @GetMapping("/me")
    public ResponseEntity<AchievementProgressListResponse> getMyAchievementProgress(
        @CurrentUser User user,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var progressPage = achievementService.getUserAchievementProgress(user.getId(), pageable);
        return ResponseEntity.ok(AchievementProgressListResponse.from(progressPage));
    }

    /**
     * Earned-only view: achievements the user has completed.
     */
    @GetMapping("/me/completed")
    public ResponseEntity<UserAchievementListResponse> getMyCompletedAchievements(
        @CurrentUser User user,
        @RequestParam(defaultValue = "0") @Min(0) int page,
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
    @PostMapping("/evaluate")
    @HasRole(Role.ADMIN)
    public ResponseEntity<UserGamificationResponse> evaluate(@RequestParam("userId") UUID userId) {
        achievementEngine.evaluateUser(userId);
        return ResponseEntity.ok(gamificationService.getUserGamification(userId));
    }
}