package com.biojaveriana.api.report;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The status of a content report", example = "PENDING")
public enum ReportStatus {
    @Schema(description = "Report is awaiting review")
    PENDING,
    @Schema(description = "Report is currently under review")
    UNDER_REVIEW,
    @Schema(description = "Report has been resolved")
    RESOLVED,
    @Schema(description = "Report has been rejected")
    REJECTED
}
