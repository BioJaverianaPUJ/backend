package com.biojaveriana.api.report.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.report.ReportStatus;
import com.biojaveriana.api.report.ReportType;

public record ReportSummaryResponse(
    UUID id,
    ReportType reportType,
    String reportedUserUsername,
    String reportedUserDisplayName,
    String reason,
    ReportStatus status,
    Instant createdAt
) {}
