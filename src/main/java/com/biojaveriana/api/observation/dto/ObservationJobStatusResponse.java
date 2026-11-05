package com.biojaveriana.api.observation.dto;

import java.util.UUID;

import com.biojaveriana.api.observation.JobStatus;
import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.ObservationJob;
import com.biojaveriana.api.s3.S3UrlGenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservationJobStatusResponse {
    private UUID jobId;
    private JobStatus status;
    private ObservationResponse observation;
    private String errorMessage;
    private int retryCount;

    public static ObservationJobStatusResponse from(ObservationJob job, Observation observation, S3UrlGenerator urlGenerator) {
        var builder = ObservationJobStatusResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount());

        if (observation != null) {
            builder.observation(ObservationResponse.from(observation, urlGenerator));
        }

        return builder.build();
    }
}
