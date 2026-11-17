package com.biojaveriana.api.observation;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/observations")
@Validated
@RequiredArgsConstructor
@Tag(name = "Observations", description = "Endpoints for managing biodiversity observations including creation, media uploads, and processing status")
public class ObservationController {
    private final ObservationService observationService;

    @PostMapping("/upload-presign")
    @Operation(summary = "Generate presigned upload URL", description = "Generates a presigned URL for uploading observation media files to S3")
    @ApiResponse(responseCode = "200", description = "Presigned URL generated successfully")
    public ResponseEntity<UploadPresign> createPresign(
            @CurrentUser User user,
            @Parameter(description = "Upload presign request body") @Valid @RequestBody ObservationUploadPresignRequest request) {
        return ResponseEntity.ok(observationService.createPresign(user, request));
    }

    @PostMapping
    @Operation(summary = "Create an observation", description = "Creates a new biodiversity observation. Media processing is asynchronous.")
    @ApiResponse(responseCode = "201", description = "Observation created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid observation data")
    public ResponseEntity<ObservationJobStatusResponse> create(
            @CurrentUser User user,
            @Parameter(description = "Observation creation request body") @Valid @RequestBody ObservationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(observationService.createObservation(request, user));
    }

    @GetMapping("/{id}/job-status")
    @Operation(summary = "Get observation processing status", description = "Retrieves the current processing status of an observation's media")
    @ApiResponse(responseCode = "200", description = "Job status retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Observation not found")
    public ResponseEntity<ObservationJobStatusResponse> getJobStatus(
            @CurrentUser User user,
            @Parameter(description = "ID of the observation", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(observationService.getJobStatus(id, user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an observation by ID", description = "Retrieves a specific observation with its details and associated media")
    @ApiResponse(responseCode = "200", description = "Observation found")
    @ApiResponse(responseCode = "404", description = "Observation not found")
    public ResponseEntity<ObservationResponse> getObservation(
            @CurrentUser User user,
            @Parameter(description = "ID of the observation", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(observationService.getObservation(id, user));
    }

    @GetMapping
    @Operation(summary = "List user's observations", description = "Retrieves paginated list of observations for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Observations retrieved successfully")
    public ResponseEntity<ObservationListResponse> list(
            @CurrentUser User user,
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        return ResponseEntity.ok(observationService.listObservations(user, page, size));
    }

    @HasRole(value = Role.ADMIN)
    @GetMapping("/admin")
    @Operation(summary = "List all observations (Admin)", description = "Retrieves paginated list of all observations. Admin-only with filtering options.")
    @ApiResponse(responseCode = "200", description = "Observations retrieved successfully")
    public ResponseEntity<ObservationListResponse> listAllForAdmin(
            @CurrentUser User user,
            @Parameter(description = "Include hidden observations") @RequestParam(defaultValue = "false") boolean includeHidden,
            @Parameter(description = "Include observations with revision requested") @RequestParam(defaultValue = "false") boolean revisionRequested,
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        return ResponseEntity.ok(observationService.listAllForAdmin(user, includeHidden, revisionRequested, page, size));
    }

    @PostMapping("/{id}/revision-request")
    @Operation(summary = "Request revision for an observation", description = "Submits a revision request for an observation. Requires appropriate permissions.")
    @ApiResponse(responseCode = "200", description = "Revision request submitted successfully")
    @ApiResponse(responseCode = "404", description = "Observation not found")
    public ResponseEntity<ObservationResponse> requestRevision(
            @CurrentUser User user,
            @Parameter(description = "ID of the observation", required = true) @PathVariable UUID id) {
        observationService.requestRevision(id, user);
        return ResponseEntity.ok(observationService.getObservation(id, user));
    }
}
