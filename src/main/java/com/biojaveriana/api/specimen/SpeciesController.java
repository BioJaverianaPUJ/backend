package com.biojaveriana.api.specimen;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Species", description = "Endpoints for managing biological species data (Admin write operations)")
public class SpeciesController {
    private final SpeciesService speciesService;

    @GetMapping
    @Operation(summary = "List all species", description = "Retrieves a paginated list of all species, sorted by scientific name")
    @ApiResponse(responseCode = "200", description = "Species retrieved successfully")
    public ResponseEntity<SpeciesListResponse> findAll(
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(value = 100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("scientificName").ascending());
        var speciesPage = speciesService.findAll(pageable);
        return ResponseEntity.ok(SpeciesListResponse.from(speciesPage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get species by ID", description = "Retrieves a species by its unique ID")
    @ApiResponse(responseCode = "200", description = "Species found")
    @ApiResponse(responseCode = "404", description = "Species not found")
    public ResponseEntity<SpeciesResponse> getSpeciesById(
            @Parameter(description = "ID of the species", required = true) @PathVariable UUID id) {
        var species = speciesService.findById(id);
        return ResponseEntity.ok(SpeciesResponse.from(species));
    }

    @PostMapping
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Create a species", description = "Creates a new species record. Admin-only operation.")
    @ApiResponse(responseCode = "201", description = "Species created successfully")
    public ResponseEntity<SpeciesResponse> create(
        @CurrentUser @Parameter(hidden = true) User admin,
        @Parameter(description = "Species creation request body", required = true) @Valid @RequestBody SpeciesCreateRequest speciesRequest
    ) {
        log.info("Creating species by admin id={}", admin.getId());
        var species = speciesService.create(speciesRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(SpeciesResponse.from(species));
    }

    @PatchMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Update a species", description = "Updates an existing species record. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Species updated successfully")
    @ApiResponse(responseCode = "404", description = "Species not found")
    public ResponseEntity<SpeciesResponse> update(
        @Parameter(description = "ID of the species to update", required = true) @PathVariable UUID id,
        @CurrentUser @Parameter(hidden = true) User admin,
        @Parameter(description = "Species update request body", required = true) @Valid @RequestBody SpeciesUpdateRequest speciesUpdate
    ) {
        log.info("Updating species {} by admin id={}", id, admin.getId());
        var updatedSpecies = speciesService.update(id, speciesUpdate);
        return ResponseEntity.ok(SpeciesResponse.from(updatedSpecies));
    }

    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Delete a species", description = "Permanently deletes a species record. Admin-only operation.")
    @ApiResponse(responseCode = "204", description = "Species deleted successfully")
    @ApiResponse(responseCode = "404", description = "Species not found")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID of the species to delete", required = true) @PathVariable UUID id,
        @CurrentUser @Parameter(hidden = true) User admin
    ) {
        log.info("Deleting species {} by admin id={}", id, admin.getId());
        speciesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
