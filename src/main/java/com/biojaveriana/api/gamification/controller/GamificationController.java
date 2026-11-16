package com.biojaveriana.api.gamification.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.gamification.dto.UserGamificationResponse;
import com.biojaveriana.api.gamification.service.GamificationService;

/**
 * Controller for user gamification endpoints.
 */
@RestController
@RequestMapping("/api/v1/gamification")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    /**
     * GET /api/v1/gamification/me
     * Returns the current user's gamification status.
     */
    @GetMapping("/me")
    public ResponseEntity<UserGamificationResponse> getMyGamification(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        UserGamificationResponse response = gamificationService.getUserGamification(userId);
        return ResponseEntity.ok(response);
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return UUID.fromString(authentication.getName());
    }
}
