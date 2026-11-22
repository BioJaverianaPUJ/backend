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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reference species catalogue: readable by everyone, writable by admins.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/species")
@Validated
@RequiredArgsConstructor
@Tag(name = "Species", description = "Reference species catalogue used for observation identification. " +
    "Reading is open to all authenticated users; creating, updating and deleting species " +
    "requires the ADMIN role (SUPERADMIN bypasses the check).")
public class SpeciesController {
    private final SpeciesService speciesService;

    @Operation(summary = "List species",
        description = "Paginated list of the catalogue, sorted alphabetically by scientific name.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of species"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping
    public ResponseEntity<SpeciesListResponse> findAll(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "20")
            @RequestParam(defaultValue = "20") @Max(value = 100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("scientificName").ascending());
        var speciesPage = speciesService.findAll(pageable);
        return ResponseEntity.ok(SpeciesListResponse.from(speciesPage));
    }

    @Operation(summary = "Get a species by ID",
        description = "Fetches a single species entry.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The species"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Species not found"),
    })
    @GetMapping("/{id}")
    public ResponseEntity<SpeciesResponse> getSpeciesById(
            @Parameter(description = "ID of the species", required = true) @PathVariable UUID id) {
        var species = speciesService.findById(id);
        return ResponseEntity.ok(SpeciesResponse.from(species));
    }

    @Operation(summary = "Create a species (admin)",
        description = "Adds a new entry to the reference catalogue. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Species created"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "409", description = "A species with the same scientific name already exists"),
    })
    @PostMapping
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<SpeciesResponse> create(
        @Parameter(hidden = true) @CurrentUser User admin,
        @Valid @RequestBody SpeciesCreateRequest speciesRequest
    ) {
        log.info("Creating species by admin id={}", admin.getId());
        var species = speciesService.create(speciesRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(SpeciesResponse.from(species));
    }

    @Operation(summary = "Update a species (admin)",
        description = "Partially updates a catalogue entry; only non-null fields are applied. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Species updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Species not found"),
    })
    @PatchMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<SpeciesResponse> update(
        @Parameter(description = "ID of the species to update", required = true) @PathVariable UUID id,
        @Parameter(hidden = true) @CurrentUser User admin,
        @Valid @RequestBody SpeciesUpdateRequest speciesUpdate
    ) {
        log.info("Updating species {} by admin id={}", id, admin.getId());
        var updatedSpecies = speciesService.update(id, speciesUpdate);
        return ResponseEntity.ok(SpeciesResponse.from(updatedSpecies));
    }

    @Operation(summary = "Delete a species (admin)",
        description = "Removes a species from the catalogue. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Species deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Species not found"),
    })
    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID of the species to delete", required = true) @PathVariable UUID id,
        @Parameter(hidden = true) @CurrentUser User admin
    ) {
        log.info("Deleting species {} by admin id={}", id, admin.getId());
        speciesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
