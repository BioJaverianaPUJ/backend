package com.biojaveriana.api.observation.rabbitmq;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.observation.JobStatus;
import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.ObservationJob;
import com.biojaveriana.api.observation.ObservationJobRepository;
import com.biojaveriana.api.observation.dto.ObservationMessage;

@ExtendWith(MockitoExtension.class)
class ObservationJobRetrySchedulerTest {

    @Mock
    private ObservationJobRepository jobRepository;

    @Mock
    private ObservationMessageProducer messageProducer;

    private ObservationJobRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ObservationJobRetryScheduler(jobRepository, messageProducer);
    }

    private Observation observation() {
        var observation = Observation.builder()
                .referencePictureKey("observations/user-1/picture-1.jpg")
                .latitude(-5.64)
                .longitude(-75.03)
                .build();
        observation.setId(UUID.randomUUID());
        return observation;
    }

    private ObservationJob pendingJobDueForRetry(Observation observation, int retryCount) {
        var job = ObservationJob.builder()
                .observation(observation)
                .status(JobStatus.PENDING)
                .retryCount(retryCount)
                .maxRetries(3)
                .nextRetryAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plus(Duration.ofHours(24)))
                .build();
        job.setId(UUID.randomUUID());
        return job;
    }

    @Test
    @DisplayName("republishes due pending jobs and clears the retry marker")
    void republishesDueJobs() {
        var observation = observation();
        var job = pendingJobDueForRetry(observation, 1);
        when(jobRepository.findByStatusAndNextRetryAtLessThanEqual(eq(JobStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(job));

        scheduler.republishDueRetries();

        ArgumentCaptor<ObservationMessage> captor = ArgumentCaptor.forClass(ObservationMessage.class);
        verify(messageProducer).sendMessage(captor.capture());

        var message = captor.getValue();
        assertEquals(observation.getId(), message.getObservationId());
        assertEquals("observations/user-1/picture-1.jpg", message.getS3Key());
        assertEquals(-5.64, message.getLatitude());
        assertEquals(-75.03, message.getLongitude());
        assertEquals(1, message.getRetryCount());
        assertEquals(3, message.getMaxRetries());

        assertNull(job.getNextRetryAt());
        verify(jobRepository).save(job);
    }

    @Test
    @DisplayName("does nothing when no jobs are due")
    void noDueJobs() {
        when(jobRepository.findByStatusAndNextRetryAtLessThanEqual(eq(JobStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of());

        scheduler.republishDueRetries();

        verifyNoInteractions(messageProducer);
        verify(jobRepository, never()).save(any(ObservationJob.class));
    }

    @Test
    @DisplayName("keeps the retry marker when publishing fails, so the next scan retries")
    void publishFailureKeepsNextRetryAt() {
        var observation = observation();
        var job = pendingJobDueForRetry(observation, 2);
        Instant nextRetryAt = job.getNextRetryAt();
        when(jobRepository.findByStatusAndNextRetryAtLessThanEqual(eq(JobStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(job));
        doThrow(new RuntimeException("broker unavailable"))
                .when(messageProducer).sendMessage(any(ObservationMessage.class));

        assertDoesNotThrow(() -> scheduler.republishDueRetries());

        assertEquals(nextRetryAt, job.getNextRetryAt());
        verify(jobRepository, never()).save(any(ObservationJob.class));
    }

    @Test
    @DisplayName("continues with the remaining jobs when one publish fails")
    void oneFailureDoesNotBlockOtherJobs() {
        var observationA = observation();
        var observationB = observation();
        var jobA = pendingJobDueForRetry(observationA, 1);
        var jobB = pendingJobDueForRetry(observationB, 1);
        Instant retryAtA = jobA.getNextRetryAt();
        when(jobRepository.findByStatusAndNextRetryAtLessThanEqual(eq(JobStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(jobA, jobB));
        doThrow(new RuntimeException("broker unavailable"))
                .doNothing()
                .when(messageProducer).sendMessage(any(ObservationMessage.class));

        scheduler.republishDueRetries();

        assertEquals(retryAtA, jobA.getNextRetryAt(), "failed publish must keep the retry marker");
        verify(jobRepository, never()).save(jobA);

        assertNull(jobB.getNextRetryAt(), "successful publish must clear the retry marker");
        verify(jobRepository).save(jobB);
    }
}
