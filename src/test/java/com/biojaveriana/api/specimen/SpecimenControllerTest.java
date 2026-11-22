package com.biojaveriana.api.specimen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@WebMvcTest(SpecimenController.class)
@Import(SecuritySliceConfiguration.class)
class SpecimenControllerTest {

    @MockitoBean
    private SpecimenService specimenService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BanService banService;

    @Autowired
    private MockMvc mockMvc;

    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).username("jane").role(Role.USER).build();
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

    private Specimen specimen(UUID id) {
        Species species = Species.builder().id(UUID.randomUUID()).scientificName("Lobophora lasiophylla")
            .commonName("Copoaz\u00e1").family("Arecaceae").build();
        return Specimen.builder()
            .id(id)
            .species(species)
            .latitude(4.6)
            .longitude(-74.0)
            .circumference(1.2)
            .height(12.5)
            .healthStatus(HealthStatus.WELL)
            .notes("Healthy canopy")
            .observationDate(Instant.now())
            .build();
    }

    @Test
    @DisplayName("GET /specimens lists all specimens")
    void findAll() throws Exception {
        authenticate(user);
        UUID specimenId = UUID.randomUUID();
        when(specimenService.findAll(any()))
            .thenReturn(new PageImpl<>(List.of(specimen(specimenId))));

        mockMvc.perform(get("/api/v1/specimens"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].latitude").value(4.6))
            .andExpect(jsonPath("$.content[0].healthStatus").value("WELL"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /specimens/{id} returns a specimen")
    void getSpecimenById() throws Exception {
        authenticate(user);
        UUID specimenId = UUID.randomUUID();
        when(specimenService.findById(specimenId)).thenReturn(specimen(specimenId));

        mockMvc.perform(get("/api/v1/specimens/" + specimenId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notes").value("Healthy canopy"));
    }

    @Test
    @DisplayName("POST /specimens creates a specimen for admins")
    void create() throws Exception {
        authenticate(admin);
        when(specimenService.create(any())).thenReturn(specimen(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/specimens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"latitude": 4.6, "longitude": -74.0, "notes": "Healthy canopy"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.speciesId").exists());
    }

    @Test
    @DisplayName("PATCH /specimens/{id} updates a specimen for admins")
    void updateSpecimen() throws Exception {
        authenticate(admin);
        UUID specimenId = UUID.randomUUID();
        when(specimenService.update(eq(specimenId), any())).thenReturn(specimen(specimenId));

        mockMvc.perform(patch("/api/v1/specimens/" + specimenId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\": \"Updated\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.speciesId").exists());
    }

    @Test
    @DisplayName("DELETE /specimens/{id} deletes a specimen")
    void deleteSpecimen() throws Exception {
        authenticate(admin);
        UUID specimenId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/specimens/" + specimenId))
            .andExpect(status().isNoContent());
    }
}
