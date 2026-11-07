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
import com.biojaveriana.api.specimen.dto.SpeciesCreateRequest;
import com.biojaveriana.api.specimen.dto.SpeciesListResponse;
import com.biojaveriana.api.specimen.dto.SpeciesResponse;
import com.biojaveriana.api.specimen.dto.SpeciesUpdateRequest;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/species")
@Validated
@RequiredArgsConstructor
public class SpeciesController {
    private final SpeciesService speciesService;

    @GetMapping
    public ResponseEntity<SpeciesListResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "20") @Max(value = 100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("scientificName").ascending());
        var speciesPage = speciesService.findAll(pageable);
        return ResponseEntity.ok(SpeciesListResponse.from(speciesPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpeciesResponse> getSpeciesById(@PathVariable UUID id) {
        var species = speciesService.findById(id);
        return ResponseEntity.ok(SpeciesResponse.from(species));
    }

    @PostMapping
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<SpeciesResponse> create(
        @CurrentUser User admin,
        @Valid @RequestBody SpeciesCreateRequest speciesRequest
    ) {
        log.info("Creating species by admin id={}", admin.getId());
        var species = speciesService.create(speciesRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(SpeciesResponse.from(species));
    }

    @PatchMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<SpeciesResponse> update(
        @PathVariable UUID id,
        @CurrentUser User admin,
        @Valid @RequestBody SpeciesUpdateRequest speciesUpdate
    ) {
        log.info("Updating species {} by admin id={}", id, admin.getId());
        var updatedSpecies = speciesService.update(id, speciesUpdate);
        return ResponseEntity.ok(SpeciesResponse.from(updatedSpecies));
    }

    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<Void> delete(
        @PathVariable UUID id,
        @CurrentUser User admin
    ) {
        log.info("Deleting species {} by admin id={}", id, admin.getId());
        speciesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
