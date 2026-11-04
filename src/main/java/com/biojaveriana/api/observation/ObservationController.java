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
        return observationService.createPresign(request);
    }

    @PostMapping
    public ObservationJobStatusResponse create(@CurrentUser User user,
            @Valid @RequestBody ObservationCreateRequest request) {
        return observationService.createObservation(request, user);
    }

    @GetMapping("/{id}/job-status")
    public ObservationJobStatusResponse getJobStatus(@PathVariable String id) {
        return observationService.getJobStatus(UUID.fromString(id));
    }

    @GetMapping("/{id}")
    public ObservationResponse getObservation(@PathVariable String id) {
        return observationService.getObservation(UUID.fromString(id));
    }

    @GetMapping
    public ObservationListResponse list(@CurrentUser User user,
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        return observationService.listObservations(user, page, size);
    }
}
