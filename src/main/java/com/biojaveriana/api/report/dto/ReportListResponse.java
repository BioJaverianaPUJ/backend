package com.biojaveriana.api.report.dto;

import com.biojaveriana.api.common.Pagination;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response containing a list of reports", example = "{\"content\": [], \"pagination\": {\"page\": 0, \"size\": 20, \"totalElements\": 0, \"totalPages\": 0, \"first\": true, \"last\": true}}")
public record ReportListResponse(
    @Schema(description = "List of report summaries in this page", example = "[{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"reportType\": \"POST\", \"reportedUserUsername\": \"john_doe\", \"reportedUserDisplayName\": \"John Doe\", \"reason\": \"Inappropriate content\", \"status\": \"PENDING\", \"createdAt\": \"2026-06-15T10:00:00Z\"}]")
    List<ReportSummaryResponse> content,

    @Schema(description = "Pagination metadata for this page")
    Pagination pagination
) {}
