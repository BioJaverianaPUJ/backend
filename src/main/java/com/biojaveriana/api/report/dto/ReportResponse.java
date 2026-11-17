package com.biojaveriana.api.report.dto;

import com.biojaveriana.api.report.ReportStatus;
import com.biojaveriana.api.report.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response body containing report details", example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"reporterUsername\": \"alice_smith\", \"reporterDisplayName\": \"Alice Smith\", \"reportedUserUsername\": \"john_doe\", \"reportedUserDisplayName\": \"John Doe\", \"reportType\": \"POST\", \"reportedPostId\": \"770e8400-e29b-41d4-a716-446655440002\", \"reportedCommentId\": null, \"reason\": \"Inappropriate content\", \"status\": \"PENDING\", \"createdAt\": \"2026-06-15T10:00:00Z\", \"updatedAt\": \"2026-06-15T10:00:00Z\"}")
public record ReportResponse(
    @Schema(description = "Unique report identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Username of the user who filed the report", example = "alice_smith")
    String reporterUsername,

    @Schema(description = "Display name of the reporter", example = "Alice Smith")
    String reporterDisplayName,

    @Schema(description = "Username of the reported user", example = "john_doe")
    String reportedUserUsername,

    @Schema(description = "Display name of the reported user", example = "John Doe")
    String reportedUserDisplayName,

    @Schema(description = "Type of content being reported", example = "POST", allowableValues = {"POST", "COMMENT"})
    ReportType reportType,

    @Schema(description = "ID of the reported post (if applicable)", example = "770e8400-e29b-41d4-a716-446655440002")
    String reportedPostId,

    @Schema(description = "ID of the reported comment (if applicable)", example = "null")
    String reportedCommentId,

    @Schema(description = "Reason for the report", example = "Inappropriate content")
    String reason,

    @Schema(description = "Current status of the report", example = "PENDING", allowableValues = {"PENDING", "UNDER_REVIEW", "RESOLVED", "REJECTED"})
    ReportStatus status,

    @Schema(description = "Timestamp when the report was created", example = "2026-06-15T10:00:00Z")
    Instant createdAt,

    @Schema(description = "Timestamp when the report was last updated", example = "2026-06-15T10:00:00Z")
    Instant updatedAt
) {}
