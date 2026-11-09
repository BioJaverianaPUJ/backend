package com.biojaveriana.api.observation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.biojaveriana.api.common.exceptions.ValidationException;
import com.biojaveriana.api.observation.config.ObservationRateLimitConfig;
import com.biojaveriana.api.observation.dto.ObservationCreateRequest;
import com.biojaveriana.api.observation.dto.ObservationJobStatusResponse;
import com.biojaveriana.api.observation.dto.ObservationMessage;
import com.biojaveriana.api.observation.dto.ObservationUploadPresignRequest;
import com.biojaveriana.api.observation.exceptions.ObservationForbiddenException;
import com.biojaveriana.api.observation.exceptions.ObservationNotFoundException;
import com.biojaveriana.api.observation.exceptions.ObservationRateLimitException;
import com.biojaveriana.api.observation.rabbitmq.ObservationMessageProducer;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class ObservationServiceTest {
	@Mock
	private ObservationRepository observationRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private ObservationJobService jobService;
	@Mock
	private ObservationImageUploader imageUploader;
	@Mock
	private ObservationMessageProducer messageProducer;
	@Mock
	private S3UrlGenerator urlGenerator;
	@Mock
	private ObservationRateLimitConfig rateLimitConfig;

	@InjectMocks
	private ObservationService service;

	@Test
	void createPresign_usesUserScopedKeyAndContentType() {
		User user = user(Role.USER);
		UploadPresign presign = new UploadPresign("key", "url", "PUT", "image/png", Instant.now());
		when(imageUploader.createPresign(anyString(), eq("image/png"))).thenReturn(presign);

		UploadPresign result = service.createPresign(user, new ObservationUploadPresignRequest("image/png"));

		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		verify(imageUploader).createPresign(key.capture(), eq("image/png"));
		assertThat(key.getValue()).startsWith("observations/" + user.getId() + "/");
		assertThat(result).isSameAs(presign);
	}

	@Test
	void createObservation_savesObservationCreatesJobAndPublishesMessage() {
		User user = user(Role.USER);
		UUID observationId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		ObservationCreateRequest request = ObservationCreateRequest.builder()
			.referencePictureKey("observations/image.jpg")
			.latitude(4.6)
			.longitude(-74.1)
			.waitTimeoutSeconds(0)
			.build();
		when(observationRepository.save(any(Observation.class))).thenAnswer(invocation -> {
			Observation observation = invocation.getArgument(0);
			observation.setId(observationId);
			return observation;
		});
		when(jobService.createJob(any(Observation.class))).thenAnswer(invocation -> ObservationJob.builder()
			.id(jobId)
			.observation(invocation.getArgument(0))
			.status(JobStatus.PENDING)
			.retryCount(0)
			.build());
		when(urlGenerator.generateUrl("observations/image.jpg")).thenReturn("https://cdn/image.jpg");

		ObservationJobStatusResponse response = service.createObservation(request, user);

		assertThat(response.getJobId()).isEqualTo(jobId);
		assertThat(response.getStatus()).isEqualTo(JobStatus.PENDING);
		assertThat(response.getObservation().getImageUrl()).isEqualTo("https://cdn/image.jpg");
		ArgumentCaptor<ObservationMessage> message = ArgumentCaptor.forClass(ObservationMessage.class);
		verify(messageProducer).sendMessage(message.capture());
		assertThat(message.getValue().getObservationId()).isEqualTo(observationId);
		assertThat(message.getValue().getS3Key()).isEqualTo("observations/image.jpg");
		verify(userRepository, never()).save(any());
	}

	@Test
	void createObservation_rejectsUserWithinRateLimitCooldown() {
		User user = user(Role.USER);
		user.setLastObservationTime(Instant.now().minusSeconds(10));
		when(rateLimitConfig.isEnabled()).thenReturn(true);
		when(rateLimitConfig.getCooldownSeconds()).thenReturn(300L);
		ObservationCreateRequest request = ObservationCreateRequest.builder()
			.referencePictureKey("image.jpg").latitude(4.6).longitude(-74.1).build();

		assertThatThrownBy(() -> service.createObservation(request, user))
			.isInstanceOf(ObservationRateLimitException.class);

		verify(observationRepository, never()).save(any());
		verify(messageProducer, never()).sendMessage(any());
	}

	@Test
	void getObservation_returnsOwnedObservationAndRejectsOtherUsers() {
		User owner = user(Role.USER);
		User otherUser = user(Role.USER);
		Observation observation = observation(owner);
		when(observationRepository.findById(observation.getId())).thenReturn(Optional.of(observation));
		when(urlGenerator.generateUrl(observation.getReferencePictureKey())).thenReturn("https://cdn/image.jpg");

		var response = service.getObservation(observation.getId(), owner);

		assertThat(response.getId()).isEqualTo(observation.getId());
		assertThat(response.getImageUrl()).isEqualTo("https://cdn/image.jpg");
		assertThatThrownBy(() -> service.getObservation(observation.getId(), otherUser))
			.isInstanceOf(ObservationForbiddenException.class);
	}

	@Test
	void getObservation_throwsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(observationRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getObservation(id, user(Role.USER)))
			.isInstanceOf(ObservationNotFoundException.class);
	}

	@Test
	void listObservations_usesCreatedAtDescendingAndMapsPage() {
		User user = user(Role.USER);
		Observation observation = observation(user);
		when(observationRepository.findByUserOrderByCreatedAtDesc(eq(user), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of(observation), PageRequest.of(0, 10), 1));

		var response = service.listObservations(user, 0, 10);

		assertThat(response.content()).singleElement().extracting("id").isEqualTo(observation.getId());
		assertThat(response.pagination()).isNotNull();
		verify(observationRepository).findByUserOrderByCreatedAtDesc(
			user, PageRequest.of(0, 10, Sort.by("createdAt").descending())
		);
	}

	@Test
	void requestRevision_requiresCheckedObservationAndSavesRequest() {
		User user = user(Role.USER);
		Observation observation = observation(user);
		observation.setChecked(true);
		when(observationRepository.findById(observation.getId())).thenReturn(Optional.of(observation));

		service.requestRevision(observation.getId(), user);

		assertThat(observation.isRevisionRequested()).isTrue();
		verify(observationRepository).save(observation);
	}

	@Test
	void requestRevision_rejectsUncheckedObservation() {
		User user = user(Role.USER);
		Observation observation = observation(user);
		when(observationRepository.findById(observation.getId())).thenReturn(Optional.of(observation));

		assertThatThrownBy(() -> service.requestRevision(observation.getId(), user))
			.isInstanceOf(ValidationException.class);

		verify(observationRepository, never()).save(any());
	}

	@Test
	void listAllForAdmin_requiresAdminRole() {
		assertThatThrownBy(() -> service.listAllForAdmin(user(Role.USER), false, false, 0, 10))
			.isInstanceOf(ObservationForbiddenException.class);
		verify(observationRepository, never()).findByHiddenFalseOrderByCreatedAtDesc(any(PageRequest.class));
	}

	private static Observation observation(User user) {
		return Observation.builder()
			.id(UUID.randomUUID())
			.user(user)
			.referencePictureKey("observations/image.jpg")
			.latitude(4.6)
			.longitude(-74.1)
			.build();
	}

	private static User user(Role role) {
		return User.builder().id(UUID.randomUUID()).username("tester").role(role).build();
	}
}
