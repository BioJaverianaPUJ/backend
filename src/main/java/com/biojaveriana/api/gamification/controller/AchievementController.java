package com.biojaveriana.api.gamification.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
import com.biojaveriana.api.gamification.dto.AchievementResponse;
import com.biojaveriana.api.gamification.dto.AchievementUpdateRequest;
import com.biojaveriana.api.gamification.dto.UserAchievementResponse;
import com.biojaveriana.api.gamification.service.AchievementService;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.Role;

/**
 * Controller for achievement management (admin CRUD).
 */
@RestController
@RequestMapping("/api/v1/achievements")
public class AchievementController {
    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    /**
     * POST /api/v1/achievements
     * Creates a new achievement (admin only).
     */
    @PostMapping
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<AchievementResponse> createAchievement(
            @RequestBody AchievementCreateRequest request) {
        AchievementResponse response = achievementService.createAchievement(request);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * PUT /api/v1/achievements/{id}
     * Updates an achievement (admin only).
     */
    @PutMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<AchievementResponse> updateAchievement(
            @PathVariable UUID id,
            @RequestBody AchievementUpdateRequest request) {
        AchievementResponse response = achievementService.updateAchievement(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/achievements/{id}
     * Deletes an achievement (admin only).
     */
    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<Void> deleteAchievement(@PathVariable UUID id) {
        achievementService.deleteAchievement(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/achievements/{id}
     * Gets an achievement by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AchievementResponse> getAchievement(@PathVariable UUID id) {
        AchievementResponse response = achievementService.getAchievement(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/achievements/slug/{slug}
     * Gets an achievement by slug.
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<AchievementResponse> getAchievementBySlug(@PathVariable String slug) {
        AchievementResponse response = achievementService.getAchievementBySlug(slug);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/achievements
     * Lists all achievements (optionally filtered by active status).
     */
    @GetMapping
    public ResponseEntity<List<AchievementResponse>> listAchievements(
            @RequestParam(required = false) Boolean active) {
        List<AchievementResponse> responses = achievementService.listAchievements(active);
        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/v1/achievements/me
     * Gets the current user's achievements.
     */
    @GetMapping("/me")
    public ResponseEntity<List<UserAchievementResponse>> getMyAchievements(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<UserAchievementResponse> responses = achievementService.getUserAchievements(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/v1/achievements/me/completed
     * Gets the current user's completed achievements.
     */
    @GetMapping("/me/completed")
    public ResponseEntity<List<UserAchievementResponse>> getMyCompletedAchievements(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<UserAchievementResponse> responses = achievementService.getCompletedAchievements(userId);
        return ResponseEntity.ok(responses);
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return UUID.fromString(authentication.getName());
    }
}
