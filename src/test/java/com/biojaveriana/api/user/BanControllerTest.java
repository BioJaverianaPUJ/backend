package com.biojaveriana.api.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

@WebMvcTest(BanController.class)
@Import(SecuritySliceConfiguration.class)
class BanControllerTest {

    @MockitoBean
    private BanService banService;

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    private User admin;
    private User target;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(UUID.randomUUID()).username("admin").role(Role.ADMIN).build();
        target = User.builder().id(UUID.randomUUID()).username("jane").role(Role.USER).build();
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

    private Ban ban() {
        return Ban.builder()
            .id(UUID.randomUUID())
            .user(target)
            .reason("spam")
            .banType(BanType.TEMPORAL)
            .status(BanStatus.ACTIVE)
            .expiresAt(Instant.now().plusSeconds(3600))
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("POST /bans creates a temporal ban")
    void createTemporalBan() throws Exception {
        authenticate(admin);
        when(banService.createTemporalBan(eq(target.getId()), eq("spam"), any(Instant.class), eq(admin.getId())))
            .thenReturn(ban());

        mockMvc.perform(post("/api/v1/bans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": "%s", "reason": "spam", "banType": "TEMPORAL",
                     "expiresAt": "2026-12-31T00:00:00Z"}
                    """.formatted(target.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reason").value("spam"))
            .andExpect(jsonPath("$.banType").value("TEMPORAL"))
            .andExpect(jsonPath("$.username").value("jane"));
    }

    @Test
    @DisplayName("POST /bans creates a permanent ban")
    void createPermanentBan() throws Exception {
        authenticate(admin);
        Ban permanent = Ban.builder()
            .id(UUID.randomUUID())
            .user(target)
            .reason("toxicity")
            .banType(BanType.PERMANENT)
            .status(BanStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();
        when(banService.createPermanentBan(eq(target.getId()), eq("toxicity"), eq(admin.getId())))
            .thenReturn(permanent);

        mockMvc.perform(post("/api/v1/bans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": "%s", "reason": "toxicity", "banType": "PERMANENT"}
                    """.formatted(target.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.banType").value("PERMANENT"));
    }

    @Test
    @DisplayName("POST /bans returns 400 without a ban type")
    void createBanValidationFails() throws Exception {
        authenticate(admin);

        mockMvc.perform(post("/api/v1/bans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": "%s", "reason": "spam"}
                    """.formatted(target.getId())))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /bans/{id}/cancel cancels a ban")
    void cancelBan() throws Exception {
        authenticate(admin);
        UUID banId = UUID.randomUUID();
        Ban cancelled = Ban.builder()
            .id(banId)
            .user(target)
            .reason("spam")
            .banType(BanType.TEMPORAL)
            .status(BanStatus.CANCELLED)
            .createdAt(Instant.now())
            .build();
        when(banService.cancelBan(banId)).thenReturn(cancelled);

        mockMvc.perform(patch("/api/v1/bans/" + banId + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /bans lists all bans")
    void findAllBans() throws Exception {
        authenticate(admin);
        when(banService.findAllBansPageable(any()))
            .thenReturn(new PageImpl<>(List.of(ban())));

        mockMvc.perform(get("/api/v1/bans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].reason").value("spam"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /bans/user/{userId} lists a user's bans")
    void findBansByUser() throws Exception {
        authenticate(admin);
        when(banService.findBansByUserIdPageable(eq(target.getId()), any()))
            .thenReturn(new PageImpl<>(List.of(ban())));

        mockMvc.perform(get("/api/v1/bans/user/" + target.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].username").value("jane"));
    }
}
