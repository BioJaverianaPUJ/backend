package com.biojaveriana.api.report.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.report.ReportStatus;
import com.biojaveriana.api.report.ReportType;

public record ReportResponse(
    UUID id,
    String reporterUsername,
    String reporterDisplayName,
    String reportedUserUsername,
    String reportedUserDisplayName,
    ReportType reportType,
    String reportedPostId,
    String reportedCommentId,
    String reason,
    ReportStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
