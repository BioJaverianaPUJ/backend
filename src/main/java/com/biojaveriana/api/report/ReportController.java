package com.biojaveriana.api.report;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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

import com.biojaveriana.api.report.dto.ReportCreateRequest;
import com.biojaveriana.api.report.dto.ReportListResponse;
import com.biojaveriana.api.report.dto.ReportResponse;
import com.biojaveriana.api.report.dto.ReportUpdateStatusRequest;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.Role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@Validated
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Endpoints for managing content reports and moderation")
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "Create a report", description = "Submits a content report for review. Any authenticated user can report inappropriate content.")
    @ApiResponse(responseCode = "201", description = "Report created successfully")
    public ResponseEntity<ReportResponse> createReport(
            @CurrentUser @Parameter(hidden = true) User reporter,
            @Parameter(description = "Report creation request body", required = true) @RequestBody @Valid ReportCreateRequest request) {
        log.info("Creating report by user id={}", reporter.getId());
        ReportResponse response = reportService.createReport(reporter, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report by ID", description = "Retrieves a report by its unique ID")
    @ApiResponse(responseCode = "200", description = "Report found")
    @ApiResponse(responseCode = "404", description = "Report not found")
    public ResponseEntity<ReportResponse> getReport(
            @Parameter(description = "ID of the report", required = true) @PathVariable UUID id) {
        log.info("Getting report id={}", id);
        ReportResponse response = reportService.getReport(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-reporter")
    @Operation(summary = "Get reports by current user", description = "Retrieves all reports submitted by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Reports retrieved successfully")
    public ResponseEntity<ReportListResponse> getReportsByReporter(
            @CurrentUser @Parameter(hidden = true) User reporter,
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "10")) @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting reports by reporter id={}", reporter.getId());
        ReportListResponse response = reportService.getReportsByReporter(reporter, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-reported-user")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Get reports for a reported user (Admin)", description = "Retrieves all reports for a specific reported user. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Reports retrieved successfully")
    public ResponseEntity<ReportListResponse> getReportsByReportedUser(
            @Parameter(description = "ID of the reported user", required = true) @RequestParam UUID reportedUserId,
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "10")) @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting reports for reported user id={}", reportedUserId);
        ReportListResponse response = reportService.getReportsByReportedUser(reportedUserId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/all")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Get all reports (Admin)", description = "Retrieves all reports in the system. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Reports retrieved successfully")
    public ResponseEntity<ReportListResponse> getAllReportsForAdmin(
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "10")) @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting all reports for admin");
        ReportListResponse response = reportService.getAllReportsForAdmin(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/pending")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Get pending reports (Admin)", description = "Retrieves all pending reports awaiting review. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Reports retrieved successfully")
    public ResponseEntity<ReportListResponse> getPendingReportsForAdmin(
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "10")) @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting pending reports for admin");
        ReportListResponse response = reportService.getPendingReportsForAdmin(page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Update report status (Admin)", description = "Updates the status of a report (e.g., PENDING -> RESOLVED). Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Report status updated successfully")
    @ApiResponse(responseCode = "404", description = "Report not found")
    public ResponseEntity<ReportResponse> updateReportStatus(
            @Parameter(description = "ID of the report", required = true) @PathVariable UUID id,
            @Parameter(description = "Report status update request body", required = true) @RequestBody @Valid ReportUpdateStatusRequest request) {
        log.info("Updating report {} status to {}", id, request.getStatus());
        ReportResponse response = reportService.updateReportStatus(id, null, request.getStatus());
        return ResponseEntity.ok(response);
    }
}
