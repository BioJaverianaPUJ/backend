package com.biojaveriana.api.gamification.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.gamification.config.GamificationConfig;
import com.biojaveriana.api.gamification.dto.UserGamificationResponse;
import com.biojaveriana.api.gamification.dto.XpLogResponse;
import com.biojaveriana.api.gamification.model.UserGamification;
import com.biojaveriana.api.gamification.model.XpLog;
import com.biojaveriana.api.gamification.repository.UserGamificationRepository;
import com.biojaveriana.api.gamification.repository.XpLogRepository;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;

@Service
public class GamificationService {

    private static final Logger log = LoggerFactory.getLogger(GamificationService.class);

    private final UserGamificationRepository userGamificationRepository;
    private final XpLogRepository xpLogRepository;
    private final UserRepository userRepository;
    private final GamificationConfig config;

    public GamificationService(
        UserGamificationRepository userGamificationRepository,
        XpLogRepository xpLogRepository,
        UserRepository userRepository,
        GamificationConfig config
    ) {
        this.userGamificationRepository = userGamificationRepository;
        this.xpLogRepository = xpLogRepository;
        this.userRepository = userRepository;
        this.config = config;
    }

    @Transactional
    public UserGamificationResponse awardXp(UUID userId, Long xpAmount, String reason) {
        UserGamification gamification = getOrCreateUserGamification(userId);

        Long oldTotalXp = gamification.getTotalXp();
        Integer oldLevel = gamification.getLevel();

        Long newTotalXp = oldTotalXp + xpAmount;
        Integer newLevel = calculateLevel(newTotalXp);

        gamification.setTotalXp(newTotalXp);
        gamification.setLevel(newLevel);
        gamification.setUpdatedAt(Instant.now());
        userGamificationRepository.save(gamification);

        XpLog xpLog = XpLog.builder()
            .userId(userId)
            .amount(xpAmount)
            .reason(reason)
            .totalAfter(newTotalXp)
            .levelAfter(newLevel)
            .build();
        xpLogRepository.save(xpLog);

        log.info("Awarded {} XP to user {}: new level {}, total XP {}",
            xpAmount, userId, newLevel, newTotalXp);

        return UserGamificationResponse.from(
            newTotalXp, newLevel,
            getXpForCurrentLevel(newLevel),
            getXpForNextLevel(newLevel)
        );
    }

    public Long getXpForCurrentLevel(Integer level) {
        if (level <= 1) return 0L;
        return Math.round(calculateXpForLevel(level));
    }

    public Long getXpForNextLevel(Integer level) {
        return Math.round(calculateXpForLevel(level + 1));
    }

    private double calculateXpForLevel(int level) {
        double baseXp = config.getBaseXp();
        double logBase = config.getLogBase();
        return baseXp * Math.log(level + 1) / Math.log(logBase);
    }

    public Integer calculateLevel(Long totalXp) {
        double baseXp = config.getBaseXp();
        double logBase = config.getLogBase();
        if (totalXp <= 0) return 1;
        double calculatedLevel = Math.exp(totalXp * Math.log(logBase) / baseXp) - 1;
        return Math.max(1, (int) Math.floor(calculatedLevel));
    }

    @Transactional
    public UserGamification getOrCreateUserGamification(UUID userId) {
        return userGamificationRepository.findById(userId)
            .orElseGet(() -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                UserGamification gamification = UserGamification.builder()
                    .userId(userId)
                    .user(user)
                    .totalXp(0L)
                    .level(1)
                    .build();
                return userGamificationRepository.save(gamification);
            });
    }

    @Transactional(readOnly = true)
    public Page<XpLogResponse> getXpHistory(UUID userId, Pageable pageable) {
        return xpLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(XpLogResponse::from);
    }

    @Transactional(readOnly = true)
    public UserGamificationResponse getUserGamification(UUID userId) {
        UserGamification gamification = userGamificationRepository.findById(userId)
            .orElseGet(() -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                UserGamification newGamification = UserGamification.builder()
                    .userId(userId)
                    .user(user)
                    .totalXp(0L)
                    .level(1)
                    .build();
                return userGamificationRepository.save(newGamification);
            });

        return UserGamificationResponse.from(
            gamification.getTotalXp(),
            gamification.getLevel(),
            getXpForCurrentLevel(gamification.getLevel()),
            getXpForNextLevel(gamification.getLevel())
        );
    }
}

