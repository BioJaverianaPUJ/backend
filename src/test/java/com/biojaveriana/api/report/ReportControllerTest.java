package com.biojaveriana.api.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import com.biojaveriana.api.report.dto.ReportCreateRequest;
import com.biojaveriana.api.report.dto.ReportListResponse;
import com.biojaveriana.api.report.dto.ReportResponse;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@WebMvcTest(ReportController.class)
@Import(SecuritySliceConfiguration.class)
class ReportControllerTest {

    @MockitoBean
    private ReportService reportService;

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

    private ReportResponse reportResponse() {
        return new ReportResponse(UUID.randomUUID(), "jane", "Jane Doe", "bob", "Bob",
            ReportType.POST, "post-id", null, "spam", ReportStatus.PENDING, null, null);
    }

    private ReportResponse reportResponse(ReportStatus status) {
        return new ReportResponse(UUID.randomUUID(), "jane", "Jane Doe", "bob", "Bob",
            ReportType.POST, "post-id", null, "spam", status, null, null);
    }

    private ReportListResponse reportList() {
        return new ReportListResponse(List.of(), new Pagination(0, 10, 0, 0, true, true));
    }

    @Test
    @DisplayName("POST /reports creates a report with the authenticated reporter")
    void createReport() throws Exception {
        authenticate(user);
        when(reportService.createReport(any(), any(ReportCreateRequest.class))).thenReturn(reportResponse());

        mockMvc.perform(post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reportType": "POST", "reportedPostId": "some-post-id", "reason": "spam"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reason").value("spam"))
            .andExpect(jsonPath("$.reporterUsername").value("jane"));
    }

    @Test
    @DisplayName("POST /reports returns 400 without a report type")
    void createReportValidationFails() throws Exception {
        authenticate(user);

        mockMvc.perform(post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason": "spam"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /reports/{id} returns the report")
    void getReport() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();
        when(reportService.getReport(id)).thenReturn(reportResponse());

        mockMvc.perform(get("/api/v1/reports/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /reports/by-reported-user is admin-only")
    void getReportsByReportedUserForbiddenForUser() throws Exception {
        authenticate(user);

        mockMvc.perform(get("/api/v1/reports/by-reported-user").param("reportedUserId", userId.toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /reports/by-reported-user works for admins")
    void getReportsByReportedUserAsAdmin() throws Exception {
        authenticate(admin);
        when(reportService.getReportsByReportedUser(userId, 0, 10)).thenReturn(reportList());

        mockMvc.perform(get("/api/v1/reports/by-reported-user").param("reportedUserId", userId.toString()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /reports/admin/pending is forbidden for regular users")
    void pendingReportsForbiddenForUser() throws Exception {
        authenticate(user);

        mockMvc.perform(get("/api/v1/reports/admin/pending"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /reports/{id}/status is forbidden for regular users")
    void updateStatusForbiddenForUser() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/reports/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status": "RESOLVED"}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /reports/{id}/status updates the status for admins")
    void updateStatusAsAdmin() throws Exception {
        authenticate(admin);
        UUID id = UUID.randomUUID();
        when(reportService.updateReportStatus(id, null, "RESOLVED")).thenReturn(reportResponse(ReportStatus.RESOLVED));

        mockMvc.perform(put("/api/v1/reports/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status": "RESOLVED"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("admin report endpoints require authentication")
    void adminEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/reports/admin/all"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /reports/by-reporter lists the caller's reports")
    void getReportsByReporter() throws Exception {
        authenticate(user);
        when(reportService.getReportsByReporter(user, 1, 20)).thenReturn(reportList());

        mockMvc.perform(get("/api/v1/reports/by-reporter").param("page", "1").param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());
    }
}
