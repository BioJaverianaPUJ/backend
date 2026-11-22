package com.biojaveriana.api.observation.rabbitmq;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.observation.JobStatus;
import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.ObservationJob;
import com.biojaveriana.api.observation.ObservationJobRepository;
import com.biojaveriana.api.observation.ObservationRepository;
import com.biojaveriana.api.observation.dto.ObservationMessageResponse;
import com.biojaveriana.api.specimen.SpecimenMatcherService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObservationMessageConsumer {
    private final JsonMapper JsonMapper;
    private final ObservationRepository observationRepository;
    private final ObservationJobRepository observationJobRepository;
    private final SpecimenMatcherService specimenMatcherService;

    @Value("${app.rabbitmq.response-queue:observation.jobs.response}")
    private String responseQueue;

    @Value("${app.observation.retry-base-delay-seconds:30}")
    private long retryBaseDelaySeconds;

    @Value("${app.observation.retry-max-delay-seconds:600}")
    private long retryMaxDelaySeconds;

    @RabbitListener(queues = "${app.rabbitmq.response-queue:observation.jobs.response}")
    @Transactional
    public void handleResponse(String payload) {
        ObservationMessageResponse response;
        try {
            response = JsonMapper.readValue(payload, ObservationMessageResponse.class);
        } catch (JacksonException e) {
            log.error("Failed to deserialize observation response", e);
            return;
        }

        UUID observationId = response.getObservationId();
        log.info("Received observation response for observationId={}, success={}", observationId, response.isSuccess());

        ObservationJob job = observationJobRepository.findByObservationId(observationId)
                .orElseThrow(() -> new RuntimeException("Job not found for observationId: " + observationId));

        Observation observation = job.getObservation();

        if (response.isSuccess()) {
            // Mark observation as checked and populate species info
            observation.setChecked(true);
            observation.setSpeciesCommonName(response.getSpeciesCommonName());
            observation.setCertainty(response.getCertainty());
            observation.setSimilarityScore(response.getSimilarityScore());
            observation.setObservationType(response.getObservationType());

            // Try to match to an existing specimen (plant observations only)
            specimenMatcherService.matchPlantSpecimen(observation).ifPresent(observation::setSpecimen);

            observationRepository.save(observation);

            // Mark job as completed
            job.setStatus(JobStatus.COMPLETED);
            job.setErrorMessage(null);
            job.setUpdatedAt(Instant.now());
            observationJobRepository.save(job);

            log.info("Observation {} completed successfully", observationId);
        } else {
            // Handle failure
            job.incrementRetry();
            job.setErrorMessage(response.getErrorMessage());
            job.setUpdatedAt(Instant.now());

            if (job.shouldRetry()) {
                job.setStatus(JobStatus.PENDING);
                job.setNextRetryAt(Instant.now().plus(retryBackoff(job.getRetryCount())));
                log.info("Scheduling retry for observation job {} (attempt {}/{}) at {}",
                        observationId, job.getRetryCount(), job.getMaxRetries(), job.getNextRetryAt());
            } else {
                job.setStatus(JobStatus.FAILED);
                log.error("Observation job {} failed after {} retries", observationId, job.getMaxRetries());
            }

            observationJobRepository.save(job);
        }
    }

    /**
     * Exponential backoff between retry attempts: base * 2^(retryCount - 1), capped.
     * The {@code ObservationJobRetryScheduler} picks the job up once the time has passed.
     */
    private Duration retryBackoff(int retryCount) {
        long delaySeconds = retryBaseDelaySeconds * (1L << Math.max(0, retryCount - 1));
        return Duration.ofSeconds(Math.min(delaySeconds, retryMaxDelaySeconds));
    }
}
