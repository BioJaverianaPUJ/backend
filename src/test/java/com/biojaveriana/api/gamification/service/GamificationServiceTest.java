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

    private GamificationService gamificationService;

    @BeforeEach
    void setUp() {
        gamificationService = new GamificationService(
            userGamificationRepository,
            xpLogRepository,
            userRepository,
            config
        );
    }

    @Nested
    @DisplayName("calculateLevel")
    class CalculateLevelTests {

        @Test
        @DisplayName("should return level 1 for zero XP")
        void shouldReturnLevel1ForZeroXp() {
            doReturn(100L).when(config).getBaseXp();
            doReturn(2.0).when(config).getLogBase();

            Integer level = gamificationService.calculateLevel(0L);
            assertEquals(1, level);
        }

        @Test
        @DisplayName("should return level 1 for negative XP")
        void shouldReturnLevel1ForNegativeXp() {
            doReturn(100L).when(config).getBaseXp();
            doReturn(2.0).when(config).getLogBase();

            Integer level = gamificationService.calculateLevel(-100L);
            assertEquals(1, level);
        }

        @Test
        @DisplayName("should calculate level using logarithmic formula")
        void shouldCalculateLevelUsingLogarithmicFormula() {
            doReturn(100L).when(config).getBaseXp();
            doReturn(2.0).when(config).getLogBase();

            Integer level = gamificationService.calculateLevel(100L);
            assertEquals(1, level);

            Integer level2 = gamificationService.calculateLevel(200L);
            assertEquals(3, level2);
        }

        @Test
        @DisplayName("should return increasing levels for increasing XP")
        void shouldReturnIncreasingLevelsForIncreasingXp() {
            doReturn(100L).when(config).getBaseXp();
            doReturn(2.0).when(config).getLogBase();

            Integer level1 = gamificationService.calculateLevel(100L);
            Integer level2 = gamificationService.calculateLevel(500L);
            Integer level3 = gamificationService.calculateLevel(1000L);

            assertTrue(level2 > level1);
            assertTrue(level3 > level2);
        }
    }

    @Nested
    @DisplayName("getXpForCurrentLevel")
    class GetXpForCurrentLevelTests {

        @Test
        @DisplayName("should return 0 for level 1")
        void shouldReturnZeroForLevel1() {
            Long xp = gamificationService.getXpForCurrentLevel(1);
            assertEquals(0L, xp);
        }

        @Test
        @DisplayName("should return correct XP threshold for higher levels")
        void shouldReturnCorrectXpThresholdForHigherLevels() {
            doReturn(100L).when(config).getBaseXp();
            doReturn(2.0).when(config).getLogBase();

            Long xp = gamificationService.getXpForCurrentLevel(2);
            assertEquals(158L, xp);
        }
    }

    @Nested
    @DisplayName("getXpForNextLevel")
    class GetXpForNextLevelTests {

        @Test
        @DisplayName("should return XP for next level")
        void shouldReturnXpForNextLevel() {
            doReturn(100L).when(config).getBaseXp();
            doReturn(2.0).when(config).getLogBase();

            Long xp = gamificationService.getXpForNextLevel(2);
            assertEquals(200L, xp);
        }

    @Nested
    @DisplayName("awardXp")
    class AwardXpTests {

        @Test
        @DisplayName("should create new gamification record when user does not exist")
        void shouldCreateNewGamificationRecordWhenUserDoesNotExist() {
            UUID userId = UUID.randomUUID();
            Long xpAmount = 100L;
            String reason = "Test achievement";

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserGamification newGamification = UserGamification.builder()
                .userId(userId)
                .user(user)
                .totalXp(0L)
                .level(1)
                .build();
            when(userGamificationRepository.findById(userId)).thenReturn(Optional.empty());
            when(userGamificationRepository.save(any())).thenReturn(newGamification);

            doReturn(100L).when(config).getBaseXp();
            doReturn(2.0).when(config).getLogBase();

            UserGamificationResponse response = gamificationService.awardXp(userId, xpAmount, reason);

            assertNotNull(response);
            assertEquals(100L, response.totalXp());
            assertEquals(1, response.level());
            verify(userGamificationRepository, times(2)).save(any());
            verify(xpLogRepository).save(any());
        }

        @Test
        @DisplayName("should update existing gamification record")
        void shouldUpdateExistingGamificationRecord() {
            UUID userId = UUID.randomUUID();
            Long xpAmount = 50L;
            String reason = "Test update";

            UserGamification existing = UserGamification.builder()
                .userId(userId)
                .totalXp(200L)
                .level(2)
                .build();
            when(userGamificationRepository.findById(userId)).thenReturn(Optional.of(existing));

            doReturn(100L).when(config).getBaseXp();
            doReturn(2.0).when(config).getLogBase();

            UserGamificationResponse response = gamificationService.awardXp(userId, xpAmount, reason);

            assertNotNull(response);
            assertEquals(250L, response.totalXp());
            assertEquals(4, response.level());
            verify(userGamificationRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            UUID userId = UUID.randomUUID();

            UserGamification existing = UserGamification.builder()
                .userId(userId)
                .totalXp(0L)
                .level(1)
                .build();
                
            when(userGamificationRepository.findById(userId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                () -> gamificationService.awardXp(userId, 100L, "Test"));
        }
    }

    @Nested
    @DisplayName("getUserGamification")
    class GetUserGamificationTests {

        @Test
        @DisplayName("should return existing gamification data")
        void shouldReturnExistingGamificationData() {
            UUID userId = UUID.randomUUID();

            UserGamification existing = UserGamification.builder()
                .userId(userId)
                .totalXp(500L)
                .level(4)
                .build();
            when(userGamificationRepository.findById(userId)).thenReturn(Optional.of(existing));

            doReturn(100L).when(config).getBaseXp();
            doReturn(2.0).when(config).getLogBase();

            UserGamificationResponse response = gamificationService.getUserGamification(userId);

            assertNotNull(response);
            assertEquals(500L, response.totalXp());
            assertEquals(4, response.level());
        }

        @Test
        @DisplayName("should create new gamification record when not found")
        void shouldCreateNewGamificationRecordWhenNotFound() {
            UUID userId = UUID.randomUUID();

            User user = mock(User.class);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserGamification newGamification = UserGamification.builder()
                .userId(userId)
                .user(user)
                .totalXp(0L)
                .level(1)
                .build();
            when(userGamificationRepository.findById(userId)).thenReturn(Optional.empty());
            when(userGamificationRepository.save(any())).thenReturn(newGamification);

            doReturn(100L).when(config).getBaseXp();
            doReturn(2.0).when(config).getLogBase();

            UserGamificationResponse response = gamificationService.getUserGamification(userId);

            assertNotNull(response);
            assertEquals(0L, response.totalXp());
            assertEquals(1, response.level());
        }
    }
}


}
