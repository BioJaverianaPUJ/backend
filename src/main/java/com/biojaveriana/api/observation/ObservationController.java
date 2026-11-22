package com.biojaveriana.api.observation;

import java.util.UUID;

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

import com.biojaveriana.api.common.dto.ErrorResponse;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Species observations: image upload, asynchronous identification jobs and
 * per-user/admin observation feeds.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/observations")
@Validated
@RequiredArgsConstructor
@Tag(name = "Observations", description = "Species observations. Upload a reference photo via the presign " +
    "flow, create the observation, then poll the job status until the identification finishes. " +
    "Observation creation is rate limited (one job per user per cooldown window).")
public class ObservationController {
    private final ObservationService observationService;

    @Operation(summary = "Create a presigned reference-photo upload URL",
        description = "Returns a short-lived signed URL and object key for uploading the observation's " +
            "reference photo directly to object storage. Upload the image to `url` with `method` " +
            "and `contentType`, then reference `key` as `referencePictureKey` when creating the observation.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Presigned upload details"),
        @ApiResponse(responseCode = "400", description = "Validation failed (missing content type)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "415", description = "Unsupported image content type"),
    })
    @PostMapping("/upload-presign")
    public ResponseEntity<UploadPresign> createPresign(@Parameter(hidden = true) @CurrentUser User user,
            @Valid @RequestBody ObservationUploadPresignRequest request) {
        return ResponseEntity.ok(observationService.createPresign(user, request));
    }

    @Operation(summary = "Create an observation (asynchronous)",
        description = "Submits a species identification job for the reference photo at the given " +
            "coordinates. Returns immediately with a job status; poll the job-status endpoint until " +
            "the job completes. A cooldown is applied between observations per user.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Job submitted; its current status is returned"),
        @ApiResponse(responseCode = "400", description = "Validation failed (out-of-range coordinates, ...)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Reference picture key not found (not uploaded yet)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "429", description = "Observation rate limit reached; try again later",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<ObservationJobStatusResponse> create(@Parameter(hidden = true) @CurrentUser User user,
            @Valid @RequestBody ObservationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(observationService.createObservation(request, user));
    }

    @Operation(summary = "Get an observation job's status",
        description = "Returns the current status of the identification job for the given observation. " +
            "Only the observation's owner (or an admin) can query it.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current job status"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Not the owner of this observation"),
        @ApiResponse(responseCode = "404", description = "Observation or job not found"),
    })
    @GetMapping("/{id}/job-status")
    public ResponseEntity<ObservationJobStatusResponse> getJobStatus(@Parameter(hidden = true) @CurrentUser User user,
            @Parameter(description = "ID of the observation", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(observationService.getJobStatus(id, user));
    }

    @Operation(summary = "Get an observation by ID",
        description = "Fetches a single observation with its identification result. " +
            "Only the owner (or an admin) can fetch it.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The observation"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Not the owner of this observation"),
        @ApiResponse(responseCode = "404", description = "Observation not found"),
    })
    @GetMapping("/{id}")
    public ResponseEntity<ObservationResponse> getObservation(@Parameter(hidden = true) @CurrentUser User user,
            @Parameter(description = "ID of the observation", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(observationService.getObservation(id, user));
    }

    @Operation(summary = "List my observations",
        description = "Paginated list of the authenticated user's observations, newest first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of observations"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping
    public ResponseEntity<ObservationListResponse> list(@Parameter(hidden = true) @CurrentUser User user,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "20")
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        return ResponseEntity.ok(observationService.listObservations(user, page, size));
    }

    @Operation(summary = "List all observations (admin)",
        description = "Moderation view over every observation. Optionally filters to hidden " +
            "observations and/or observations with a revision request. Requires the ADMIN role " +
            "(SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of observations"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @HasRole(value = Role.ADMIN)
    @GetMapping("/admin")
    public ResponseEntity<ObservationListResponse> listAllForAdmin(@Parameter(hidden = true) @CurrentUser User user,
            @Parameter(description = "Include hidden observations", example = "false")
            @RequestParam(defaultValue = "false") boolean includeHidden,
            @Parameter(description = "Only observations with a pending revision request", example = "false")
            @RequestParam(defaultValue = "false") boolean revisionRequested,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "20")
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        return ResponseEntity.ok(observationService.listAllForAdmin(user, includeHidden, revisionRequested, page, size));
    }

    @Operation(summary = "Request a revision of an observation",
        description = "Flags the observation's identification for review by an admin. " +
            "Only the observation's owner can request a revision. Returns the updated observation.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The observation with revision requested"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Not the owner of this observation"),
        @ApiResponse(responseCode = "404", description = "Observation not found"),
    })
    @PostMapping("/{id}/revision-request")
    public ResponseEntity<ObservationResponse> requestRevision(@Parameter(hidden = true) @CurrentUser User user,
            @Parameter(description = "ID of the observation", required = true) @PathVariable UUID id) {
        observationService.requestRevision(id, user);
        return ResponseEntity.ok(observationService.getObservation(id, user));
    }
}
