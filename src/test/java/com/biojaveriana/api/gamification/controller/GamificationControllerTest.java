package com.biojaveriana.api.gamification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.biojaveriana.api.SecuritySliceConfiguration;
import com.biojaveriana.api.gamification.dto.UserGamificationResponse;
import com.biojaveriana.api.gamification.service.GamificationService;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@WebMvcTest(GamificationController.class)
@Import(SecuritySliceConfiguration.class)
class GamificationControllerTest {

    @MockitoBean
    private GamificationService gamificationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BanService banService;

    @Autowired
    private MockMvc mockMvc;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).username("jane").role(Role.USER).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /gamification/me returns the authenticated user's gamification summary")
    void getMyGamification() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(user.getId().toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, "N/A", List.of()));
        when(userService.findById(user.getId())).thenReturn(user);
        when(gamificationService.getUserGamification(user.getId())).thenReturn(
            new UserGamificationResponse(user.getId(), "jane", 500L, 4, 400L, 600L, 50.0));

        mockMvc.perform(get("/api/v1/gamification/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalXp").value(500))
            .andExpect(jsonPath("$.level").value(4))
            .andExpect(jsonPath("$.username").value("jane"));
    }
}
