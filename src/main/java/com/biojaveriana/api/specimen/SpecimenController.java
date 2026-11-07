package com.biojaveriana.api.specimen;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.specimen.dto.SpecimenCreateRequest;
import com.biojaveriana.api.specimen.dto.SpecimenListResponse;
import com.biojaveriana.api.specimen.dto.SpecimenResponse;
import com.biojaveriana.api.specimen.dto.SpecimenUpdateRequest;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/specimens")
@Validated
@RequiredArgsConstructor
public class SpecimenController {
    private final SpecimenService specimenService;

    @GetMapping
    public ResponseEntity<SpecimenListResponse> findAll(
        @RequestParam(defaultValue = "0") @Min(value = 0) int page,
        @RequestParam(defaultValue = "20") @Max(value = 100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        var specimenPage = specimenService.findAll(pageable);
        return ResponseEntity.ok(SpecimenListResponse.from(specimenPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecimenResponse> getSpecimenById(@PathVariable UUID id) {
        var specimen = specimenService.findById(id);
        return ResponseEntity.ok(SpecimenResponse.from(specimen));
    }

    @PostMapping
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<SpecimenResponse> create(
        @CurrentUser User admin,
        @Valid @RequestBody SpecimenCreateRequest request
    ) {
        log.info("Creating specimen by admin id={}", admin.getId());
        var specimen = specimenService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SpecimenResponse.from(specimen));
    }

    @PatchMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<SpecimenResponse> update(
        @PathVariable UUID id,
        @CurrentUser User admin,
        @Valid @RequestBody SpecimenUpdateRequest specimenUpdate
    ) {
        log.info("Updating specimen {} by admin id={}", id, admin.getId());
        var updatedSpecimen = specimenService.update(id, specimenUpdate);
        return ResponseEntity.ok(SpecimenResponse.from(updatedSpecimen));
    }

    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<Void> delete(
        @PathVariable UUID id,
        @CurrentUser User admin
    ) {
        log.info("Deleting specimen {} by admin id={}", id, admin.getId());
        specimenService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
