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

@WebMvcTest(SpeciesController.class)
@Import(SecuritySliceConfiguration.class)
class SpeciesControllerTest {

    @MockitoBean
    private SpeciesService speciesService;

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

    private Species species(UUID id) {
        return Species.builder()
            .id(id)
            .scientificName("Lobophora lasiophylla")
            .commonName("Copoaz\u00e1")
            .family("Arecaceae")
            .woodDensity(0.55)
            .build();
    }

    @Test
    @DisplayName("GET /species lists the catalogue")
    void findAll() throws Exception {
        authenticate(user);
        UUID speciesId = UUID.randomUUID();
        when(speciesService.findAll(any()))
            .thenReturn(new PageImpl<>(List.of(species(speciesId))));

        mockMvc.perform(get("/api/v1/species"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].scientificName").value("Lobophora lasiophylla"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /species/{id} returns a species")
    void getSpeciesById() throws Exception {
        authenticate(user);
        UUID speciesId = UUID.randomUUID();
        when(speciesService.findById(speciesId)).thenReturn(species(speciesId));

        mockMvc.perform(get("/api/v1/species/" + speciesId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.family").value("Arecaceae"))
            .andExpect(jsonPath("$.woodDensity").value(0.55));
    }

    @Test
    @DisplayName("POST /species creates a species for admins")
    void create() throws Exception {
        authenticate(admin);
        when(speciesService.create(any())).thenReturn(species(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/species")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"scientificName": "Lobophora lasiophylla", "commonName": "Copoaz\u00e1",
                     "family": "Arecaceae", "woodDensity": 0.55}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.scientificName").value("Lobophora lasiophylla"));
    }

    @Test
    @DisplayName("PATCH /species/{id} updates a species for admins")
    void updateSpecies() throws Exception {
        authenticate(admin);
        UUID speciesId = UUID.randomUUID();
        when(speciesService.update(eq(speciesId), any())).thenReturn(species(speciesId));

        mockMvc.perform(patch("/api/v1/species/" + speciesId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"commonName\": \"Updated\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scientificName").value("Lobophora lasiophylla"));
    }

    @Test
    @DisplayName("DELETE /species/{id} deletes a species")
    void deleteSpecies() throws Exception {
        authenticate(admin);
        UUID speciesId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/species/" + speciesId))
            .andExpect(status().isNoContent());
    }
}
