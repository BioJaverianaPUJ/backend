package com.biojaveriana.api.gamification.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.gamification.dto.XpLogResponse;
import com.biojaveriana.api.gamification.service.GamificationService;

/**
 * Controller for XP history endpoint.
 */
@RestController
@RequestMapping("/api/v1/gamification")
public class XpHistoryController {

    private final GamificationService gamificationService;

    public XpHistoryController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    /**
     * GET /api/v1/gamification/me/xp-history
     * Returns the current user's XP history.
     */
    @GetMapping("/me/xp-history")
    public ResponseEntity<Page<XpLogResponse>> getXpHistory(
        Authentication authentication,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID userId = extractUserId(authentication);
        Page<XpLogResponse> history = gamificationService.getXpHistory(userId, pageable);
        return ResponseEntity.ok(history);
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return UUID.fromString(authentication.getName());
    }
}
