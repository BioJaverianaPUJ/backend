package com.biojaveriana.api.gamification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.gamification.dto.AchievementCreateRequest;
import com.biojaveriana.api.gamification.dto.AchievementResponse;
import com.biojaveriana.api.gamification.dto.AchievementUpdateRequest;
import com.biojaveriana.api.gamification.dto.UserAchievementResponse;
import com.biojaveriana.api.gamification.model.Achievement;
import com.biojaveriana.api.gamification.model.UserAchievement;
import com.biojaveriana.api.gamification.repository.AchievementRepository;
import com.biojaveriana.api.gamification.repository.UserAchievementRepository;
import com.biojaveriana.api.gamification.exception.NotFoundException;
import com.biojaveriana.api.gamification.exception.ValidationException;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    private AchievementService achievementService;

    @BeforeEach
    void setUp() {
        achievementService = new AchievementService(achievementRepository, userAchievementRepository);
    }
    @Nested
    @DisplayName("createAchievement")
    class CreateAchievementTests {

        @Test
        @DisplayName("should create achievement successfully")
        void shouldCreateAchievementSuccessfully() {
            AchievementCreateRequest request = new AchievementCreateRequest(
                "first-post", "First Post", "Complete your first post", 50, "post_created", null
            );

            Achievement savedAchievement = Achievement.builder()
                .id(UUID.randomUUID())
                .slug(request.slug())
                .name(request.name())
                .description(request.description())
                .xpReward(request.xpReward())
                .eventType(request.eventType())
                .predicateJson(request.predicateJson())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            when(achievementRepository.findBySlug(request.slug())).thenReturn(Optional.empty());
            when(achievementRepository.save(any())).thenReturn(savedAchievement);

            AchievementResponse response = achievementService.createAchievement(request);

            assertNotNull(response);
            assertEquals(request.slug(), response.slug());
            assertEquals(request.name(), response.name());
            assertEquals(request.xpReward(), response.xpReward());
            assertEquals(request.eventType(), response.eventType());
            assertTrue(response.active());
            verify(achievementRepository).save(any());
        }

        @Test
        @DisplayName("should throw exception when slug already exists")
        void shouldThrowExceptionWhenSlugAlreadyExists() {
            AchievementCreateRequest request = new AchievementCreateRequest(
                "first-post", "First Post", "Complete your first post", 50, "post_created", null
            );

            Achievement existingAchievement = Achievement.builder()
                .id(UUID.randomUUID())
                .slug("first-post")
                .name("First Post")
                .active(true)
                .build();

            when(achievementRepository.findBySlug(request.slug())).thenReturn(Optional.of(existingAchievement));

            assertThrows(ValidationException.class,
                () -> achievementService.createAchievement(request));
        }
        }

    @Nested
    @DisplayName("updateAchievement")
    class UpdateAchievementTests {

        @Test
        @DisplayName("should update achievement fields")
        void shouldUpdateAchievementFields() {
            UUID id = UUID.randomUUID();
            AchievementUpdateRequest request = new AchievementUpdateRequest(
                "Updated Name", "Updated Description", 100, "post_created", null, true
            );

            Achievement existingAchievement = Achievement.builder()
                .id(id)
                .slug("first-post")
                .name("First Post")
                .description("Old description")
                .xpReward(50)
                .eventType("post_created")
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            Achievement updatedAchievement = Achievement.builder()
                .id(id)
                .slug("first-post")
                .name("Updated Name")
                .description("Updated Description")
                .xpReward(100)
                .eventType("post_created")
                .active(true)
                .createdAt(existingAchievement.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

            when(achievementRepository.findById(id)).thenReturn(Optional.of(existingAchievement));
            when(achievementRepository.save(any())).thenReturn(updatedAchievement);

            AchievementResponse response = achievementService.updateAchievement(id, request);

            assertNotNull(response);
            assertEquals("Updated Name", response.name());
            assertEquals("Updated Description", response.description());
            assertEquals(100, response.xpReward());
            assertTrue(response.active());
            verify(achievementRepository).save(any());
        }
    }

    @Nested
    @DisplayName("deleteAchievement")
    class DeleteAchievementTests {

        @Test
        @DisplayName("should delete achievement successfully")
        void shouldDeleteAchievementSuccessfully() {
            UUID id = UUID.randomUUID();
            when(achievementRepository.existsById(id)).thenReturn(true);
            assertDoesNotThrow(() -> achievementService.deleteAchievement(id));
            verify(achievementRepository).deleteById(id);
        }

        @Test
        @DisplayName("should throw exception when achievement not found for deletion")
        void shouldThrowExceptionWhenAchievementNotFoundForDeletion() {
            UUID id = UUID.randomUUID();
            when(achievementRepository.existsById(id)).thenReturn(false);
            assertThrows(NotFoundException.class,
                () -> achievementService.deleteAchievement(id));
        }

    @Nested
    @DisplayName("getAchievement")
    class GetAchievementTests {

        @Test
        @DisplayName("should return achievement by id")
        void shouldReturnAchievementById() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            Achievement achievement = Achievement.builder()
                .id(id).slug("first-post").name("First Post").description("Complete your first post")
                .xpReward(50).eventType("post_created").active(true)
                .createdAt(now).updatedAt(now).build();
            when(achievementRepository.findById(id)).thenReturn(Optional.of(achievement));
            AchievementResponse response = achievementService.getAchievement(id);
            assertNotNull(response);
            assertEquals(id, response.id());
            assertEquals("first-post", response.slug());
            assertEquals("First Post", response.name());
        }

        @Test
        @DisplayName("should throw exception when achievement not found by id")
        void shouldThrowExceptionWhenAchievementNotFoundById() {
            UUID id = UUID.randomUUID();
            when(achievementRepository.findById(id)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                () -> achievementService.getAchievement(id));
        }

        @Test
        @DisplayName("should return achievement by slug")
        void shouldReturnAchievementBySlug() {
            Instant now = Instant.now();
            Achievement achievement = Achievement.builder()
                .id(UUID.randomUUID()).slug("first-post").name("First Post")
                .description("Complete your first post").xpReward(50)
                .eventType("post_created").active(true)
                .createdAt(now).updatedAt(now).build();
            when(achievementRepository.findBySlug("first-post")).thenReturn(Optional.of(achievement));
            AchievementResponse response = achievementService.getAchievementBySlug("first-post");
            assertNotNull(response);
            assertEquals("first-post", response.slug());
            assertEquals("First Post", response.name());
        }

        @Test
        @DisplayName("should throw exception when achievement not found by slug")
        void shouldThrowExceptionWhenAchievementNotFoundBySlug() {
            when(achievementRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

    @Nested
    @DisplayName("listAchievements")
    class ListAchievementsTests {

        @Test
        @DisplayName("should return all achievements when active is null")
        void shouldReturnAllAchievementsWhenActiveIsNull() {
            Instant now = Instant.now();
            Achievement achievement1 = Achievement.builder()
                .id(UUID.randomUUID()).slug("first-post").name("First Post")
                .active(true).createdAt(now).updatedAt(now).build();
            Achievement achievement2 = Achievement.builder()
                .id(UUID.randomUUID()).slug("five-posts").name("Five Posts")
                .active(false).createdAt(now).updatedAt(now).build();
            when(achievementRepository.findAll()).thenReturn(List.of(achievement1, achievement2));
            List<AchievementResponse> responses = achievementService.listAchievements(null);
            assertEquals(2, responses.size());
            verify(achievementRepository).findAll();
        }

        @Test
        @DisplayName("should return only active achievements when active is true")
        void shouldReturnOnlyActiveAchievementsWhenActiveIsTrue() {
            Instant now = Instant.now();
            Achievement activeAchievement = Achievement.builder()
                .id(UUID.randomUUID()).slug("first-post").name("First Post")
                .active(true).createdAt(now).updatedAt(now).build();
            when(achievementRepository.findByActiveOrderByCreatedAtDesc(true))
                .thenReturn(List.of(activeAchievement));
            List<AchievementResponse> responses = achievementService.listAchievements(true);
            assertEquals(1, responses.size());
            assertTrue(responses.get(0).active());
        }

        @Test
        @DisplayName("should return only inactive achievements when active is false")
        void shouldReturnOnlyInactiveAchievementsWhenActiveIsFalse() {
            Instant now = Instant.now();
            Achievement inactiveAchievement = Achievement.builder()
                .id(UUID.randomUUID()).slug("five-posts").name("Five Posts")
                .active(false).createdAt(now).updatedAt(now).build();
            when(achievementRepository.findByActiveOrderByCreatedAtDesc(false))
                .thenReturn(List.of(inactiveAchievement));
            List<AchievementResponse> responses = achievementService.listAchievements(false);
            assertEquals(1, responses.size());
            assertFalse(responses.get(0).active());

        }
    }
    @Nested
    @DisplayName("getUserAchievements")
    class GetUserAchievementsTests {

        @Test
        @DisplayName("should return user achievements")
        void shouldReturnUserAchievements() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();
            Achievement achievement = Achievement.builder()
                .id(UUID.randomUUID()).slug("first-post").name("First Post")
                .xpReward(50).active(true).createdAt(now).updatedAt(now).build();
            UserAchievement userAchievement = UserAchievement.builder()
                .id(UUID.randomUUID()).achievement(achievement).userId(userId)
                .progress(5).completed(false).xpAwarded(0L)
                .createdAt(now).updatedAt(now).build();
            when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(userAchievement));
            List<UserAchievementResponse> responses = achievementService.getUserAchievements(userId);
            assertEquals(1, responses.size());
            assertEquals("First Post", responses.get(0).name());
        }

        @Test
        @DisplayName("should return empty list when no user achievements")
        void shouldReturnEmptyListWhenNoUserAchievements() {
            UUID userId = UUID.randomUUID();
            when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of());
            List<UserAchievementResponse> responses = achievementService.getUserAchievements(userId);
            assertNotNull(responses);
            assertTrue(responses.isEmpty());
        }
    }

    @Nested
    @DisplayName("getCompletedAchievements")
    class GetCompletedAchievementsTests {

        @Test
        @DisplayName("should return only completed achievements")
        void shouldReturnOnlyCompletedAchievements() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();
            Achievement achievement = Achievement.builder()
                .id(UUID.randomUUID()).slug("first-post").name("First Post")
                .xpReward(50).active(true).createdAt(now).updatedAt(now).build();
            UserAchievement completed = UserAchievement.builder()
                .id(UUID.randomUUID()).achievement(achievement).userId(userId)
                .progress(5).completed(true).completedAt(now)
                .xpAwarded(50L).createdAt(now).updatedAt(now).build();
            when(userAchievementRepository.findByUserIdAndCompletedTrue(userId))
                .thenReturn(List.of(completed));
            List<UserAchievementResponse> responses = achievementService.getCompletedAchievements(userId);
            assertEquals(1, responses.size());
            assertTrue(responses.get(0).completed());
        }

        @Test
        @DisplayName("should return empty list when no completed achievements")
        void shouldReturnEmptyListWhenNoCompletedAchievements() {
            UUID userId = UUID.randomUUID();
            when(userAchievementRepository.findByUserIdAndCompletedTrue(userId)).thenReturn(List.of());
            List<UserAchievementResponse> responses = achievementService.getCompletedAchievements(userId);
            assertNotNull(responses);
            assertTrue(responses.isEmpty());
        }
    }
}

}

}


}