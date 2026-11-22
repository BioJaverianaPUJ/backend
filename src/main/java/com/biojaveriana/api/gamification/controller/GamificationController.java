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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/gamification")
@Validated
@RequiredArgsConstructor
public class GamificationController {
    private final GamificationService gamificationService;

    @GetMapping("/me")
    public ResponseEntity<UserGamificationResponse> getMyGamification(@CurrentUser User user) {
        log.info("Fetching gamification summary for user id={}", user.getId());
        return ResponseEntity.ok(gamificationService.getUserGamification(user.getId()));
    }
}