package com.biojaveriana.api.observation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.biojaveriana.api.observation.exceptions.ObservationJobNotFoundException;

@ExtendWith(MockitoExtension.class)
class ObservationJobServiceTest {
	@Mock
	private ObservationJobRepository jobRepository;

	@InjectMocks
	private ObservationJobService service;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "jobExpiryHours", 24);
	}

	@Test
	void createJob_setsPendingStatusAndRetryDefaults() {
		Observation observation = Observation.builder().id(UUID.randomUUID()).build();
		Instant before = Instant.now();
		when(jobRepository.save(org.mockito.ArgumentMatchers.any(ObservationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		ObservationJob job = service.createJob(observation);

		assertThat(job.getObservation()).isSameAs(observation);
		assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
		assertThat(job.getRetryCount()).isZero();
		assertThat(job.getMaxRetries()).isEqualTo(3);
		assertThat(job.getExpiresAt()).isAfter(before);
		verify(jobRepository).save(job);
	}

	@Test
	void findById_throwsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(jobRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(id))
			.isInstanceOf(ObservationJobNotFoundException.class);
	}

	@Test
	void findByObservationId_returnsMatchingJob() {
		UUID observationId = UUID.randomUUID();
		ObservationJob job = job(observationId);
		when(jobRepository.findByObservationId(observationId)).thenReturn(Optional.of(job));

		assertThat(service.findByObservationId(observationId)).isSameAs(job);
	}

	@Test
	void markAsProcessingCompletedAndFailed_updatesStatusAndError() {
		ObservationJob job = job(UUID.randomUUID());
		job.setErrorMessage("old error");
		when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

		service.markAsProcessing(job.getId());
		assertThat(job.getStatus()).isEqualTo(JobStatus.PROCESSING);
		service.markAsCompleted(job.getId());
		assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
		assertThat(job.getErrorMessage()).isNull();
		service.markAsFailed(job.getId(), "processing failed");
		assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
		assertThat(job.getErrorMessage()).isEqualTo("processing failed");
		verify(jobRepository, org.mockito.Mockito.times(3)).save(job);
	}

	@Test
	void incrementRetry_returnsWhetherAnotherRetryIsAllowed() {
		ObservationJob job = job(UUID.randomUUID());
		job.setMaxRetries(2);
		when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

		assertThat(service.incrementRetry(job.getId())).isTrue();
		assertThat(service.incrementRetry(job.getId())).isFalse();
		assertThat(job.getRetryCount()).isEqualTo(2);
		verify(jobRepository, org.mockito.Mockito.times(2)).save(job);
	}

	private static ObservationJob job(UUID observationId) {
		return ObservationJob.builder()
			.id(UUID.randomUUID())
			.observation(Observation.builder().id(observationId).build())
			.status(JobStatus.PENDING)
			.retryCount(0)
			.maxRetries(3)
			.expiresAt(Instant.now().plusSeconds(3600))
			.build();
	}
}
