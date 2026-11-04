package com.biojaveriana.api.observation;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.observation.dto.ObservationListResponse;
import com.biojaveriana.api.observation.dto.ObservationResponse;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.user.Role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/observations")
@Validated
@RequiredArgsConstructor
@HasRole(value = Role.ADMIN)
public class ObservationAdminController {
    private final ObservationService observationService;
    private final S3UrlGenerator s3UrlGenerator;

    @GetMapping
    public ObservationListResponse findAll(
        @RequestParam(required = false, defaultValue = "false") boolean revisionRequested,
        @RequestParam(defaultValue = "0") @Min(value = 0) int page,
        @RequestParam(defaultValue = "20") @Max(value = 100) @Min(value = 1) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Observation> observations;

        if (revisionRequested) {
            observations = observationService.findAllWithRevisionRequestedTrue(pageable);
        } else {
            observations = observationService.findAllAdmin(pageable);
        }

        return ObservationListResponse.from(observations);
    }

    @GetMapping("/{id}")
    public ObservationResponse findById(@PathVariable UUID id) {
        var observation = observationService.findAdminById(id);
        return ObservationResponse.from(observation, s3UrlGenerator);
    }
}
