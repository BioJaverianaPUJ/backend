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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/pois")
@RequiredArgsConstructor
public class PoiController {

    private final PoiService poiService;

    @HasRole(Role.ADMIN)
    @PostMapping
    public ResponseEntity<PoiResponse> createPoi(@CurrentUser User user, @Valid @RequestBody PoiCreateRequest request) {
        log.info("Creating POI by user id={}", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(poiService.createPoi(user, request));
    }

    @HasRole(Role.ADMIN)
    @PutMapping("/{poiId}")
    public ResponseEntity<PoiResponse> updatePoi(@PathVariable UUID poiId, @CurrentUser User user,
                                                   @Valid @RequestBody PoiUpdateRequest request) {
        log.info("Updating POI id={} by user id={}", poiId, user.getId());
        return ResponseEntity.ok(poiService.updatePoi(poiId, user, request));
    }

    @HasRole(Role.ADMIN)
    @DeleteMapping("/{poiId}")
    public ResponseEntity<Void> deletePoi(@PathVariable UUID poiId, @CurrentUser User user) {
        log.info("Deleting POI id={} by user id={}", poiId, user.getId());
        poiService.deletePoi(poiId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{poiId}")
    public ResponseEntity<PoiResponse> getPoi(@PathVariable UUID poiId, @CurrentUser User user) {
        log.info("Getting POI id={} by user id={}", poiId, user.getId());
        return ResponseEntity.ok(poiService.getPoi(poiId, user));
    }

    @GetMapping
    public ResponseEntity<PoiListResponse> listPois(@CurrentUser User user,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        log.info("Listing POIs for user id={}, page={}, size={}", user.getId(), page, size);
        return ResponseEntity.ok(poiService.listPois(user, page, size));
    }

    @PostMapping("/telemetry")
    public ResponseEntity<List<PoiResponse>> recordTelemetry(@CurrentUser User user,
                                                              @Valid @RequestBody PoiTelemetryRequest request) {
        log.info("Recording telemetry for user id={}, lat={}, lon={}", user.getId(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(poiService.recordTelemetry(user, request));
    }
}
