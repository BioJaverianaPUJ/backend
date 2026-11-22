package com.biojaveriana.api.gamification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.biojaveriana.api.SecuritySliceConfiguration;
import com.biojaveriana.api.gamification.dto.AchievementListResponse;
import com.biojaveriana.api.gamification.dto.AchievementPreviewResponse;
import com.biojaveriana.api.gamification.dto.AchievementResponse;
import com.biojaveriana.api.gamification.dto.UserGamificationResponse;
import com.biojaveriana.api.gamification.service.AchievementEngine;
import com.biojaveriana.api.gamification.service.AchievementService;
import com.biojaveriana.api.gamification.service.GamificationService;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@WebMvcTest(AchievementController.class)
@Import(SecuritySliceConfiguration.class)
class AchievementControllerTest {

    @MockitoBean
    private AchievementService achievementService;

    @MockitoBean
    private AchievementEngine achievementEngine;

    @MockitoBean
    private GamificationService gamificationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BanService banService;

    @Autowired
    private MockMvc mockMvc;

    private User admin;
    private User user;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(UUID.randomUUID()).username("admin").role(Role.ADMIN).build();
        user = User.builder().id(UUID.randomUUID()).username("jane").role(Role.USER).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(User caller) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(caller.getId().toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, "N/A", List.of()));
        when(userService.findById(caller.getId())).thenReturn(caller);
    }

    private AchievementResponse achievementResponse(UUID id) {
        return new AchievementResponse(id, "first-post", "First Post", "Post your first observation",
            10, null, true, null, null);
    }

    @Test
    @DisplayName("POST /achievements creates an achievement")
    void createAchievement() throws Exception {
        authenticate(admin);
        UUID achievementId = UUID.randomUUID();
        when(achievementService.createAchievement(any())).thenReturn(achievementResponse(achievementId));

        mockMvc.perform(post("/api/v1/achievements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\": \"first-post\", \"name\": \"First Post\", \"xpReward\": 10}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("first-post"))
            .andExpect(jsonPath("$.name").value("First Post"));
    }

    @Test
    @DisplayName("PUT /achievements/{id} updates an achievement")
    void updateAchievement() throws Exception {
        authenticate(admin);
        UUID achievementId = UUID.randomUUID();
        when(achievementService.updateAchievement(eq(achievementId), any()))
            .thenReturn(achievementResponse(achievementId));

        mockMvc.perform(put("/api/v1/achievements/" + achievementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Renamed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("first-post"));
    }

    @Test
    @DisplayName("DELETE /achievements/{id} deletes an achievement")
    void deleteAchievement() throws Exception {
        authenticate(admin);
        UUID achievementId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/achievements/" + achievementId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /achievements/{id} returns an achievement")
    void getAchievement() throws Exception {
        authenticate(user);
        UUID achievementId = UUID.randomUUID();
        when(achievementService.getAchievement(achievementId)).thenReturn(achievementResponse(achievementId));

        mockMvc.perform(get("/api/v1/achievements/" + achievementId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.xpReward").value(10));
    }

    @Test
    @DisplayName("GET /achievements/slug/{slug} returns an achievement by slug")
    void getAchievementBySlug() throws Exception {
        authenticate(user);
        UUID achievementId = UUID.randomUUID();
        when(achievementService.getAchievementBySlug("first-post")).thenReturn(achievementResponse(achievementId));

        mockMvc.perform(get("/api/v1/achievements/slug/first-post"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("First Post"));
    }

    @Test
    @DisplayName("GET /achievements lists achievements")
    void listAchievements() throws Exception {
        authenticate(user);
        UUID achievementId = UUID.randomUUID();
        when(achievementService.listAchievements(isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(achievementResponse(achievementId))));

        mockMvc.perform(get("/api/v1/achievements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].slug").value("first-post"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /achievements/preview dry-runs a candidate predicate")
    void preview() throws Exception {
        authenticate(admin);
        when(achievementService.preview(any())).thenReturn(
            new AchievementPreviewResponse(true, Map.of("postsCreated", 3L)));

        mockMvc.perform(post("/api/v1/achievements/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"%s\"}".formatted(user.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.satisfied").value(true))
            .andExpect(jsonPath("$.metricValues.postsCreated").value(3));
    }

    @Test
    @DisplayName("POST /achievements/evaluate force-evaluates a user's achievements")
    void evaluate() throws Exception {
        authenticate(admin);
        when(gamificationService.getUserGamification(user.getId())).thenReturn(
            new UserGamificationResponse(user.getId(), "jane", 500L, 4, 400L, 600L, 50.0));

        mockMvc.perform(post("/api/v1/achievements/evaluate").param("userId", user.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalXp").value(500));
    }
}
