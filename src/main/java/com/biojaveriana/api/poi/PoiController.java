package com.biojaveriana.api.poi;

import java.util.List;
import java.util.UUID;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Points of interest (POIs): geographic zones managed by admins, browsed by users.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pois")
@RequiredArgsConstructor
@Tag(name = "POIs", description = "Points of interest: geographic zones (points with a radius or polygons) " +
    "such as green zones or points of interest. Only admins can create, update or delete POIs; " +
    "any user can browse the list. `POST /telemetry` reports a user's location and returns the " +
    "POIs around it.")
public class PoiController {

    private final PoiService poiService;

    @Operation(summary = "Create a POI (admin)",
        description = "Creates a new POI. For `geometryType=POINT` provide `centerLatitude`, " +
            "`centerLongitude` and `radiusMeters`; for `geometryType=POLYGON` provide " +
            "`polygonCoordinates`. Visibility is `PUBLIC` or `PRIVATE`; zone type is " +
            "`GREEN_ZONE` or `INTEREST_ZONE`. Requires the ADMIN role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "POI created"),
        @ApiResponse(responseCode = "400", description = "Validation failed (bad geometry or coordinates)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @HasRole(Role.ADMIN)
    @PostMapping
    public ResponseEntity<PoiResponse> createPoi(@Parameter(hidden = true) @CurrentUser User user,
            @Valid @RequestBody PoiCreateRequest request) {
        log.info("Creating POI by user id={}", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(poiService.createPoi(user, request));
    }

    @Operation(summary = "Update a POI (admin)",
        description = "Partially updates a POI; only non-null fields are applied. " +
            "Requires the ADMIN role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "POI updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "POI not found"),
    })
    @HasRole(Role.ADMIN)
    @PutMapping("/{poiId}")
    public ResponseEntity<PoiResponse> updatePoi(@Parameter(description = "ID of the POI to update", required = true) @PathVariable UUID poiId,
            @Parameter(hidden = true) @CurrentUser User user,
            @Valid @RequestBody PoiUpdateRequest request) {
        log.info("Updating POI id={} by user id={}", poiId, user.getId());
        return ResponseEntity.ok(poiService.updatePoi(poiId, user, request));
    }

    @Operation(summary = "Delete a POI (admin)",
        description = "Removes a POI. Requires the ADMIN role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "POI deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "POI not found"),
    })
    @HasRole(Role.ADMIN)
    @DeleteMapping("/{poiId}")
    public ResponseEntity<Void> deletePoi(
            @Parameter(description = "ID of the POI to delete", required = true) @PathVariable UUID poiId,
            @Parameter(hidden = true) @CurrentUser User user) {
        log.info("Deleting POI id={} by user id={}", poiId, user.getId());
        poiService.deletePoi(poiId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a POI by ID",
        description = "Fetches a single POI. Private POIs are only visible to admins.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The POI"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "POI not found"),
    })
    @GetMapping("/{poiId}")
    public ResponseEntity<PoiResponse> getPoi(
            @Parameter(description = "ID of the POI to fetch", required = true) @PathVariable UUID poiId,
            @Parameter(hidden = true) @CurrentUser User user) {
        log.info("Getting POI id={} by user id={}", poiId, user.getId());
        return ResponseEntity.ok(poiService.getPoi(poiId, user));
    }

    @Operation(summary = "List POIs",
        description = "Paginated list of POIs visible to the caller (public POIs for regular " +
            "users, everything for admins).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of POIs"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping
    public ResponseEntity<PoiListResponse> listPois(@Parameter(hidden = true) @CurrentUser User user,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        log.info("Listing POIs for user id={}, page={}, size={}", user.getId(), page, size);
        return ResponseEntity.ok(poiService.listPois(user, page, size));
    }

    @Operation(summary = "Report my location (telemetry)",
        description = "Records the caller's position and returns the POIs around that location " +
            "(used to surface nearby zones to the user).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of nearby POIs"),
        @ApiResponse(responseCode = "400", description = "Validation failed (out-of-range coordinates)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @PostMapping("/telemetry")
    public ResponseEntity<List<PoiResponse>> recordTelemetry(@Parameter(hidden = true) @CurrentUser User user,
            @Valid @RequestBody PoiTelemetryRequest request) {
        log.info("Recording telemetry for user id={}, lat={}, lon={}", user.getId(), request.latitude(), request.longitude());
        return ResponseEntity.ok(poiService.recordTelemetry(user, request));
    }
}
