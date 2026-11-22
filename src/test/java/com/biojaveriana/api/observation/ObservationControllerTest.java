package com.biojaveriana.api.observation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.biojaveriana.api.SecuritySliceConfiguration;
import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.observation.dto.ObservationJobStatusResponse;
import com.biojaveriana.api.observation.dto.ObservationListResponse;
import com.biojaveriana.api.observation.dto.ObservationResponse;
import com.biojaveriana.api.observation.dto.ObservationUploadPresignRequest;
import com.biojaveriana.api.observation.exceptions.ObservationForbiddenException;
import com.biojaveriana.api.observation.exceptions.ObservationRateLimitException;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@WebMvcTest(ObservationController.class)
@Import(SecuritySliceConfiguration.class)
class ObservationControllerTest {

    @MockitoBean
    private ObservationService observationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BanService banService;

    @Autowired
    private MockMvc mockMvc;

    private User user;
    private User admin;
    private UUID userId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).username("jane").role(Role.USER).build();
        adminId = UUID.randomUUID();
        admin = User.builder().id(adminId).username("admin").role(Role.ADMIN).build();
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

    private ObservationListResponse observationList() {
        return new ObservationListResponse(List.of(), new Pagination(0, 20, 0, 0, true, true));
    }

    @Test
    @DisplayName("POST /observations/upload-presign returns a presigned upload URL")
    void createPresign() throws Exception {
        authenticate(user);
        UploadPresign presign = new UploadPresign("observations/" + userId + "/abc.jpg", "https://s3.example.com/signed",
            "PUT", "image/jpeg", Instant.now().plusSeconds(300));
        when(observationService.createPresign(eq(user), any(ObservationUploadPresignRequest.class))).thenReturn(presign);

        mockMvc.perform(post("/api/v1/observations/upload-presign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"contentType": "image/jpeg"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value(presign.key()))
            .andExpect(jsonPath("$.method").value("PUT"));
    }

    @Test
    @DisplayName("POST /observations creates an observation job")
    void createObservation() throws Exception {
        authenticate(user);
        ObservationJobStatusResponse response = ObservationJobStatusResponse.builder()
            .jobId(UUID.randomUUID()).status(JobStatus.PENDING).retryCount(0).build();
        when(observationService.createObservation(any(), eq(user))).thenReturn(response);

        mockMvc.perform(post("/api/v1/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"referencePictureKey": "observations/key.jpg", "latitude": 1.0, "longitude": 2.0}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /observations returns 400 for out-of-range coordinates")
    void createObservationValidationFails() throws Exception {
        authenticate(user);

        mockMvc.perform(post("/api/v1/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"referencePictureKey": "key", "latitude": 121.0, "longitude": 2.0}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /observations maps the rate limit to 429")
    void createObservationRateLimited() throws Exception {
        authenticate(user);
        when(observationService.createObservation(any(), eq(user)))
            .thenThrow(new ObservationRateLimitException(300));

        mockMvc.perform(post("/api/v1/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"referencePictureKey": "key", "latitude": 1.0, "longitude": 2.0}
                    """))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("OBSERVATION_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("GET /observations/{id} returns the observation to its owner")
    void getObservation() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();
        when(observationService.getObservation(id, user)).thenReturn(
            ObservationResponse.builder().id(id).speciesCommonName("Oak").build());

        mockMvc.perform(get("/api/v1/observations/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.speciesCommonName").value("Oak"));
    }

    @Test
    @DisplayName("GET /observations/{id} maps the forbidden exception to 403")
    void getObservationForbidden() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();
        when(observationService.getObservation(id, user)).thenThrow(new ObservationForbiddenException());

        mockMvc.perform(get("/api/v1/observations/" + id))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /observations lists the caller's observations")
    void listObservations() throws Exception {
        authenticate(user);
        when(observationService.listObservations(user, 0, 20)).thenReturn(observationList());

        mockMvc.perform(get("/api/v1/observations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("GET /observations/admin is forbidden for regular users")
    void adminListForbiddenForUser() throws Exception {
        authenticate(user);

        mockMvc.perform(get("/api/v1/observations/admin"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /observations/admin works for admins")
    void adminListAsAdmin() throws Exception {
        authenticate(admin);
        when(observationService.listAllForAdmin(admin, false, false, 0, 20)).thenReturn(observationList());

        mockMvc.perform(get("/api/v1/observations/admin"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /observations/{id}/revision-request flags the observation and returns it")
    void requestRevision() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();
        ObservationResponse response = ObservationResponse.builder().id(id).revisionRequested(true).build();
        when(observationService.getObservation(id, user)).thenReturn(response);

        mockMvc.perform(post("/api/v1/observations/" + id + "/revision-request"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revisionRequested").value(true));

        verify(observationService).requestRevision(id, user);
    }

    @Test
    @DisplayName("GET /observations/{id}/job-status requires authentication")
    void jobStatusRequiresAuth() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/observations/" + id + "/job-status"))
            .andExpect(status().isUnauthorized());
    }
}
