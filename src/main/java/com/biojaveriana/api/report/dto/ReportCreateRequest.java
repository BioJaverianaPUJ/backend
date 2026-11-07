package com.biojaveriana.api.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCreateRequest {
    @NotBlank(message = "Report type is required")
    private String reportType;

    @NotBlank(message = "Reported post ID is required for POST type")
    private String reportedPostId;

    private String reportedCommentId;

    @NotBlank(message = "Reason is required")
    @Size(max = 2000, message = "Reason must be at most 2000 characters")
    private String reason;
}
