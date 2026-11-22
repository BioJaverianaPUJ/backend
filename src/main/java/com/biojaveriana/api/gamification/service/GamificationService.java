package com.biojaveriana.api.gamification.service;

import java.util.UUID;

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
import jakarta.persistence.EntityManager;

@Service
public class GamificationService {
    private final UserGamificationRepository userGamificationRepository;
    private final XpLogRepository xpLogRepository;
    private final GamificationConfig gamificationConfig;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public GamificationService(
        UserGamificationRepository userGamificationRepository,
        XpLogRepository xpLogRepository,
        GamificationConfig gamificationConfig,
        UserRepository userRepository,
        EntityManager entityManager
    ) {
        this.userGamificationRepository = userGamificationRepository;
        this.xpLogRepository = xpLogRepository;
        this.gamificationConfig = gamificationConfig;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public UserGamificationResponse awardXp(UUID userId, Long amount, String source) {
        UserGamification gamification = getOrCreateUserGamification(userId);

        long newTotalXp = gamification.getTotalXp() + amount;
        int newLevel = calculateLevel(newTotalXp);

        gamification.setTotalXp(newTotalXp);
        gamification.setLevel(newLevel);
        userGamificationRepository.save(gamification);

        XpLog xpLog = XpLog.builder()
            .userId(userId)
            .amount(amount)
            .reason(source)
            .totalAfter(newTotalXp)
            .levelAfter(newLevel)
            .build();
        xpLogRepository.save(xpLog);

        return UserGamificationResponse.from(
            newTotalXp,
            newLevel,
            getXpForCurrentLevel(newLevel),
            getXpForNextLevel(newLevel)
        );
    }

    @Transactional(readOnly = true)
    public long getXpForCurrentLevel(Integer level) {
        return calculateXpForLevel(level);
    }

    @Transactional(readOnly = true)
    public long getXpForNextLevel(Integer level) {
        return calculateXpForLevel(level + 1);
    }

    /**
     * Cumulative XP threshold required to REACH the given level. Level 1 starts
     * at 0; each subsequent level costs baseXp * logBase^(n-1) more than the
     * previous one.
     */
    private long calculateXpForLevel(Integer level) {
        if (level <= 1) {
            return 0L;
        }
        long baseXp = gamificationConfig.getBaseXp();
        double logBase = gamificationConfig.getLogBase();
        long requiredXp = 0L;
        for (int i = 1; i < level; i++) {
            requiredXp += (long) (baseXp * Math.pow(logBase, i - 1));
        }
        return requiredXp;
    }

    private int calculateLevel(Long totalXp) {
        int level = 1;
        long xpForNextLevel = calculateXpForLevel(level + 1);
        while (totalXp >= xpForNextLevel) {
            level++;
            xpForNextLevel = calculateXpForLevel(level + 1);
        }
        return level;
    }

    @Transactional
    public UserGamification getOrCreateUserGamification(UUID userId) {
        return userGamificationRepository.findById(userId)
            .orElseGet(() -> createNewUserGamification(userId));
    }

    @Transactional
    public UserGamification getOrCreateUserGamificationWithLock(UUID userId) {
        return userGamificationRepository.findByIdWithPessimisticLock(userId)
            .orElseGet(() -> createNewUserGamification(userId));
    }

    /**
     * Creates the gamification record for a user. Uses {@code persist} rather
     * than {@code repository.save}: the primary key is the user id (assigned,
     * not generated), so {@code save} would take the merge path and fail on
     * the {@code @MapsId} association.
     */
    private UserGamification createNewUserGamification(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        UserGamification newGamification = UserGamification.builder()
            .userId(userId)
            .user(user)
            .totalXp(0L)
            .level(1)
            .build();
        entityManager.persist(newGamification);
        return newGamification;
    }

    @Transactional(readOnly = true)
    public Page<XpLogResponse> getXpHistory(UUID userId, Pageable pageable) {
        return xpLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(XpLogResponse::from);
    }

    @Transactional
    public UserGamificationResponse getUserGamification(UUID userId) {
        UserGamification gamification = userGamificationRepository.findById(userId)
            .orElseGet(() -> createNewUserGamification(userId));

        User user = userRepository.findById(userId).orElse(null);
        String username = user != null ? user.getUsername() : null;

        return UserGamificationResponse.from(
            userId,
            username,
            gamification.getTotalXp(),
            gamification.getLevel(),
            getXpForCurrentLevel(gamification.getLevel()),
            getXpForNextLevel(gamification.getLevel())
        );
    }
}