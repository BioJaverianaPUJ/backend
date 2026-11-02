package com.biojaveriana.api.job;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FailedJobHandler {
    private static final long GRACE_PERIOD_MINUTES = 2;

    private final JobRepository jobRepository;
    private final OutboxRepository outboxRepository;

    @Scheduled(fixedDelayString = "${app.failedjob.processing.delayMs:30000}")
    @Transactional
    public void handleFailedJobs() {
        Instant deadline = Instant.now().minusSeconds(GRACE_PERIOD_MINUTES * 60);
        List<Job> failedJobs = jobRepository.findFailedJobsNeedingRetry(deadline);

        if (failedJobs.isEmpty()) {
            return;
        }

        log.info("Found {} failed job(s) eligible for retry", failedJobs.size());

        for (Job job : failedJobs) {
            repushJob(job);
        }
    }

    void repushJob(Job job) {
        int newRetries = job.getRetries() + 1;

        if (newRetries >= job.getMaxRetries()) {
            log.warn("Job {} has reached max retries ({}/{}) — not repushing",
                job.getId(), newRetries, job.getMaxRetries());
            return;
        }

        job.setRetries(newRetries);

        job.setStatus(JobStatus.PENDING);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        jobRepository.save(job);

        log.info("Repushing job {} to queue '{}' (attempt {}/{})",
            job.getId(), job.getQueueKey(), newRetries, job.getMaxRetries());

        OutboxEvent event = OutboxEvent.builder()
            .aggregateId(job.getId().toString())
            .eventType(OutboxEventType.JOB_REPUSHED)
            .payload(job.getPayload())
            .queueKey(job.getQueueKey())
            .status(OutboxEventStatus.PENDING)
            .build();

        outboxRepository.save(event);
    }
}
