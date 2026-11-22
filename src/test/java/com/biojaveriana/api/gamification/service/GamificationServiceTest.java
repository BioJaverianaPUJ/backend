package com.biojaveriana.api.gamification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.gamification.config.GamificationConfig;
import com.biojaveriana.api.gamification.dto.UserGamificationResponse;
import com.biojaveriana.api.gamification.model.UserGamification;
import com.biojaveriana.api.gamification.repository.UserGamificationRepository;
import com.biojaveriana.api.gamification.repository.XpLogRepository;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    private UserGamificationRepository userGamificationRepository;

    @Mock
    private XpLogRepository xpLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GamificationConfig config;

    @Mock
    private EntityManager entityManager;

    private GamificationService gamificationService;

    @BeforeEach
    void setUp() {
        gamificationService = new GamificationService(
            userGamificationRepository,
            xpLogRepository,
            config,
            userRepository,
            entityManager
        );
        lenient().doReturn(100L).when(config).getBaseXp();
        lenient().doReturn(2.0).when(config).getLogBase();
    }

    @Nested
    @DisplayName("XP thresholds")
    class XpThresholdTests {
        @Test
        @DisplayName("level 1 starts at 0 XP")
        void level1StartsAtZero() {
            assertEquals(0L, gamificationService.getXpForCurrentLevel(1));
        }

        @Test
        @DisplayName("level 2 threshold is baseXp")
        void level2ThresholdIsBaseXp() {
            assertEquals(100L, gamificationService.getXpForCurrentLevel(2));
        }

        @Test
        @DisplayName("level 3 threshold is baseXp*(1+logBase)")
        void level3Threshold() {
            assertEquals(300L, gamificationService.getXpForCurrentLevel(3));
        }

        @Test
        @DisplayName("next-level XP is the following threshold")
        void nextLevelXp() {
            assertEquals(100L, gamificationService.getXpForNextLevel(1));
            assertEquals(300L, gamificationService.getXpForNextLevel(2));
        }
    }

    @Nested
    @DisplayName("awardXp")
    class AwardXpTests {
        @Test
        @DisplayName("should create a new gamification record and log XP")
        void shouldCreateNewRecord() {
            UUID userId = UUID.randomUUID();
            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userGamificationRepository.findById(userId)).thenReturn(Optional.empty());

            UserGamificationResponse response = gamificationService.awardXp(userId, 50L, "Test");

            assertEquals(50L, response.totalXp());
            assertEquals(1, response.level());
            verify(entityManager).persist(any(UserGamification.class));
            verify(xpLogRepository).save(any());
        }

        @Test
        @DisplayName("should accumulate XP and recompute level on an existing record")
        void shouldAccumulateXp() {
            UUID userId = UUID.randomUUID();
            UserGamification existing = UserGamification.builder().userId(userId).totalXp(250L).level(2).build();
            when(userGamificationRepository.findById(userId)).thenReturn(Optional.of(existing));

            UserGamificationResponse response = gamificationService.awardXp(userId, 100L, "Test");

            assertEquals(350L, response.totalXp());
            assertEquals(3, response.level());
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            UUID userId = UUID.randomUUID();
            when(userGamificationRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> gamificationService.awardXp(userId, 10L, "Test"));
        }
    }

    @Nested
    @DisplayName("getUserGamification")
    class GetUserGamificationTests {
        @Test
        @DisplayName("should return existing gamification with username")
        void shouldReturnExisting() {
            UUID userId = UUID.randomUUID();
            User user = mock(User.class);
            when(user.getUsername()).thenReturn("alice");
            UserGamification existing = UserGamification.builder().userId(userId).totalXp(500L).level(4).build();
            when(userGamificationRepository.findById(userId)).thenReturn(Optional.of(existing));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserGamificationResponse response = gamificationService.getUserGamification(userId);

            assertEquals(500L, response.totalXp());
            assertEquals(4, response.level());
            assertEquals("alice", response.username());
        }

        @Test
        @DisplayName("should create a new record when missing")
        void shouldCreateWhenMissing() {
            UUID userId = UUID.randomUUID();
            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userGamificationRepository.findById(userId)).thenReturn(Optional.empty());

            UserGamificationResponse response = gamificationService.getUserGamification(userId);

            assertEquals(0L, response.totalXp());
            assertEquals(1, response.level());
            verify(entityManager).persist(any(UserGamification.class));
        }
    }
}