package com.biojaveriana.api.gamification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.gamification.dto.UserGamificationResponse;
import com.biojaveriana.api.gamification.service.GamificationService;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.user.User;

@RestController
@RequestMapping("/api/v1/gamification")
public class GamificationController {
    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserGamificationResponse> getMyGamification(@CurrentUser User user) {
        return ResponseEntity.ok(gamificationService.getUserGamification(user.getId()));
    }
}