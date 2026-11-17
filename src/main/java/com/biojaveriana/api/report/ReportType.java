package com.biojaveriana.api.report;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The type of content being reported", example = "POST")
public enum ReportType {
    @Schema(description = "A post is being reported")
    POST,
    @Schema(description = "A comment is being reported")
    COMMENT
}
