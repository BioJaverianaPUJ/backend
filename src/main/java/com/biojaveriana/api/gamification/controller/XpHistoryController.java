package com.biojaveriana.api.gamification.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.gamification.dto.XpLogResponse;
import com.biojaveriana.api.gamification.service.GamificationService;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.user.User;

@RestController
@RequestMapping("/api/v1/gamification")
public class XpHistoryController {
    private final GamificationService gamificationService;

    public XpHistoryController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/me/xp-history")
    public ResponseEntity<Page<XpLogResponse>> getXpHistory(@CurrentUser User user, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(gamificationService.getXpHistory(user.getId(), pageable));
    }
}