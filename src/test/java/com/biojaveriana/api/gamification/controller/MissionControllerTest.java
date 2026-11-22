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
import com.biojaveriana.api.gamification.dto.MissionResponse;
import com.biojaveriana.api.gamification.dto.MyMissionsResponse;
import com.biojaveriana.api.gamification.dto.UserGamificationResponse;
import com.biojaveriana.api.gamification.dto.UserMissionResponse;
import com.biojaveriana.api.gamification.model.MissionStatus;
import com.biojaveriana.api.gamification.model.MissionType;
import com.biojaveriana.api.gamification.service.GamificationService;
import com.biojaveriana.api.gamification.service.MissionEngine;
import com.biojaveriana.api.gamification.service.MissionService;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@WebMvcTest(MissionController.class)
@Import(SecuritySliceConfiguration.class)
class MissionControllerTest {

    @MockitoBean
    private MissionService missionService;

    @MockitoBean
    private MissionEngine missionEngine;

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

    private MissionResponse missionResponse(UUID id) {
        return new MissionResponse(id, "daily-walk", "Daily Walk", "Walk 10 minutes", 5,
            MissionType.DAILY, null, null, null, true, null, null);
    }

    private UserMissionResponse userMissionResponse(UUID id) {
        return new UserMissionResponse(id, "daily-walk", "Daily Walk", "Walk 10 minutes", 5,
            MissionType.DAILY, null, null, MissionStatus.ACTIVE, null, null, null);
    }

    @Test
    @DisplayName("POST /missions creates a mission")
    void createMission() throws Exception {
        authenticate(admin);
        UUID missionId = UUID.randomUUID();
        when(missionService.createMission(any())).thenReturn(missionResponse(missionId));

        mockMvc.perform(post("/api/v1/missions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\": \"daily-walk\", \"name\": \"Daily Walk\", \"xpReward\": 5, \"type\": \"DAILY\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("daily-walk"))
            .andExpect(jsonPath("$.type").value("DAILY"));
    }

    @Test
    @DisplayName("PUT /missions/{id} updates a mission")
    void updateMission() throws Exception {
        authenticate(admin);
        UUID missionId = UUID.randomUUID();
        when(missionService.updateMission(eq(missionId), any())).thenReturn(missionResponse(missionId));

        mockMvc.perform(put("/api/v1/missions/" + missionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Renamed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("daily-walk"));
    }

    @Test
    @DisplayName("DELETE /missions/{id} deletes a mission")
    void deleteMission() throws Exception {
        authenticate(admin);
        UUID missionId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/missions/" + missionId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /missions/{id} returns a mission")
    void getMission() throws Exception {
        authenticate(user);
        UUID missionId = UUID.randomUUID();
        when(missionService.getMission(missionId)).thenReturn(missionResponse(missionId));

        mockMvc.perform(get("/api/v1/missions/" + missionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.xpReward").value(5));
    }

    @Test
    @DisplayName("GET /missions/slug/{slug} returns a mission by slug")
    void getMissionBySlug() throws Exception {
        authenticate(user);
        UUID missionId = UUID.randomUUID();
        when(missionService.getMissionBySlug("daily-walk")).thenReturn(missionResponse(missionId));

        mockMvc.perform(get("/api/v1/missions/slug/daily-walk"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Daily Walk"));
    }

    @Test
    @DisplayName("GET /missions lists missions")
    void listMissions() throws Exception {
        authenticate(user);
        UUID missionId = UUID.randomUUID();
        when(missionService.listMissions(isNull(), isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(missionResponse(missionId))));

        mockMvc.perform(get("/api/v1/missions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].slug").value("daily-walk"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /missions/me returns the user's mission board")
    void getMyMissions() throws Exception {
        authenticate(user);
        UUID missionId = UUID.randomUUID();
        when(missionService.getMyMissions(user.getId())).thenReturn(
            new MyMissionsResponse(List.of(userMissionResponse(missionId)), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/missions/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.daily[0].slug").value("daily-walk"))
            .andExpect(jsonPath("$.daily[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /missions/evaluate force-evaluates a user's missions")
    void evaluate() throws Exception {
        authenticate(admin);
        when(gamificationService.getUserGamification(user.getId())).thenReturn(
            new UserGamificationResponse(user.getId(), "jane", 500L, 4, 400L, 600L, 50.0));

        mockMvc.perform(post("/api/v1/missions/evaluate").param("userId", user.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalXp").value(500));
    }
}
