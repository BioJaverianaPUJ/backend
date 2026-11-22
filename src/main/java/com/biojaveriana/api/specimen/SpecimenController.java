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
 * Recorded tree specimens, linked to species in the reference catalogue.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/specimens")
@Validated
@RequiredArgsConstructor
@Tag(name = "Specimens", description = "Individual tree specimens recorded in the field (measurements, " +
    "health, location), each linked to a species from the reference catalogue. " +
    "Reading is open to all authenticated users; creating, updating and deleting specimens " +
    "requires the ADMIN role (SUPERADMIN bypasses the check).")
public class SpecimenController {
    private final SpecimenService specimenService;

    @Operation(summary = "List specimens",
        description = "Paginated list of all recorded specimens, ordered by ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of specimens"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping
    public ResponseEntity<SpecimenListResponse> findAll(
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") @Min(value = 0) int page,
        @Parameter(description = "Page size (1..100)", example = "20")
        @RequestParam(defaultValue = "20") @Max(value = 100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        var specimenPage = specimenService.findAll(pageable);
        return ResponseEntity.ok(SpecimenListResponse.from(specimenPage));
    }

    @Operation(summary = "Get a specimen by ID",
        description = "Fetches a single recorded specimen.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The specimen"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Specimen not found"),
    })
    @GetMapping("/{id}")
    public ResponseEntity<SpecimenResponse> getSpecimenById(
            @Parameter(description = "ID of the specimen", required = true) @PathVariable UUID id) {
        var specimen = specimenService.findById(id);
        return ResponseEntity.ok(SpecimenResponse.from(specimen));
    }

    @Operation(summary = "Create a specimen (admin)",
        description = "Records a new specimen. `speciesId` (when provided) must reference an " +
            "existing species. Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Specimen created"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Referenced species not found"),
    })
    @PostMapping
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<SpecimenResponse> create(
        @Parameter(hidden = true) @CurrentUser User admin,
        @Valid @RequestBody SpecimenCreateRequest request
    ) {
        log.info("Creating specimen by admin id={}", admin.getId());
        var specimen = specimenService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SpecimenResponse.from(specimen));
    }

    @Operation(summary = "Update a specimen (admin)",
        description = "Partially updates a specimen; only non-null fields are applied. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Specimen updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Specimen not found"),
    })
    @PatchMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<SpecimenResponse> update(
        @Parameter(description = "ID of the specimen to update", required = true) @PathVariable UUID id,
        @Parameter(hidden = true) @CurrentUser User admin,
        @Valid @RequestBody SpecimenUpdateRequest specimenUpdate
    ) {
        log.info("Updating specimen {} by admin id={}", id, admin.getId());
        var updatedSpecimen = specimenService.update(id, specimenUpdate);
        return ResponseEntity.ok(SpecimenResponse.from(updatedSpecimen));
    }

    @Operation(summary = "Delete a specimen (admin)",
        description = "Removes a recorded specimen. Requires the ADMIN role " +
            "(SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Specimen deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Specimen not found"),
    })
    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID of the specimen to delete", required = true) @PathVariable UUID id,
        @Parameter(hidden = true) @CurrentUser User admin
    ) {
        log.info("Deleting specimen {} by admin id={}", id, admin.getId());
        specimenService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
