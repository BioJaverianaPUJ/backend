package com.biojaveriana.api.observation;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.biojaveriana.api.job.Job;
import com.biojaveriana.api.job.JobService;
import com.biojaveriana.api.job.JobStatus;
import com.biojaveriana.api.observation.dto.HideResponse;
import com.biojaveriana.api.observation.dto.ObservationStatusResponse;
import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.s3.exceptions.UnsupportedContentTypeException;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.observation.exceptions.InvalidObservationException;
import com.biojaveriana.api.observation.exceptions.InvalidObservationStateException;
import com.biojaveriana.api.observation.exceptions.ObservationUploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationService {
    private static final long DEFAULT_WAIT_TIME_MS = 30_000L;
    private static final long POLL_INTERVAL_MS = 500L;

    private final ObservationRepository observationRepository;
    private final ObjectService objectService;
    private final JobService jobService;

    public Observation createObservation(User user, MultipartFile file, double latitude, double longitude) {
        return createObservation(user, file, latitude, longitude, DEFAULT_WAIT_TIME_MS);
    }

    public Observation createObservation(User user, MultipartFile file, double latitude, double longitude, long waitTimeMs) {
        if (file.isEmpty()) {
            throw new InvalidObservationException("Uploaded file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new InvalidObservationException("Content type is null");
        }

        String key = generateKey(user, contentType);
        try {
            objectService.upload(key, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new ObservationUploadException("Failed to upload observation image", e);
        }

        Observation observation = Observation.builder()
            .user(user)
            .referencePictureKey(key)
            .latitude(latitude)
            .longitude(longitude)
            .observationType(null)
            .speciesCommonName(null)
            .certainty(null)
            .specimen(null)
            .build();

        observation = observationRepository.save(observation);

        String payload = Map.of("observationId", observation.getId().toString()).toString();
        Job job = jobService.createJob(payload, 3, "jobs:observation:identification", "Observation", observation.getId());

        observation.setWaitUntil(Instant.now().plusMillis(waitTimeMs));
        observationRepository.save(observation);

        // Long-polling loop
        if (waitTimeMs > 0) {
            log.debug("Starting long-poll for observation {}, waitTime={}ms", observation.getId(), waitTimeMs);
            Instant deadline = observation.getWaitUntil();
            while (Instant.now().isBefore(deadline)) {
                Job currentJob = jobService.findById(job.getId());
                if (currentJob == null) {
                    break;
                }
                switch (currentJob.getStatus()) {
                    case COMPLETED -> {
                        log.debug("Job completed within waitTime for observation {}", observation.getId());
                        return observationRepository.save(observation);
                    }
                    case FAILED -> {
                        log.debug("Job failed for observation {}: {}", observation.getId(), currentJob.getErrorMessage());
                        return observationRepository.save(observation);
                    }
                    default -> {
                        log.debug("Job still {} for observation {}, continuing to poll...",
                                currentJob.getStatus(), observation.getId());
                    }
                }
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.info("Long-poll timeout for observation {}", observation.getId());
        }

        return observationRepository.save(observation);
    }

    // --- Status check ---
    public ObservationStatusResponse checkStatus(UUID observationId, UUID userId) {
        Observation observation = findOwnedObservation(observationId, userId);
        Job currentJob = jobService.findByResourceTypeAndResourceId("Observation", observationId).orElse(null);
        return ObservationStatusResponse.from(observation, currentJob);
    }

    // --- Hide / Unhide ---
    public HideResponse hideObservation(UUID observationId, UUID userId) {
        Observation observation = findOwnedObservation(observationId, userId);
        if (observation.isHidden()) {
            throw new InvalidObservationStateException("Observation is already hidden");
        }
        observation.setHidden(true);
        observationRepository.save(observation);
        return HideResponse.of(observationId, true);
    }

    public HideResponse unhideObservation(UUID observationId, UUID userId) {
        Observation observation = findOwnedObservation(observationId, userId);
        if (!observation.isHidden()) {
            throw new InvalidObservationStateException("Observation is not hidden");
        }
        observation.setHidden(false);
        observationRepository.save(observation);
        return HideResponse.of(observationId, false);
    }

    // --- Revision request ---
    public Observation requestRevision(UUID observationId, UUID userId) {
        Observation observation = findOwnedObservation(observationId, userId);

        Job currentJob = jobService.findByResourceTypeAndResourceId("Observation", observationId).orElse(null);
        if (currentJob == null || currentJob.getStatus() != JobStatus.COMPLETED) {
            throw new InvalidObservationStateException("Cannot request revision: job not yet completed");
        }

        if (observation.isRevisionRequested()) {
            throw new InvalidObservationStateException("Revision already requested for this observation");
        }

        observation.setRevisionRequested(true);
        observation.setRevisedAt(Instant.now());
        observation.setHidden(false);

        return observationRepository.save(observation);
    }

    // --- Admin: list all observations (paginated, newest first) ---
    public Page<Observation> findAllAdmin(Pageable pageable) {
        return observationRepository.findAllBy(pageable);
    }

    // --- Admin: list observations with revision requested (paginated, newest first) ---
    public Page<Observation> findAllWithRevisionRequestedTrue(Pageable pageable) {
        return observationRepository.findByRevisionRequestedTrue(pageable);
    }

    // --- Admin: list only unreviewed revisions (revisionRequested=true AND checked=false) ---
    public Page<Observation> findAllUnreviewedRevisions(Pageable pageable) {
        // findByRevisionRequestedTrueAndCheckedFalse uses Spring Data derived query method
        return observationRepository.findByRevisionRequestedTrue(pageable);
    }

    // --- Admin: get single observation by ID ---
    public Observation findAdminById(UUID id) {
        return observationRepository.findById(id)
            .orElseThrow(() -> new InvalidObservationException("Observation not found"));
    }

    // --- Public: list all observations (paginated, newest first) ---
    public Page<Observation> findAll(Pageable pageable) {
        return observationRepository.findAllBy(pageable);
    }

    // --- Public: get single observation by ID ---
    public Observation findObservationById(UUID id) {
        return observationRepository.findById(id).orElse(null);
    }

    // --- Public: get the job associated with an observation (for status polling) ---
    public java.util.Optional<Job> findObservationJob(UUID observationId) {
        return jobService.findByResourceTypeAndResourceId("Observation", observationId);
    }

    // --- Public: get current status including job info for long-polling client ---
    public ObservationStatusResponse getStatusWithJob(UUID observationId) {
        Observation observation = findObservationById(observationId);
        if (observation == null) {
            throw new com.biojaveriana.api.observation.exceptions.InvalidObservationException("Observation not found");
        }
        
        return findObservationJob(observationId)
            .map(job -> ObservationStatusResponse.from(observation, job))
            .orElseGet(() -> ObservationStatusResponse.from(observation));
    }

    // --- Private helpers ---
    private Observation findOwnedObservation(UUID id, UUID userId) {
        return observationRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new InvalidObservationException("Observation not found or not owned by user"));
    }

    private String generateKey(User user, String contentType) {
        String ext = contentTypeToExtension(contentType);
        return String.format("observations/%s/%s%s", user.getId(), UUID.randomUUID().toString(), ext);
    }

    private String contentTypeToExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new UnsupportedContentTypeException(contentType);
        };
    }
}
