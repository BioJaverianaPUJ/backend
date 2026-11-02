package com.biojaveriana.api.job;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {
    private final JobRepository jobRepository;
    private final OutboxRepository outboxRepository;

    public Job createJob(String payload, String queueKey) {
        return createJobInternal(payload, 3, queueKey);
    }

    public Job createJob(String payload, int maxRetries, String queueKey) {
        return createJobInternal(payload, maxRetries, queueKey);
    }

    private Job createJobInternal(String payload, int maxRetries, String queueKey) {
        Job job = Job.builder()
            .payload(payload)
            .result(null)
            .errorMessage(null)
            .retries(0)
            .maxRetries(maxRetries)
            .status(JobStatus.PENDING)
            .queuedAt(null)
            .startedAt(null)
            .completedAt(null)
            .queueKey(queueKey)
            .build();

        job = jobRepository.save(job);
        enqueueViaOutbox(job, queueKey);

        log.info("Job {} created and queued via outbox pattern for queue '{}'", job.getId(), queueKey);
        return job;
    }

    @Transactional
    public void completeJob(UUID jobId, String result) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        if (job.getStatus() != JobStatus.PROCESSING) {
            throw new IllegalStateException("Job " + jobId + " is not in PROCESSING state");
        }

        job.setStatus(JobStatus.COMPLETED);
        job.setResult(result);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);

        log.info("Job {} completed successfully with result", jobId);
    }

    @Transactional
    public void failJob(UUID jobId, String errorMessage) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        if (job.getStatus() != JobStatus.PROCESSING) {
            throw new IllegalStateException("Job " + jobId + " is not in PROCESSING state");
        }

        int newRetries = job.getRetries() + 1;

        if (newRetries < job.getMaxRetries()) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setRetries(newRetries);
            job.setStartedAt(null);
            job.setCompletedAt(null);
        } else {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("Max retries (" + job.getMaxRetries() + ") exceeded. Last error: " + errorMessage);
        }

        job.setCompletedAt(Instant.now());
        jobRepository.save(job);

        log.warn("Job {} marked as failed (retries {}/{}). {}", jobId, newRetries, job.getMaxRetries(),
                newRetries < job.getMaxRetries() ? "Eligible for retry" : "Max retries exhausted");
    }

    @Transactional
    public void startProcessing(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        if (job.getStatus() != JobStatus.QUEUED) {
            throw new IllegalStateException("Job " + jobId + " is not in QUEUED state");
        }

        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        log.info("Job {} started processing", jobId);
    }

    private void enqueueViaOutbox(Job job, String queueKey) {
        job.setStatus(JobStatus.QUEUED);
        job.setQueuedAt(Instant.now());
        jobRepository.save(job);

        OutboxEvent event = OutboxEvent.builder()
            .aggregateId(job.getId().toString())
            .eventType(OutboxEventType.JOB_CREATED)
            .payload(Map.of("id", job.getId().toString()).toString())
            .queueKey(queueKey)
            .status(OutboxEventStatus.PENDING)
            .build();

        outboxRepository.save(event);

        log.info("Outbox event created for Job {}", job.getId());
    }
}
