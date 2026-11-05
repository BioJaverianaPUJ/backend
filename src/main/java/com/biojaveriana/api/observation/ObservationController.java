package com.biojaveriana.api.observation;

import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.observation.dto.ObservationCreateRequest;
import com.biojaveriana.api.observation.dto.ObservationJobStatusResponse;
import com.biojaveriana.api.observation.dto.ObservationListResponse;
import com.biojaveriana.api.observation.dto.ObservationResponse;
import com.biojaveriana.api.observation.dto.ObservationUploadPresignRequest;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/observations")
@Validated
@RequiredArgsConstructor
public class ObservationController {
    private final ObservationService observationService;

    @PostMapping("/upload-presign")
    public UploadPresign createPresign(@CurrentUser User user,
            @Valid @RequestBody ObservationUploadPresignRequest request) {
        return observationService.createPresign(user, request);
    }

    @PostMapping
    public ObservationJobStatusResponse create(@CurrentUser User user,
            @Valid @RequestBody ObservationCreateRequest request) {
        return observationService.createObservation(request, user);
    }

    @GetMapping("/{id}/job-status")
    public ObservationJobStatusResponse getJobStatus(@CurrentUser User user, @PathVariable String id) {
        return observationService.getJobStatus(UUID.fromString(id), user);
    }

    @GetMapping("/{id}")
    public ObservationResponse getObservation(@CurrentUser User user, @PathVariable String id) {
        return observationService.getObservation(UUID.fromString(id), user);
    }

    @GetMapping
    public ObservationListResponse list(@CurrentUser User user,
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        return observationService.listObservations(user, page, size);
    }

    @HasRole(value = Role.ADMIN)
    @GetMapping("/admin")
    public ObservationListResponse listAllForAdmin(@CurrentUser User user,
            @RequestParam(defaultValue = "false") boolean includeHidden,
            @RequestParam(defaultValue = "false") boolean revisionRequested,
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        return observationService.listAllForAdmin(user, includeHidden, revisionRequested, page, size);
    }

    @PostMapping("/{id}/revision-request")
    public ObservationResponse requestRevision(@CurrentUser User user, @PathVariable String id) {
        observationService.requestRevision(UUID.fromString(id), user);
        return observationService.getObservation(UUID.fromString(id), user);
    }
}
