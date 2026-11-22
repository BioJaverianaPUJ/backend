package com.biojaveriana.api.observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.biojaveriana.api.common.exceptions.ValidationException;
import com.biojaveriana.api.gamification.event.EventDispatcher;
import com.biojaveriana.api.observation.config.ObservationRateLimitConfig;
import com.biojaveriana.api.observation.dto.ObservationCreateRequest;
import com.biojaveriana.api.observation.dto.ObservationJobStatusResponse;
import com.biojaveriana.api.observation.dto.ObservationListResponse;
import com.biojaveriana.api.observation.dto.ObservationMessage;
import com.biojaveriana.api.observation.dto.ObservationResponse;
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

    @Mock
    private EventDispatcher eventDispatcher;

    private ObservationService observationService;
    private User user;
    private User other;
    private User admin;
    private User superadmin;

    @BeforeEach
    void setUp() {
        observationService = new ObservationService(observationRepository, userRepository, jobService, imageUploader,
            messageProducer, urlGenerator, rateLimitConfig, eventDispatcher);
        user = User.builder().id(UUID.randomUUID()).username("jane").role(Role.USER).build();
        other = User.builder().id(UUID.randomUUID()).username("bob").role(Role.USER).build();
        admin = User.builder().id(UUID.randomUUID()).username("admin").role(Role.ADMIN).build();
        superadmin = User.builder().id(UUID.randomUUID()).username("super").role(Role.SUPERADMIN).build();
        lenient().when(observationRepository.save(any(Observation.class))).thenAnswer(inv -> {
            Observation o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(UUID.randomUUID());
            }
            return o;
        });
        lenient().when(urlGenerator.generateUrl(anyString()))
            .thenAnswer(inv -> "https://cdn.example.com/bucket/" + inv.getArgument(0));
    }

    private Observation buildObservation(UUID id, User owner) {
        return Observation.builder()
            .id(id)
            .user(owner)
            .referencePictureKey("observations/" + owner.getId() + "/pic.jpg")
            .latitude(1.0)
            .longitude(2.0)
            .checked(false)
            .hidden(false)
            .revisionRequested(false)
            .build();
    }

    private ObservationCreateRequest request() {
        return ObservationCreateRequest.builder()
            .referencePictureKey("key1")
            .latitude(1.0)
            .longitude(2.0)
            .waitTimeoutSeconds(0)
            .build();
    }

    private ObservationJob buildJob(UUID id) {
        return ObservationJob.builder().id(id).status(JobStatus.PENDING).retryCount(0).maxRetries(3).build();
    }

    @Nested
    @DisplayName("createPresign")
    class CreatePresignTests {

        @Test
        @DisplayName("delegates to the image uploader with a key under observations/<userId>/")
        void createPresignUsesGeneratedKey() {
            when(imageUploader.createPresign(anyString(), eq("image/jpeg")))
                .thenReturn(new UploadPresign("key", "url", "PUT", "image/jpeg", Instant.now()));

            observationService.createPresign(user, ObservationUploadPresignRequest.builder().contentType("image/jpeg").build());

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(imageUploader).createPresign(keyCaptor.capture(), eq("image/jpeg"));
            assertTrue(keyCaptor.getValue().startsWith("observations/" + user.getId() + "/"));
        }
    }


    @Nested
    @DisplayName("createObservation")
    class CreateObservationTests {

        @Test
        @DisplayName("creates the observation, its job and publishes a message")
        void createsObservationJobAndMessage() {
            when(rateLimitConfig.isEnabled()).thenReturn(false);
            ObservationJob job = buildJob(UUID.randomUUID());
            when(jobService.createJob(any(Observation.class))).thenReturn(job);

            ObservationJobStatusResponse response = observationService.createObservation(request(), user);

            assertEquals(job.getId(), response.getJobId());
            ArgumentCaptor<ObservationMessage> messageCaptor = ArgumentCaptor.forClass(ObservationMessage.class);
            verify(messageProducer).sendMessage(messageCaptor.capture());
            ObservationMessage message = messageCaptor.getValue();
            assertNotNull(message.getObservationId());
            assertEquals("key1", message.getS3Key());
            assertEquals(1.0, message.getLatitude());
            assertEquals(2.0, message.getLongitude());
            assertEquals(0, message.getRetryCount());
            assertEquals(3, message.getMaxRetries());
            verify(eventDispatcher).dispatchObservationCreated(user.getId(), message.getObservationId());
        }

        @Test
        @DisplayName("a recent observation by a regular user is rate limited")
        void rateLimitBlocksRecentObservation() {
            when(rateLimitConfig.isEnabled()).thenReturn(true);
            when(rateLimitConfig.getCooldownSeconds()).thenReturn(300L);
            user.setLastObservationTime(Instant.now().minusSeconds(10));

            assertThrows(ObservationRateLimitException.class, () -> observationService.createObservation(request(), user));

            verify(observationRepository, never()).save(any(Observation.class));
            verify(messageProducer, never()).sendMessage(any(ObservationMessage.class));
        }

        @Test
        @DisplayName("an observation is allowed once the cooldown has elapsed")
        void rateLimitAllowsAfterCooldown() {
            when(rateLimitConfig.isEnabled()).thenReturn(true);
            when(rateLimitConfig.getCooldownSeconds()).thenReturn(300L);
            user.setLastObservationTime(Instant.now().minusSeconds(400));
            when(jobService.createJob(any(Observation.class))).thenReturn(buildJob(UUID.randomUUID()));

            observationService.createObservation(request(), user);

            verify(userRepository).save(user);
            assertNotNull(user.getLastObservationTime());
        }

        @Test
        @DisplayName("admins are exempt from the rate limit")
        void adminBypassesRateLimit() {
            when(rateLimitConfig.isEnabled()).thenReturn(true);
            admin.setLastObservationTime(Instant.now());
            when(jobService.createJob(any(Observation.class))).thenReturn(buildJob(UUID.randomUUID()));

            observationService.createObservation(request(), admin);

            verify(userRepository, never()).save(any(User.class));
            verify(messageProducer).sendMessage(any(ObservationMessage.class));
        }

        @Test
        @DisplayName("the rate limit is skipped entirely when disabled")
        void disabledRateLimitIsSkipped() {
            when(rateLimitConfig.isEnabled()).thenReturn(false);
            user.setLastObservationTime(Instant.now());
            when(jobService.createJob(any(Observation.class))).thenReturn(buildJob(UUID.randomUUID()));

            observationService.createObservation(request(), user);

            verify(userRepository, never()).save(any(User.class));
            verify(messageProducer).sendMessage(any(ObservationMessage.class));
        }
    }


    @Nested
    @DisplayName("getJobStatus")
    class GetJobStatusTests {

        @Test
        @DisplayName("the owner gets the job status")
        void ownerGetsJobStatus() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            ObservationJob job = buildJob(UUID.randomUUID());
            when(observationRepository.findById(observation.getId())).thenReturn(Optional.of(observation));
            when(jobService.findByObservationId(observation.getId())).thenReturn(job);

            ObservationJobStatusResponse response = observationService.getJobStatus(observation.getId(), user);

            assertEquals(job.getId(), response.getJobId());
            assertEquals(JobStatus.PENDING, response.getStatus());
        }

        @Test
        @DisplayName("another user is forbidden from the job status")
        void foreignUserIsForbidden() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            when(observationRepository.findById(observation.getId())).thenReturn(Optional.of(observation));

            assertThrows(ObservationForbiddenException.class, () -> observationService.getJobStatus(observation.getId(), other));
        }

        @Test
        @DisplayName("a missing observation is not found")
        void missingObservationNotFound() {
            UUID id = UUID.randomUUID();
            when(observationRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ObservationNotFoundException.class, () -> observationService.getJobStatus(id, user));
        }
    }

    @Nested
    @DisplayName("getObservation")
    class GetObservationTests {

        @Test
        @DisplayName("the owner gets the mapped observation with its image URL")
        void ownerGetsObservation() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            when(observationRepository.findById(observation.getId())).thenReturn(Optional.of(observation));

            ObservationResponse response = observationService.getObservation(observation.getId(), user);

            assertEquals(observation.getId(), response.getId());
            assertEquals("https://cdn.example.com/bucket/" + observation.getReferencePictureKey(), response.getImageUrl());
        }

        @Test
        @DisplayName("another user is forbidden from the observation")
        void foreignUserIsForbidden() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            when(observationRepository.findById(observation.getId())).thenReturn(Optional.of(observation));

            assertThrows(ObservationForbiddenException.class, () -> observationService.getObservation(observation.getId(), other));
        }
    }


    @Nested
    @DisplayName("listObservations")
    class ListObservationsTests {

        @Test
        @DisplayName("returns the user's own observations")
        void returnsUsersObservations() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            when(observationRepository.findByUserOrderByCreatedAtDesc(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(observation)));

            ObservationListResponse response = observationService.listObservations(user, 0, 20);

            assertEquals(1, response.content().size());
            assertEquals(observation.getId(), response.content().get(0).getId());
            assertEquals(0, response.pagination().page());
        }
    }

    @Nested
    @DisplayName("requestRevision")
    class RequestRevisionTests {

        @Test
        @DisplayName("a checked own observation can request a revision")
        void checkedObservationCanRequestRevision() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            observation.setChecked(true);
            when(observationRepository.findById(observation.getId())).thenReturn(Optional.of(observation));

            observationService.requestRevision(observation.getId(), user);

            assertTrue(observation.isRevisionRequested());
            verify(observationRepository).save(observation);
        }

        @Test
        @DisplayName("an unchecked observation cannot request a revision")
        void uncheckedObservationCannotRequestRevision() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            when(observationRepository.findById(observation.getId())).thenReturn(Optional.of(observation));

            assertThrows(ValidationException.class, () -> observationService.requestRevision(observation.getId(), user));
        }

        @Test
        @DisplayName("another user cannot request a revision")
        void foreignUserCannotRequestRevision() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            observation.setChecked(true);
            when(observationRepository.findById(observation.getId())).thenReturn(Optional.of(observation));

            assertThrows(ObservationForbiddenException.class, () -> observationService.requestRevision(observation.getId(), other));
        }

        @Test
        @DisplayName("a missing observation cannot request a revision")
        void missingObservationCannotRequestRevision() {
            UUID id = UUID.randomUUID();
            when(observationRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ObservationNotFoundException.class, () -> observationService.requestRevision(id, user));
        }
    }

    @Nested
    @DisplayName("listAllForAdmin")
    class ListAllForAdminTests {

        @Test
        @DisplayName("non-admins are forbidden")
        void nonAdminIsForbidden() {
            assertThrows(ObservationForbiddenException.class,
                () -> observationService.listAllForAdmin(user, false, false, 0, 20));
        }

        @Test
        @DisplayName("superadmins can list visible observations by default")
        void superadminSeesVisibleByDefault() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            when(observationRepository.findByHiddenFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(observation)));

            ObservationListResponse response = observationService.listAllForAdmin(superadmin, false, false, 0, 20);

            assertEquals(1, response.content().size());
        }

        @Test
        @DisplayName("superadmins can filter by revisionRequested")
        void superadminFiltersByRevisionRequested() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            when(observationRepository.findByRevisionRequestedTrueOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(observation)));

            ObservationListResponse response = observationService.listAllForAdmin(superadmin, false, true, 0, 20);

            assertEquals(1, response.content().size());
        }

        @Test
        @DisplayName("admins see visible observations by default")
        void adminSeesVisibleByDefault() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            when(observationRepository.findByHiddenFalseOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(observation)));

            ObservationListResponse response = observationService.listAllForAdmin(admin, false, false, 0, 20);

            assertEquals(1, response.content().size());
        }

        @Test
        @DisplayName("admins can include hidden observations")
        void adminIncludesHidden() {
            when(observationRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

            ObservationListResponse response = observationService.listAllForAdmin(admin, true, false, 0, 20);

            assertTrue(response.content().isEmpty());
        }

        @Test
        @DisplayName("admins can filter by revisionRequested")
        void adminFiltersByRevisionRequested() {
            Observation observation = buildObservation(UUID.randomUUID(), user);
            when(observationRepository.findByRevisionRequestedTrueOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(observation)));

            ObservationListResponse response = observationService.listAllForAdmin(admin, false, true, 0, 20);

            assertEquals(1, response.content().size());
        }
    }
}
