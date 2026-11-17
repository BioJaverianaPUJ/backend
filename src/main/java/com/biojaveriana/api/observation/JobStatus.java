package com.biojaveriana.api.observation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The status of an observation processing job", example = "PENDING")
public enum JobStatus {
    @Schema(description = "Job is queued and pending processing")
    PENDING,
    @Schema(description = "Job is currently being processed")
    PROCESSING,
    @Schema(description = "Job has completed successfully")
    COMPLETED,
    @Schema(description = "Job has failed")
    FAILED
}
