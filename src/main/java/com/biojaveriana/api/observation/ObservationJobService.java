package com.biojaveriana.api.observation;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.biojaveriana.api.observation.exceptions.ObservationJobNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationJobService {
    private final ObservationJobRepository jobRepository;

    @Value("${app.rabbitmq.job-expiry-hours:24}")
    private int jobExpiryHours;

    public ObservationJob createJob(Observation observation) {
        var job = ObservationJob.builder()
                .observation(observation)
                .status(JobStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .expiresAt(Instant.now().plus(Duration.ofHours(jobExpiryHours)))
                .build();
        return jobRepository.save(job);
    }

    public ObservationJob findById(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ObservationJobNotFoundException(jobId));
    }

    public ObservationJob findByObservationId(UUID observationId) {
        return jobRepository.findByObservationId(observationId)
                .orElseThrow(() -> new ObservationJobNotFoundException(observationId));
    }

    public void markAsProcessing(UUID jobId) {
        var job = findById(jobId);
        job.setStatus(JobStatus.PROCESSING);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
    }

    public void markAsCompleted(UUID jobId) {
        var job = findById(jobId);
        job.setStatus(JobStatus.COMPLETED);
        job.setErrorMessage(null);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
    }

    public void markAsFailed(UUID jobId, String errorMessage) {
        var job = findById(jobId);
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
    }

    public boolean incrementRetry(UUID jobId) {
        var job = findById(jobId);
        job.incrementRetry();
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        return job.shouldRetry();
    }
}
