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
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @CurrentUser User reporter,
            @RequestBody @Valid ReportCreateRequest request) {
        log.info("Creating report by user id={}", reporter.getId());
        ReportResponse response = reportService.createReport(reporter, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable UUID id) {
        log.info("Getting report id={}", id);
        ReportResponse response = reportService.getReport(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-reporter")
    public ResponseEntity<ReportListResponse> getReportsByReporter(
            @CurrentUser User reporter,
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting reports by reporter id={}", reporter.getId());
        ReportListResponse response = reportService.getReportsByReporter(reporter, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-reported-user")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<ReportListResponse> getReportsByReportedUser(
            @RequestParam UUID reportedUserId,
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting reports for reported user id={}", reportedUserId);
        ReportListResponse response = reportService.getReportsByReportedUser(reportedUserId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/all")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<ReportListResponse> getAllReportsForAdmin(
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting all reports for admin");
        ReportListResponse response = reportService.getAllReportsForAdmin(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/pending")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<ReportListResponse> getPendingReportsForAdmin(
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "10") @Max(value = 100) int size) {
        log.info("Getting pending reports for admin");
        ReportListResponse response = reportService.getPendingReportsForAdmin(page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<ReportResponse> updateReportStatus(
            @PathVariable UUID id,
            @RequestBody @Valid ReportUpdateStatusRequest request) {
        log.info("Updating report {} status to {}", id, request.getStatus());
        ReportResponse response = reportService.updateReportStatus(id, null, request.getStatus());
        return ResponseEntity.ok(response);
    }
}
