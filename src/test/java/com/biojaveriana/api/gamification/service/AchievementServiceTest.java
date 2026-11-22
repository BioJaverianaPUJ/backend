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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.biojaveriana.api.gamification.dto.AchievementCreateRequest;
import com.biojaveriana.api.gamification.dto.AchievementProgressResponse;
import com.biojaveriana.api.gamification.dto.AchievementPreviewRequest;
import com.biojaveriana.api.gamification.dto.AchievementPreviewResponse;
import com.biojaveriana.api.gamification.dto.AchievementResponse;
import com.biojaveriana.api.gamification.dto.AchievementUpdateRequest;
import com.biojaveriana.api.gamification.exception.NotFoundException;
import com.biojaveriana.api.gamification.exception.ValidationException;
import com.biojaveriana.api.gamification.metric.Metric;
import com.biojaveriana.api.gamification.metric.MetricRegistry;
import com.biojaveriana.api.gamification.model.Achievement;
import com.biojaveriana.api.gamification.model.UserAchievement;
import com.biojaveriana.api.gamification.predicate.PredicateCodec;
import com.biojaveriana.api.gamification.predicate.PredicateEvaluator;
import com.biojaveriana.api.gamification.predicate.PredicateProgressCalculator;
import com.biojaveriana.api.gamification.repository.AchievementRepository;
import com.biojaveriana.api.gamification.repository.UserAchievementRepository;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {
    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    private JsonMapper jsonMapper;
    private MetricRegistry metricRegistry;
    private AchievementService achievementService;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        Metric posts = new Metric() {
            @Override
            public String type() {
                return "posts";
            }

            @Override
            public Long value(UUID id) {
                return 5L;
            }
        };
        metricRegistry = new MetricRegistry(List.of(posts));
        PredicateCodec codec = new PredicateCodec(jsonMapper);
        PredicateEvaluator evaluator = new PredicateEvaluator(metricRegistry);
        achievementService = new AchievementService(achievementRepository, userAchievementRepository,
            codec, metricRegistry, evaluator, jsonMapper, new PredicateProgressCalculator());
    }

    private JsonNode predicate(String json) throws Exception {
        return jsonMapper.readTree(json);
    }

    private Achievement built(String slug, String name, Integer xp, String predicateJson) {
        return Achievement.builder()
            .id(UUID.randomUUID())
            .slug(slug)
            .name(name)
            .xpReward(xp)
            .predicateJson(predicateJson)
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    @Nested
    @DisplayName("createAchievement")
    class CreateAchievementTests {
        @Test
        @DisplayName("should create an achievement with a structured predicate")
        void shouldCreateAchievement() throws Exception {
            AchievementCreateRequest request = new AchievementCreateRequest(
                "first-post", "First Post", "Complete your first post", 50,
                predicate("{\"metric\":\"posts\",\"op\":\"gte\",\"value\":5}"));

            when(achievementRepository.findBySlug("first-post")).thenReturn(Optional.empty());
            when(achievementRepository.save(any())).thenAnswer(inv -> {
                Achievement a = inv.getArgument(0);
                a.setCreatedAt(Instant.now());
                a.setUpdatedAt(Instant.now());
                return a;
            });

            AchievementResponse response = achievementService.createAchievement(request);

            assertNotNull(response);
            assertEquals("first-post", response.slug());
            assertEquals("First Post", response.name());
            assertEquals(50, response.xpReward());
            assertNotNull(response.predicate());
            assertTrue(response.active());
            verify(achievementRepository).save(any());
        }

        @Test
        @DisplayName("should reject a duplicate slug")
        void shouldRejectDuplicateSlug() {
            AchievementCreateRequest request = new AchievementCreateRequest("first-post", "First Post", null, 50, null);
            when(achievementRepository.findBySlug("first-post")).thenReturn(Optional.of(built("first-post", "x", 1, null)));
            assertThrows(ValidationException.class, () -> achievementService.createAchievement(request));
        }

        @Test
        @DisplayName("should reject an unknown metric")
        void shouldRejectUnknownMetric() throws Exception {
            AchievementCreateRequest request = new AchievementCreateRequest(
                "first-post", "First Post", null, 50,
                predicate("{\"metric\":\"nope\",\"op\":\"gte\",\"value\":1}"));
            when(achievementRepository.findBySlug("first-post")).thenReturn(Optional.empty());
            assertThrows(ValidationException.class, () -> achievementService.createAchievement(request));
        }

        @Test
        @DisplayName("should reject a null predicate")
        void shouldRejectNullPredicate() {
            AchievementCreateRequest request = new AchievementCreateRequest("first-post", "First Post", null, 50, null);
            when(achievementRepository.findBySlug("first-post")).thenReturn(Optional.empty());
            assertThrows(ValidationException.class, () -> achievementService.createAchievement(request));
        }
    }

    @Nested
    @DisplayName("updateAchievement")
    class UpdateAchievementTests {
        @Test
        @DisplayName("should update achievement fields")
        void shouldUpdateAchievementFields() {
            UUID id = UUID.randomUUID();
            AchievementUpdateRequest request = new AchievementUpdateRequest("Updated Name", "Updated Description", 100, null, true);
            Achievement existing = built("first-post", "First Post", 50, null);
            existing.setId(id);
            existing.setActive(false);
            when(achievementRepository.findById(id)).thenReturn(Optional.of(existing));
            when(achievementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AchievementResponse response = achievementService.updateAchievement(id, request);

            assertEquals("Updated Name", response.name());
            assertEquals("Updated Description", response.description());
            assertEquals(100, response.xpReward());
            assertTrue(response.active());
            verify(achievementRepository).save(any());
        }

        @Test
        @DisplayName("should reject non-positive xp reward")
        void shouldRejectNonPositiveXp() {
            UUID id = UUID.randomUUID();
            AchievementUpdateRequest request = new AchievementUpdateRequest(null, null, 0, null, null);
            Achievement a = built("first-post", "First Post", 50, null);
            a.setId(id);
            when(achievementRepository.findById(id)).thenReturn(Optional.of(a));
            assertThrows(ValidationException.class, () -> achievementService.updateAchievement(id, request));
        }

        @Test
        @DisplayName("should throw when achievement not found on update")
        void shouldThrowWhenNotFoundOnUpdate() {
            UUID id = UUID.randomUUID();
            when(achievementRepository.findById(id)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                () -> achievementService.updateAchievement(id, new AchievementUpdateRequest(null, null, null, null, null)));
        }
    }

    @Nested
    @DisplayName("deleteAchievement")
    class DeleteAchievementTests {
        @Test
        @DisplayName("should delete when it exists")
        void shouldDeleteWhenExists() {
            UUID id = UUID.randomUUID();
            when(achievementRepository.existsById(id)).thenReturn(true);
            assertDoesNotThrow(() -> achievementService.deleteAchievement(id));
            verify(achievementRepository).deleteById(id);
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(achievementRepository.existsById(id)).thenReturn(false);
            assertThrows(NotFoundException.class, () -> achievementService.deleteAchievement(id));
        }
    }

    @Nested
    @DisplayName("getAchievement / getAchievementBySlug")
    class GetAchievementTests {
        @Test
        @DisplayName("should return achievement by id")
        void shouldReturnById() {
            UUID id = UUID.randomUUID();
            Achievement a = built("first-post", "First Post", 50, null);
            a.setId(id);
            when(achievementRepository.findById(id)).thenReturn(Optional.of(a));
            AchievementResponse response = achievementService.getAchievement(id);
            assertEquals(id, response.id());
            assertEquals("first-post", response.slug());
        }

        @Test
        @DisplayName("should throw when not found by id")
        void shouldThrowByIdNotFound() {
            UUID id = UUID.randomUUID();
            when(achievementRepository.findById(id)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> achievementService.getAchievement(id));
        }

        @Test
        @DisplayName("should return achievement by slug")
        void shouldReturnBySlug() {
            when(achievementRepository.findBySlug("first-post")).thenReturn(Optional.of(built("first-post", "First Post", 50, null)));
            assertEquals("First Post", achievementService.getAchievementBySlug("first-post").name());
        }

        @Test
        @DisplayName("should throw when not found by slug")
        void shouldThrowBySlugNotFound() {
            when(achievementRepository.findBySlug("nope")).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> achievementService.getAchievementBySlug("nope"));
        }
    }

    @Nested
    @DisplayName("listAchievements")
    class ListAchievementsTests {
        @Test
        @DisplayName("should return all when active is null")
        void shouldReturnAllWhenActiveNull() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            when(achievementRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(built("a", "A", 1, null), built("b", "B", 2, null)), pageable, 2));
            assertEquals(2, achievementService.listAchievements(null, pageable).getContent().size());
        }

        @Test
        @DisplayName("should filter by active")
        void shouldFilterByActive() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            when(achievementRepository.findByActiveOrderByCreatedAtDesc(true, pageable))
                .thenReturn(new PageImpl<>(List.of(built("a", "A", 1, null)), pageable, 1));
            assertEquals(1, achievementService.listAchievements(true, pageable).getContent().size());
            when(achievementRepository.findByActiveOrderByCreatedAtDesc(false, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
            assertEquals(0, achievementService.listAchievements(false, pageable).getContent().size());
        }
    }

    @Nested
    @DisplayName("user achievements")
    class UserAchievementTests {
        @Test
        @DisplayName("progress view lists active achievements with live progress and completion state")
        void shouldReturnAchievementProgress() {
            UUID userId = UUID.randomUUID();
            Achievement a = built("first-post", "First Post", 50,
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":5}");
            UserAchievement ua = UserAchievement.builder()
                .id(UUID.randomUUID()).achievement(a).userId(userId)
                .completed(true).completedAt(Instant.now()).xpAwarded(50L).build();
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            when(achievementRepository.findByActiveTrue(pageable)).thenReturn(new PageImpl<>(List.of(a), pageable, 1));
            when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(ua));

            Page<AchievementProgressResponse> page = achievementService.getUserAchievementProgress(userId, pageable);

            assertEquals(1, page.getContent().size());
            AchievementProgressResponse r = page.getContent().get(0);
            assertEquals("first-post", r.slug());
            assertTrue(r.completed());
            assertEquals(50L, r.xpAwarded());
            assertNotNull(r.completedAt());
            assertNotNull(r.progress());
            assertEquals("CONDITION", r.progress().type());
            assertEquals("posts", r.progress().metric());
            assertEquals(5L, r.progress().current());
            assertEquals(5L, r.progress().target());
            assertEquals("gte", r.progress().operator());
            assertTrue(r.progress().satisfied());
        }

        @Test
        @DisplayName("progress view reports uncompleted achievements with live current value")
        void shouldReportInProgressAchievement() {
            UUID userId = UUID.randomUUID();
            Achievement a = built("five-posts", "Five Posts", 20,
                "{\"metric\":\"posts\",\"op\":\"gte\",\"value\":10}");
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            when(achievementRepository.findByActiveTrue(pageable)).thenReturn(new PageImpl<>(List.of(a), pageable, 1));
            when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of());

            Page<AchievementProgressResponse> page = achievementService.getUserAchievementProgress(userId, pageable);

            assertEquals(1, page.getContent().size());
            AchievementProgressResponse r = page.getContent().get(0);
            assertFalse(r.completed());
            assertNull(r.completedAt());
            assertNull(r.xpAwarded());
            assertEquals(5L, r.progress().current());
            assertEquals(10L, r.progress().target());
            assertFalse(r.progress().satisfied());
        }

        @Test
        @DisplayName("should return only completed")
        void shouldReturnOnlyCompleted() {
            UUID userId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 20, Sort.by("completedAt").descending());
            when(userAchievementRepository.findByUserIdAndCompletedTrue(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
            assertTrue(achievementService.getCompletedAchievements(userId, pageable).getContent().isEmpty());
        }
    }

    @Nested
    @DisplayName("preview")
    class PreviewTests {
        @Test
        @DisplayName("should report satisfied with metric values")
        void shouldReportSatisfied() throws Exception {
            AchievementPreviewRequest request = new AchievementPreviewRequest(UUID.randomUUID(),
                predicate("{\"metric\":\"posts\",\"op\":\"gte\",\"value\":5}"));
            AchievementPreviewResponse response = achievementService.preview(request);
            assertTrue(response.satisfied());
            assertEquals(5L, response.metricValues().get("posts"));
        }

        @Test
        @DisplayName("should report not satisfied")
        void shouldReportNotSatisfied() throws Exception {
            AchievementPreviewRequest request = new AchievementPreviewRequest(UUID.randomUUID(),
                predicate("{\"metric\":\"posts\",\"op\":\"gte\",\"value\":10}"));
            assertFalse(achievementService.preview(request).satisfied());
        }
    }
}
