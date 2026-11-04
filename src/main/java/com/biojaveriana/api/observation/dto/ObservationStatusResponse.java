package com.biojaveriana.api.observation.dto;

import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.job.Job;
import com.biojaveriana.api.job.JobStatus;
import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.exceptions.InvalidObservationStateException;

public record ObservationStatusResponse(
    UUID observationId,
    String status,
    JobStatus jobStatus,
    String errorMessage,
    Instant completedAt,
    boolean revisionRequested,
    Instant revisedAt
) {
    // --- Convenience factory (caller must supply the current job) ---
    public static ObservationStatusResponse from(Observation observation, Job currentJob) {
        if (observation == null) {
            throw new InvalidObservationStateException("Observation not found");
        }
        if (currentJob == null) {
            return new ObservationStatusResponse(
                observation.getId(),
                "JOB_NOT_FOUND",
                null,
                null,
                null,
                observation.isRevisionRequested(),
                observation.getRevisedAt()
            );
        }

        var completedAt = currentJob.getCompletedAt();
        if (completedAt != null) {
            return new ObservationStatusResponse(
                observation.getId(),
                "JOB_COMPLETED",
                currentJob.getStatus(),
                currentJob.getErrorMessage(),
                completedAt,
                observation.isRevisionRequested(),
                observation.getRevisedAt()
            );
        }

        var status = currentJob.getStatus();
        if (status == JobStatus.FAILED) {
            return new ObservationStatusResponse(
                observation.getId(),
                "JOB_FAILED",
                currentJob.getStatus(),
                currentJob.getErrorMessage(),
                completedAt,
                observation.isRevisionRequested(),
                observation.getRevisedAt()
            );
        }

        return new ObservationStatusResponse(
            observation.getId(),
            "JOB_IN_PROGRESS",
            currentJob.getStatus(),
            null,
            null,
            observation.isRevisionRequested(),
            observation.getRevisedAt()
        );
    }

    // --- Legacy convenience factory (no job available) ---
    public static ObservationStatusResponse from(Observation observation) {
        if (observation == null) {
            throw new InvalidObservationStateException("Observation not found");
        }
        return new ObservationStatusResponse(
            observation.getId(),
            "JOB_NOT_FOUND",
            null,
            null,
            null,
            observation.isRevisionRequested(),
            observation.getRevisedAt()
        );
    }
}
