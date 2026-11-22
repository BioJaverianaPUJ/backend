package com.biojaveriana.api.observation.rabbitmq;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.observation.JobStatus;
import com.biojaveriana.api.observation.ObservationJob;
import com.biojaveriana.api.observation.ObservationJobRepository;
import com.biojaveriana.api.observation.dto.ObservationMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Re-publishes observation jobs whose previous attempt failed and that are due for a retry.
 *
 * <p>The response consumer sets {@code nextRetryAt} on a failed job (with a backoff). This
 * scheduler periodically picks up PENDING jobs whose {@code nextRetryAt} has passed and sends
 * them back to the {@code observation.jobs} queue, so failed jobs are actually resent to the
 * worker instead of staying PENDING forever.
 *
 * <p>Re-publishing happens after the state has been committed to the database (the consumer
 * transaction), and {@code nextRetryAt} is only cleared once the message has been published,
 * so a publish failure is simply retried on the next scan without losing the job.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ObservationJobRetryScheduler {
    private final ObservationJobRepository jobRepository;
    private final ObservationMessageProducer messageProducer;

    @Scheduled(fixedDelayString = "${app.observation.retry-scan-interval-ms:15000}")
    @Transactional
    public void republishDueRetries() {
        List<ObservationJob> dueJobs =
                jobRepository.findByStatusAndNextRetryAtLessThanEqual(JobStatus.PENDING, Instant.now());
        if (dueJobs.isEmpty()) {
            return;
        }

        log.info("Found {} observation job(s) due for retry", dueJobs.size());
        for (ObservationJob job : dueJobs) {
            republish(job);
        }
    }

    private void republish(ObservationJob job) {
        try {
            var observation = job.getObservation();
            var message = ObservationMessage.builder()
                    .observationId(observation.getId())
                    .s3Key(observation.getReferencePictureKey())
                    .latitude(observation.getLatitude())
                    .longitude(observation.getLongitude())
                    .retryCount(job.getRetryCount())
                    .maxRetries(job.getMaxRetries())
                    .build();

            // Publish first, then clear the retry marker: if publishing fails, nextRetryAt
            // stays set and the job is retried on the next scan.
            messageProducer.sendMessage(message);

            job.setNextRetryAt(null);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            log.info("Re-published observation job {} for observation {} (attempt {}/{})",
                    job.getId(), observation.getId(), job.getRetryCount(), job.getMaxRetries());
        } catch (Exception e) {
            log.error("Failed to re-publish observation job {} for observation {}",
                    job.getId(), job.getObservation().getId(), e);
        }
    }
}
