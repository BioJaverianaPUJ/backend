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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/gamification")
@Validated
@RequiredArgsConstructor
public class XpHistoryController {
    private final GamificationService gamificationService;

    @GetMapping("/me/xp-history")
    public ResponseEntity<XpHistoryListResponse> getXpHistory(
        @CurrentUser User user,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        log.info("Listing XP history for user id={}, page={}, size={}", user.getId(), page, size);
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var xpPage = gamificationService.getXpHistory(user.getId(), pageable);
        return ResponseEntity.ok(XpHistoryListResponse.from(xpPage));
    }
}