package com.biojaveriana.api.poi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.biojaveriana.api.SecuritySliceConfiguration;
import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.poi.dto.PoiListResponse;
import com.biojaveriana.api.poi.dto.PoiResponse;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@WebMvcTest(PoiController.class)
@Import(SecuritySliceConfiguration.class)
class PoiControllerTest {

    @MockitoBean
    private PoiService poiService;

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

    private PoiResponse poiResponse(UUID id) {
        return new PoiResponse(id, admin.getId(), "Green Zone", "A quiet zone", PoiType.POINT,
            PoiZoneType.GREEN_ZONE, null, 4.6, -74.0, 100.0, "PUBLIC", true, null, null);
    }

    @Test
    @DisplayName("POST /pois creates a POI for admins")
    void createPoi() throws Exception {
        authenticate(admin);
        UUID poiId = UUID.randomUUID();
        when(poiService.createPoi(eq(admin), any())).thenReturn(poiResponse(poiId));

        mockMvc.perform(post("/api/v1/pois")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Green Zone", "geometryType": "POINT", "type": "GREEN_ZONE",
                     "centerLatitude": 4.6, "centerLongitude": -74.0, "radiusMeters": 100.0,
                     "visibility": "PUBLIC"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Green Zone"))
            .andExpect(jsonPath("$.geometryType").value("POINT"));
    }

    @Test
    @DisplayName("POST /pois returns 400 without a name or geometry")
    void createPoiValidationFails() throws Exception {
        authenticate(admin);

        mockMvc.perform(post("/api/v1/pois")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /pois/{id} updates a POI")
    void updatePoi() throws Exception {
        authenticate(admin);
        UUID poiId = UUID.randomUUID();
        when(poiService.updatePoi(eq(poiId), eq(admin), any())).thenReturn(poiResponse(poiId));

        mockMvc.perform(put("/api/v1/pois/" + poiId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Renamed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Green Zone"));
    }

    @Test
    @DisplayName("DELETE /pois/{id} deletes a POI")
    void deletePoi() throws Exception {
        authenticate(admin);
        UUID poiId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/pois/" + poiId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /pois/{id} returns a POI")
    void getPoi() throws Exception {
        authenticate(user);
        UUID poiId = UUID.randomUUID();
        when(poiService.getPoi(eq(poiId), eq(user))).thenReturn(poiResponse(poiId));

        mockMvc.perform(get("/api/v1/pois/" + poiId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Green Zone"))
            .andExpect(jsonPath("$.type").value("GREEN_ZONE"));
    }

    @Test
    @DisplayName("GET /pois lists POIs")
    void listPois() throws Exception {
        authenticate(user);
        UUID poiId = UUID.randomUUID();
        when(poiService.listPois(eq(user), eq(0), eq(20)))
            .thenReturn(new PoiListResponse(List.of(poiResponse(poiId)), new Pagination(0, 20, 1, 1, false, false)));

        mockMvc.perform(get("/api/v1/pois"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Green Zone"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /pois/telemetry records a position and returns nearby POIs")
    void recordTelemetry() throws Exception {
        authenticate(user);
        UUID poiId = UUID.randomUUID();
        when(poiService.recordTelemetry(eq(user), any())).thenReturn(List.of(poiResponse(poiId)));

        mockMvc.perform(post("/api/v1/pois/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\": 4.6, \"longitude\": -74.0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Green Zone"));
    }

    @Test
    @DisplayName("POST /pois/telemetry returns 400 for out-of-range coordinates")
    void recordTelemetryValidationFails() throws Exception {
        authenticate(user);

        mockMvc.perform(post("/api/v1/pois/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\": 95.0, \"longitude\": 0.0}"))
            .andExpect(status().isBadRequest());
    }
}
