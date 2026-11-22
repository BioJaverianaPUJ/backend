package com.biojaveriana.api.gamification.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.gamification.dto.XpHistoryListResponse;
import com.biojaveriana.api.gamification.service.GamificationService;
import com.biojaveriana.api.security.CurrentUser;
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
 * XP acquisition history for the authenticated user.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gamification")
@Validated
@RequiredArgsConstructor
@Tag(name = "Gamification", description = "XP totals, levels and other gamification summaries for users. " +
    "Mission and achievement management lives under the *Missions* and *Achievements* tags.")
public class XpHistoryController {
    private final GamificationService gamificationService;

    @Operation(summary = "My XP history",
        description = "Paginated list of the authenticated user's XP grants, newest first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of XP history entries"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping("/me/xp-history")
    public ResponseEntity<XpHistoryListResponse> getXpHistory(
        @Parameter(hidden = true) @CurrentUser User user,
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (1..100)", example = "20")
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        log.info("Listing XP history for user id={}, page={}, size={}", user.getId(), page, size);
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var xpPage = gamificationService.getXpHistory(user.getId(), pageable);
        return ResponseEntity.ok(XpHistoryListResponse.from(xpPage));
    }
}