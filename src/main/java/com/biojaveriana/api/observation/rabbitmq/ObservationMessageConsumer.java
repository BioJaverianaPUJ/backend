package com.biojaveriana.api.observation.rabbitmq;

import java.time.Instant;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.ObservationJob;
import com.biojaveriana.api.observation.ObservationJobRepository;
import com.biojaveriana.api.observation.ObservationRepository;
import com.biojaveriana.api.observation.dto.ObservationMessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObservationMessageConsumer {
    private final ObjectMapper objectMapper;
    private final ObservationRepository observationRepository;
    private final ObservationJobRepository observationJobRepository;

    @Value("${app.rabbitmq.response-queue:observation.jobs.response}")
    private String responseQueue;

    @RabbitListener(queues = "${app.rabbitmq.response-queue:observation.jobs.response}")
    public void handleResponse(String payload) {
        ObservationMessageResponse response;
        try {
            response = objectMapper.readValue(payload, ObservationMessageResponse.class);
        } catch (JsonProcessingException e) {
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
            observation.setObservationType(response.getObservationType());
            observationRepository.save(observation);

            // Mark job as completed
            job.setStatus(com.biojaveriana.api.observation.JobStatus.COMPLETED);
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
                job.setStatus(com.biojaveriana.api.observation.JobStatus.PENDING);
                log.info("Retrying observation job {} (attempt {}/{})", observationId, job.getRetryCount(), job.getMaxRetries());
                // Re-publish via producer - but we need to avoid circular dependency
                // The job will be picked up by a separate retry mechanism
            } else {
                job.setStatus(com.biojaveriana.api.observation.JobStatus.FAILED);
                log.error("Observation job {} failed after {} retries", observationId, job.getMaxRetries());
            }

            observationJobRepository.save(job);
        }
    }
}
