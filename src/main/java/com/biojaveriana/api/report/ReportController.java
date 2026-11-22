package com.biojaveriana.api.report;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.common.dto.ErrorResponse;
import com.biojaveriana.api.report.dto.ReportCreateRequest;
import com.biojaveriana.api.report.dto.ReportListResponse;
import com.biojaveriana.api.report.dto.ReportResponse;
import com.biojaveriana.api.report.dto.ReportUpdateStatusRequest;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Content reports: users flag posts or comments, admins triage them.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@Validated
@RequiredArgsConstructor
@Tag(name = "Reports", description = "User reports against posts or comments, plus admin triage " +
    "(per-user views, pending queue and status updates).")
public class ReportController {
    private final ReportService reportService;

    @Operation(summary = "Report a post or comment",
        description = "Creates a PENDING report. `reportType` must be `POST` (with `reportedPostId`) " +
            "or `COMMENT` (with `reportedCommentId`). Hidden or banned content cannot be reported. " +
            "A user can have only one PENDING report per piece of content.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Report created"),
        @ApiResponse(responseCode = "400", description = "Validation failed or invalid/missing report type",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Reported post or comment not found (or hidden)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "A pending report for this content already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @Parameter(hidden = true) @CurrentUser User reporter,
            @RequestBody @Valid ReportCreateRequest request) {
        log.info("Creating report by user id={}", reporter.getId());
        ReportResponse response = reportService.createReport(reporter, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a report by ID",
        description = "Fetches a single report with full details (reason and both users).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The report"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Report not found"),
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReport(
            @Parameter(description = "ID of the report", required = true) @PathVariable UUID id) {
        log.info("Getting report id={}", id);
        ReportResponse response = reportService.getReport(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "My submitted reports",
        description = "Paginated list of the reports submitted by the authenticated user, newest first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of reports"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping("/by-reporter")
    public ResponseEntity<ReportListResponse> getReportsByReporter(
            @Parameter(hidden = true) @CurrentUser User reporter,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "10")
            @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting reports by reporter id={}", reporter.getId());
        ReportListResponse response = reportService.getReportsByReporter(reporter, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reports against a user (admin)",
        description = "Paginated list of all reports whose reported user is the given one. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of reports"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters or malformed user ID"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @GetMapping("/by-reported-user")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<ReportListResponse> getReportsByReportedUser(
            @Parameter(description = "ID of the reported user", required = true) @RequestParam UUID reportedUserId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "10")
            @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting reports for reported user id={}", reportedUserId);
        ReportListResponse response = reportService.getReportsByReportedUser(reportedUserId, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "All reports (admin)",
        description = "Paginated list of every report, regardless of status. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of reports"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @GetMapping("/admin/all")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<ReportListResponse> getAllReportsForAdmin(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "10")
            @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting all reports for admin");
        ReportListResponse response = reportService.getAllReportsForAdmin(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Pending reports (admin)",
        description = "Paginated list of reports still in PENDING status, for the moderation queue. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of pending reports"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @GetMapping("/admin/pending")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<ReportListResponse> getPendingReportsForAdmin(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "10")
            @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting pending reports for admin");
        ReportListResponse response = reportService.getPendingReportsForAdmin(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update a report's status (admin)",
        description = "Moves the report through the triage workflow. `status` must be one of " +
            "`PENDING`, `UNDER_REVIEW`, `RESOLVED`, `REJECTED`. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The report with its new status"),
        @ApiResponse(responseCode = "400", description = "Validation failed or invalid status value"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Report not found"),
    })
    @PutMapping("/{id}/status")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<ReportResponse> updateReportStatus(
            @Parameter(description = "ID of the report to update", required = true) @PathVariable UUID id,
            @RequestBody @Valid ReportUpdateStatusRequest request) {
        log.info("Updating report {} status to {}", id, request.getStatus());
        ReportResponse response = reportService.updateReportStatus(id, null, request.getStatus());
        return ResponseEntity.ok(response);
    }
}
