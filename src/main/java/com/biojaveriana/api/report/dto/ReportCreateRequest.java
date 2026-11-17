package com.biojaveriana.api.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for creating a new content report", example = "{\"reportType\": \"POST\", \"reportedPostId\": \"550e8400-e29b-41d4-a716-446655440000\", \"reportedCommentId\": null, \"reason\": \"Inappropriate content\"}")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCreateRequest {
    @Schema(description = "Type of content being reported (POST, COMMENT, etc.)", example = "POST", allowableValues = {"POST", "COMMENT"})
    @NotBlank(message = "Report type is required")
    private String reportType;

    @Schema(description = "ID of the reported post (required when reportType is POST)", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Reported post ID is required for POST type")
    private String reportedPostId;

    @Schema(description = "ID of the reported comment (optional)", example = null)
    private String reportedCommentId;

    @Schema(description = "Reason for the report (max 2000 characters)", example = "Inappropriate content", maxLength = 2000)
    @NotBlank(message = "Reason is required")
    @Size(max = 2000, message = "Reason must be at most 2000 characters")
    private String reason;
}
