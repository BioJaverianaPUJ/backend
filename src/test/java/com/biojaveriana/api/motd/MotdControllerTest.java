package com.biojaveriana.api.motd;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.biojaveriana.api.SecuritySliceConfiguration;
import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.motd.dto.FunFactListResponse;
import com.biojaveriana.api.motd.dto.FunFactResponse;
import com.biojaveriana.api.motd.dto.FunFactSummaryResponse;
import com.biojaveriana.api.motd.dto.MotdResponse;
import com.biojaveriana.api.motd.dto.UserSummaryResponse;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@WebMvcTest(MotdController.class)
@Import(SecuritySliceConfiguration.class)
class MotdControllerTest {

    @MockitoBean
    private FunFactService funFactService;

    @MockitoBean
    private MotdService motdService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BanService banService;

    @Autowired
    private MockMvc mockMvc;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(UUID.randomUUID()).username("admin").role(Role.ADMIN).build();
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

    private FunFactResponse funFactResponse(UUID id) {
        UserSummaryResponse author =
            new UserSummaryResponse(admin.getId(), "admin", "Ann", "Admin", null);
        return new FunFactResponse(id, author, "Fun title", "Fun description", null, List.of(), false, null, null);
    }

    @Test
    @DisplayName("GET /motd/today returns the featured fun fact")
    void today() throws Exception {
        when(motdService.today()).thenReturn(
            Optional.of(new MotdResponse(LocalDate.now(), Instant.now(), funFactResponse(UUID.randomUUID()))));

        mockMvc.perform(get("/api/v1/motd/today"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.funFact.title").value("Fun title"));
    }

    @Test
    @DisplayName("GET /motd/today returns an empty body when nothing is featured")
    void todayEmpty() throws Exception {
        when(motdService.today()).thenReturn(Optional.empty());

        String body = mockMvc.perform(get("/api/v1/motd/today"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertTrue(body.isBlank() || body.trim().equals("null"));
    }

    @Test
    @DisplayName("POST /motd/funfacts/presign returns an upload presign for admins")
    void createPresign() throws Exception {
        authenticate(admin);
        Instant expiresAt = Instant.now().plusSeconds(300);
        when(funFactService.createPresign(eq(admin), eq("image/png")))
            .thenReturn(new UploadPresign("funfacts/key.png", "https://example.com/upload", "PUT",
                "image/png", expiresAt));

        mockMvc.perform(post("/api/v1/motd/funfacts/presign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentType\": \"image/png\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("funfacts/key.png"))
            .andExpect(jsonPath("$.method").value("PUT"));
    }

    @Test
    @DisplayName("POST /motd/funfacts creates a fun fact for admins")
    void createFunFact() throws Exception {
        authenticate(admin);
        UUID funFactId = UUID.randomUUID();
        when(funFactService.create(eq(admin), any())).thenReturn(funFactResponse(funFactId));

        mockMvc.perform(post("/api/v1/motd/funfacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Fun title\", \"description\": \"Fun description\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Fun title"));
    }

    @Test
    @DisplayName("POST /motd/funfacts returns 400 without a title")
    void createFunFactValidationFails() throws Exception {
        authenticate(admin);

        mockMvc.perform(post("/api/v1/motd/funfacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\": \"Fun description\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /motd/funfacts/{id} updates a fun fact")
    void updateFunFact() throws Exception {
        authenticate(admin);
        UUID funFactId = UUID.randomUUID();
        when(funFactService.update(eq(funFactId), eq(admin), any())).thenReturn(funFactResponse(funFactId));

        mockMvc.perform(put("/api/v1/motd/funfacts/" + funFactId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"New title\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Fun title"));
    }

    @Test
    @DisplayName("DELETE /motd/funfacts/{id} deletes a fun fact")
    void deleteFunFact() throws Exception {
        authenticate(admin);
        UUID funFactId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/motd/funfacts/" + funFactId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /motd/funfacts/{id} returns a fun fact")
    void getFunFactById() throws Exception {
        authenticate(admin);
        UUID funFactId = UUID.randomUUID();
        when(funFactService.getById(funFactId)).thenReturn(funFactResponse(funFactId));

        mockMvc.perform(get("/api/v1/motd/funfacts/" + funFactId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Fun title"))
            .andExpect(jsonPath("$.author.username").value("admin"));
    }

    @Test
    @DisplayName("GET /motd/funfacts lists all fun facts")
    void listFunFacts() throws Exception {
        authenticate(admin);
        FunFactSummaryResponse summary =
            new FunFactSummaryResponse(UUID.randomUUID(), new UserSummaryResponse(admin.getId(),
                "admin", "Ann", "Admin", null), "Fun title", null, null);
        when(funFactService.list(0, 20))
            .thenReturn(new FunFactListResponse(List.of(summary), new Pagination(0, 20, 1, 1, false, false)));

        mockMvc.perform(get("/api/v1/motd/funfacts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("Fun title"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT /motd/funfacts/{id}/toggle-hidden toggles visibility")
    void toggleHidden() throws Exception {
        authenticate(admin);
        UUID funFactId = UUID.randomUUID();
        when(funFactService.toggleHidden(eq(funFactId), eq(admin))).thenReturn(funFactResponse(funFactId));

        mockMvc.perform(put("/api/v1/motd/funfacts/" + funFactId + "/toggle-hidden"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Fun title"));
    }
}
