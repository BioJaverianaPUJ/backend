package com.biojaveriana.api.gamification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.gamification.dto.UserGamificationResponse;
import com.biojaveriana.api.gamification.service.GamificationService;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gamification summary for the authenticated user.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gamification")
@Validated
@RequiredArgsConstructor
@Tag(name = "Gamification", description = "XP totals, levels and other gamification summaries for users. " +
    "Mission and achievement management lives under the *Missions* and *Achievements* tags.")
public class GamificationController {
    private final GamificationService gamificationService;

    @Operation(summary = "My gamification summary",
        description = "Returns the authenticated user's current gamification state " +
            "(total XP, derived level and related counters).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Gamification summary of the authenticated user"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping("/me")
    public ResponseEntity<UserGamificationResponse> getMyGamification(
            @Parameter(hidden = true) @CurrentUser User user) {
        log.info("Fetching gamification summary for user id={}", user.getId());
        return ResponseEntity.ok(gamificationService.getUserGamification(user.getId()));
    }
}