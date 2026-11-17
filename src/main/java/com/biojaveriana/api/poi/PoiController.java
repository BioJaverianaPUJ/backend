package com.biojaveriana.api.poi;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.poi.dto.PoiCreateRequest;
import com.biojaveriana.api.poi.dto.PoiListResponse;
import com.biojaveriana.api.poi.dto.PoiResponse;
import com.biojaveriana.api.poi.dto.PoiTelemetryRequest;
import com.biojaveriana.api.poi.dto.PoiUpdateRequest;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/pois")
@RequiredArgsConstructor
@Tag(name = "Points of Interest", description = "Endpoints for managing geographical points of interest")
public class PoiController {

    private final PoiService poiService;

    @HasRole(Role.ADMIN)
    @PostMapping
    @Operation(summary = "Create a Point of Interest", description = "Creates a new POI. Admin-only operation.")
    @ApiResponse(responseCode = "201", description = "POI created successfully")
    public ResponseEntity<PoiResponse> createPoi(
        @CurrentUser @Parameter(hidden = true) User user,
        @Parameter(description = "POI creation request body", required = true) @Valid @RequestBody PoiCreateRequest request
    ) {
        log.info("Creating POI by user id={}", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(poiService.createPoi(user, request));
    }

    @HasRole(Role.ADMIN)
    @PutMapping("/{poiId}")
    @Operation(summary = "Update POI (Admin)", description = "Updates a POI's information. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "POI updated successfully")
    @ApiResponse(responseCode = "404", description = "POI not found")
    public ResponseEntity<PoiResponse> updatePoi(
        @Parameter(description = "ID of the POI to update", required = true) @PathVariable UUID poiId,
        @CurrentUser @Parameter(hidden = true) User user,
        @Parameter(description = "POI update request body", required = true) @Valid @RequestBody PoiUpdateRequest request
    ) {
        log.info("Updating POI id={} by user id={}", poiId, user.getId());
        return ResponseEntity.ok(poiService.updatePoi(poiId, user, request));
    }

    @HasRole(Role.ADMIN)
    @DeleteMapping("/{poiId}")
    @Operation(summary = "Delete POI (Admin)", description = "Permanently deletes a POI. Admin-only operation.")
    @ApiResponse(responseCode = "204", description = "POI deleted successfully")
    @ApiResponse(responseCode = "404", description = "POI not found")
    public ResponseEntity<Void> deletePoi(
        @Parameter(description = "ID of the POI to delete", required = true) @PathVariable UUID poiId,
        @CurrentUser @Parameter(hidden = true) User user
    ) {
        log.info("Deleting POI id={} by user id={}", poiId, user.getId());
        poiService.deletePoi(poiId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{poiId}")
    @Operation(summary = "Get POI by ID", description = "Retrieves a POI by its unique ID")
    @ApiResponse(responseCode = "200", description = "POI found")
    @ApiResponse(responseCode = "404", description = "POI not found")
    public ResponseEntity<PoiResponse> getPoi(
        @Parameter(description = "ID of the POI", required = true) @PathVariable UUID poiId,
        @CurrentUser @Parameter(hidden = true) User user
    ) {
        log.info("Getting POI id={} by user id={}", poiId, user.getId());
        return ResponseEntity.ok(poiService.getPoi(poiId, user));
    }

    @GetMapping
    @Operation(summary = "List all POIs", description = "Retrieves a paginated list of all POIs")
    @ApiResponse(responseCode = "200", description = "POIs retrieved successfully")
    public ResponseEntity<PoiListResponse> listPois(
        @CurrentUser @Parameter(hidden = true) User user,
        @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Listing POIs for user id={}, page={}, size={}", user.getId(), page, size);
        return ResponseEntity.ok(poiService.listPois(user, page, size));
    }

    @PostMapping("/telemetry")
    @Operation(summary = "Record POI telemetry", description = "Records telemetry data (location) for the user's active POI")
    @ApiResponse(responseCode = "200", description = "Telemetry recorded successfully")
    public ResponseEntity<List<PoiResponse>> recordTelemetry(
        @CurrentUser @Parameter(hidden = true) User user,
        @Parameter(description = "Telemetry request body", required = true) @Valid @RequestBody PoiTelemetryRequest request
    ) {
        log.info("Recording telemetry for user id={}, lat={}, lon={}", user.getId(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(poiService.recordTelemetry(user, request));
    }
}
