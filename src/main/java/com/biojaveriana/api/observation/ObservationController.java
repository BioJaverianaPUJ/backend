package com.biojaveriana.api.observation;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.biojaveriana.api.observation.dto.HideResponse;
import com.biojaveriana.api.observation.dto.ObservationListResponse;
import com.biojaveriana.api.observation.dto.ObservationResponse;
import com.biojaveriana.api.observation.dto.ObservationStatusResponse;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/observations")
@Validated
@RequiredArgsConstructor
public class ObservationController {
    private final ObservationService observationService;

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID createObservation(
        @CurrentUser User user,
        @RequestParam double latitude,
        @RequestParam double longitude,
        @RequestPart("file") @Valid MultipartFile file
    ) {
        Observation observation = observationService.createObservation(user, file, latitude, longitude);
        return observation.getId();
    }

    @GetMapping("/{id}")
    public ObservationResponse findById(@PathVariable UUID id) {
        Observation observation = observationService.findObservationById(id);
        if (observation == null) {
            throw new com.biojaveriana.api.observation.exceptions.InvalidObservationException("Observation not found");
        }
        return ObservationResponse.from(observation, null);
    }

    @GetMapping
    public ObservationListResponse findAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(
            page, size,
            Sort.by("createdAt").descending()
        );
        return ObservationListResponse.from(observationService.findAll(pageable));
    }

    @PostMapping("/{id}/hide")
    public HideResponse hideObservation(@CurrentUser User user, @PathVariable UUID id) {
        return observationService.hideObservation(id, user.getId());
    }

    @PostMapping("/{id}/unhide")
    public HideResponse unhideObservation(@CurrentUser User user, @PathVariable UUID id) {
        return observationService.unhideObservation(id, user.getId());
    }

    @PostMapping("/{id}/request-revision")
    public ObservationStatusResponse requestRevision(@CurrentUser User user, @PathVariable UUID id) {
        Observation observation = observationService.requestRevision(id, user.getId());
        return ObservationStatusResponse.from(observation);
    }

    @GetMapping("/{id}/status")
    public ObservationStatusResponse getObservationStatus(@PathVariable UUID id) {
        Observation observation = observationService.findObservationById(id);
        if (observation == null) {
            throw new com.biojaveriana.api.observation.exceptions.InvalidObservationException("Observation not found");
        }
        return ObservationStatusResponse.from(observation);
    }

    @GetMapping("/{id}/long-poll")
    public ObservationStatusResponse longPoll(@PathVariable UUID id) {
        // Return current status including job info. Client should poll repeatedly until status changes.
        return observationService.getStatusWithJob(id);
    }
}
