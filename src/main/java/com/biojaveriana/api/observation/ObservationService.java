package com.biojaveriana.api.observation;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.observation.exceptions.ObservationNotFoundException;
import com.biojaveriana.api.observation.dto.ObservationCreateRequest;
import com.biojaveriana.api.observation.dto.ObservationJobStatusResponse;
import com.biojaveriana.api.observation.dto.ObservationListResponse;
import com.biojaveriana.api.observation.dto.ObservationResponse;
import com.biojaveriana.api.observation.dto.ObservationUploadPresignRequest;
import com.biojaveriana.api.observation.dto.ObservationMessage;
import com.biojaveriana.api.observation.rabbitmq.ObservationMessageProducer;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationService {
    private final ObservationRepository observationRepository;
    private final ObservationJobService jobService;
    private final ObservationImageUploader imageUploader;
    private final ObservationMessageProducer messageProducer;
    private final S3UrlGenerator urlGenerator;

    @Transactional
    public com.biojaveriana.api.s3.UploadPresign createPresign(ObservationUploadPresignRequest request) {
        return imageUploader.createPresign(request.getContentType());
    }

    @Transactional
    public ObservationJobStatusResponse createObservation(ObservationCreateRequest request, User user) {
        log.info("Confirmed upload for key={}", request.getReferencePictureKey());

        var observation = Observation.builder()
                .user(user)
                .referencePictureKey(request.getReferencePictureKey())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .checked(false)
                .hidden(false)
                .revisionRequested(false)
                .build();

        observation = observationRepository.save(observation);
        log.info("Created observation id={}", observation.getId());

        var job = jobService.createJob(observation);
        log.info("Created job id={} for observation id={}", job.getId(), observation.getId());

        var message = ObservationMessage.builder()
                .observationId(observation.getId())
                .s3Key(request.getReferencePictureKey())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .retryCount(0)
                .maxRetries(3)
                .build();

        messageProducer.sendMessage(message);
        log.info("Published observation job to queue for observation id={}", observation.getId());

        if (request.getWaitTimeoutSeconds() != null && request.getWaitTimeoutSeconds() > 0) {
            return pollForResult(job.getId(), request.getWaitTimeoutSeconds(), user);
        }

        return ObservationJobStatusResponse.from(job, null, urlGenerator);
    }

    private ObservationJobStatusResponse pollForResult(UUID jobId, int timeoutSeconds, User user) {
        var deadline = Instant.now().plus(Duration.ofSeconds(timeoutSeconds));

        while (Instant.now().isBefore(deadline)) {
            var job = jobService.findById(jobId);
            var status = job.getStatus();

            if (status == JobStatus.COMPLETED) {
                var observation = observationRepository.findById(job.getObservation().getId()).orElse(null);
                return ObservationJobStatusResponse.from(job, observation, urlGenerator);
            }

            if (status == JobStatus.FAILED) {
                return ObservationJobStatusResponse.from(job, null, urlGenerator);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        var job = jobService.findById(jobId);
        var observation = observationRepository.findById(job.getObservation().getId()).orElse(null);
        return ObservationJobStatusResponse.from(job, observation, urlGenerator);
    }

    @Transactional(readOnly = true)
    public ObservationJobStatusResponse getJobStatus(UUID observationId) {
        var observation = observationRepository.findById(observationId)
                .orElseThrow(() -> new ObservationNotFoundException(observationId));

        var job = jobService.findByObservationId(observationId);

        return ObservationJobStatusResponse.from(job, observation, urlGenerator);
    }

    @Transactional(readOnly = true)
    public ObservationResponse getObservation(UUID observationId) {
        var observation = observationRepository.findById(observationId)
            .orElseThrow(() -> new ObservationNotFoundException(observationId));

        return ObservationResponse.from(observation, urlGenerator);
    }

    @Transactional(readOnly = true)
    public ObservationListResponse listObservations(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Observation> pageResult = observationRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        var content = pageResult.getContent().stream()
            .map(o -> ObservationResponse.from(o, urlGenerator))
            .toList();

        return ObservationListResponse.from(
            content,
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages()
        );
    }
}
