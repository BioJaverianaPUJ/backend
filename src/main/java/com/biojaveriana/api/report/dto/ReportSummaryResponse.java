package com.biojaveriana.api.report.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.report.ReportStatus;
import com.biojaveriana.api.report.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compact report summary for list views", example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"reportType\": \"POST\", \"reportedUserUsername\": \"john_doe\", \"reportedUserDisplayName\": \"John Doe\", \"reason\": \"Inappropriate content\", \"status\": \"PENDING\", \"createdAt\": \"2026-06-15T10:00:00Z\"}")
public record ReportSummaryResponse(
    @Schema(description = "Unique report identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Type of content being reported", example = "POST", allowableValues = {"POST", "COMMENT"})
    ReportType reportType,

    @Schema(description = "Username of the reported user", example = "john_doe")
    String reportedUserUsername,

    @Schema(description = "Display name of the reported user", example = "John Doe")
    String reportedUserDisplayName,

    @Schema(description = "Reason for the report", example = "Inappropriate content")
    String reason,

    @Schema(description = "Current status of the report", example = "PENDING", allowableValues = {"PENDING", "UNDER_REVIEW", "RESOLVED", "REJECTED"})
    ReportStatus status,

    @Schema(description = "Timestamp when the report was created", example = "2026-06-15T10:00:00Z")
    Instant createdAt
) {}
