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
@Tag(name = "Specimens", description = "Endpoints for managing biological specimen records (Admin write operations)")
public class SpecimenController {
    private final SpecimenService specimenService;

    @GetMapping
    @Operation(summary = "List all specimens", description = "Retrieves a paginated list of all specimens, sorted by ID")
    @ApiResponse(responseCode = "200", description = "Specimens retrieved successfully")
    public ResponseEntity<SpecimenListResponse> findAll(
        @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
        @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(value = 100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        var specimenPage = specimenService.findAll(pageable);
        return ResponseEntity.ok(SpecimenListResponse.from(specimenPage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specimen by ID", description = "Retrieves a specimen by its unique ID")
    @ApiResponse(responseCode = "200", description = "Specimen found")
    @ApiResponse(responseCode = "404", description = "Specimen not found")
    public ResponseEntity<SpecimenResponse> getSpecimenById(
            @Parameter(description = "ID of the specimen", required = true) @PathVariable UUID id) {
        var specimen = specimenService.findById(id);
        return ResponseEntity.ok(SpecimenResponse.from(specimen));
    }

    @PostMapping
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Create a specimen", description = "Creates a new specimen record. Admin-only operation.")
    @ApiResponse(responseCode = "201", description = "Specimen created successfully")
    public ResponseEntity<SpecimenResponse> create(
        @CurrentUser @Parameter(hidden = true) User admin,
        @Parameter(description = "Specimen creation request body", required = true) @Valid @RequestBody SpecimenCreateRequest request
    ) {
        log.info("Creating specimen by admin id={}", admin.getId());
        var specimen = specimenService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SpecimenResponse.from(specimen));
    }

    @PatchMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Update a specimen", description = "Updates an existing specimen record. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Specimen updated successfully")
    @ApiResponse(responseCode = "404", description = "Specimen not found")
    public ResponseEntity<SpecimenResponse> update(
        @Parameter(description = "ID of the specimen to update", required = true) @PathVariable UUID id,
        @CurrentUser @Parameter(hidden = true) User admin,
        @Parameter(description = "Specimen update request body", required = true) @Valid @RequestBody SpecimenUpdateRequest specimenUpdate
    ) {
        log.info("Updating specimen {} by admin id={}", id, admin.getId());
        var updatedSpecimen = specimenService.update(id, specimenUpdate);
        return ResponseEntity.ok(SpecimenResponse.from(updatedSpecimen));
    }

    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Delete a specimen", description = "Permanently deletes a specimen record. Admin-only operation.")
    @ApiResponse(responseCode = "204", description = "Specimen deleted successfully")
    @ApiResponse(responseCode = "404", description = "Specimen not found")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID of the specimen to delete", required = true) @PathVariable UUID id,
        @CurrentUser @Parameter(hidden = true) User admin
    ) {
        log.info("Deleting specimen {} by admin id={}", id, admin.getId());
        specimenService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
