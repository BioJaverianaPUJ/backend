package com.biojaveriana.api.poi;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.common.exceptions.ForbiddenException;
import com.biojaveriana.api.poi.dto.PoiCreateRequest;
import com.biojaveriana.api.poi.dto.PoiListResponse;
import com.biojaveriana.api.poi.dto.PoiResponse;
import com.biojaveriana.api.poi.dto.PoiTelemetryRequest;
import com.biojaveriana.api.poi.dto.PoiUpdateRequest;
import com.biojaveriana.api.poi.exceptions.PoiNotFoundException;
import com.biojaveriana.api.poi.exceptions.PoiValidationException;
import com.biojaveriana.api.gamification.event.EventDispatcher;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoiService {
    private static final int MIN_POLYGON_COORDINATES = 3;
    private static final int TELEMETRY_DEDUPLICATION_WINDOW_MINUTES = 5;

    private final PoiRepository poiRepository;
    private final PoiTelemetryRepository poiTelemetryRepository;
    private final EventDispatcher eventDispatcher;

    @Transactional
    public PoiResponse createPoi(User user, PoiCreateRequest request) {
        validateCreateRequest(request);
        var poi = Poi.builder()
            .creator(user).name(request.name()).description(request.description())
            .geometryType(request.geometryType()).type(request.type())
            .visibility(request.visibility()).active(true).build();
        if (request.geometryType() == PoiType.POLYGON) {
            poi.setPolygonCoordinatesWkt(polygonToWkt(request.polygonCoordinates()));
        } else {
            poi.setCenterLatitude(request.centerLatitude());
            poi.setCenterLongitude(request.centerLongitude());
            poi.setRadiusMeters(request.radiusMeters());
        }
        poi = poiRepository.save(poi);
        log.info("POI created id={} by user id={}", poi.getId(), user.getId());
        return PoiResponse.from(poi);
    }

    @Transactional
    public PoiResponse updatePoi(UUID poiId, User user, PoiUpdateRequest request) {
        var poi = poiRepository.findById(poiId).orElseThrow(() -> new PoiNotFoundException(poiId));
        if (!poi.getCreator().getId().equals(user.getId()) && user.getRole() != Role.ADMIN && user.getRole() != Role.SUPERADMIN) {
            throw new ForbiddenException();
        }

        if (request.name() != null) poi.setName(request.name());
        if (request.description() != null) poi.setDescription(request.description());
        if (request.visibility() != null) poi.setVisibility(request.visibility());
        if (request.type() != null) poi.setType(request.type());
        if (request.active() != null) poi.setActive(request.active());
        if (request.polygonCoordinates() != null && !request.polygonCoordinates().isEmpty()) {
            poi.setGeometryType(PoiType.POLYGON);
            poi.setPolygonCoordinatesWkt(polygonToWkt(request.polygonCoordinates()));
            poi.setCenterLatitude(null); poi.setCenterLongitude(null); poi.setRadiusMeters(null);
        } else if (request.centerLatitude() != null && request.centerLongitude() != null) {
            poi.setGeometryType(PoiType.POINT);
            poi.setCenterLatitude(request.centerLatitude());
            poi.setCenterLongitude(request.centerLongitude());
            poi.setRadiusMeters(request.radiusMeters());
            poi.setPolygonCoordinatesWkt(null);
        }
        poi = poiRepository.save(poi);
        log.info("POI updated id={} by user id={}", poiId, user.getId());
        return PoiResponse.from(poi);
    }

    @Transactional
    public void deletePoi(UUID poiId, User user) {
        var poi = poiRepository.findById(poiId).orElseThrow(() -> new PoiNotFoundException(poiId));
        if (!poi.getCreator().getId().equals(user.getId()) && user.getRole() != Role.ADMIN && user.getRole() != Role.SUPERADMIN) {
            throw new com.biojaveriana.api.common.exceptions.ForbiddenException();
        }
        poi.setActive(false);
        poiRepository.save(poi);
        log.info("POI soft-deleted id={} by user id={}", poiId, user.getId());
    }

    @Transactional(readOnly = true)
    public PoiResponse getPoi(UUID poiId, User user) {
        var poi = poiRepository.findById(poiId).orElseThrow(() -> new PoiNotFoundException(poiId));
        if (!poi.getActive()) throw new PoiNotFoundException(poiId);
        if (poi.getVisibility() == PoiVisibility.PRIVATE) {
            if (user.getRole() != Role.ADMIN && user.getRole() != Role.SUPERADMIN) {
                throw new com.biojaveriana.api.common.exceptions.ForbiddenException();
            }
        }
        return PoiResponse.from(poi);
    }

    @Transactional(readOnly = true)
    public PoiListResponse listPois(User user, int page, int size) {
        List<PoiVisibility> visibleTypes = List.of(PoiVisibility.PUBLIC);
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPERADMIN) {
            visibleTypes = List.of(PoiVisibility.PUBLIC, PoiVisibility.PRIVATE);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Poi> pageResult = poiRepository.findByActiveTrueAndVisibilityInOrderByCreatedAtDesc(visibleTypes, pageable);
        return PoiListResponse.from(pageResult);
    }

    @Transactional
    public List<PoiResponse> recordTelemetry(User user, PoiTelemetryRequest request) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(Duration.ofMinutes(TELEMETRY_DEDUPLICATION_WINDOW_MINUTES));
        List<PoiVisibility> visibleTypes = List.of(PoiVisibility.PUBLIC);
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPERADMIN) {
            visibleTypes = List.of(PoiVisibility.PUBLIC, PoiVisibility.PRIVATE);
        }
        double longitude = request.longitude();
        double latitude = request.latitude();

        List<Poi> pois = new ArrayList<>();

        // --- Point POIs: distance check in Java (Haversine) ---
        var pointPois = poiRepository.findByActiveTrueAndVisibilityInAndGeometryType(visibleTypes, PoiType.POINT);
        var targetPoint = new GeometryFactory().createPoint(new Coordinate(longitude, latitude));
        for (var poi : pointPois) {
            if (poi.getCenterLatitude() != null && poi.getCenterLongitude() != null && poi.getRadiusMeters() != null) {
                double distance = calculateDistance(latitude, longitude,
                    poi.getCenterLatitude(), poi.getCenterLongitude());
                if (distance <= poi.getRadiusMeters()) {
                    pois.add(poi);
                }
            }
        }

        // --- Polygon POIs: point-in-polygon in Java (JTS) ---
        var polygonPois = poiRepository.findByActiveTrueAndVisibilityInAndGeometryType(visibleTypes, PoiType.POLYGON);
        for (var poi : polygonPois) {
            if (poi.getPolygonCoordinatesWkt() != null) {
                try {
                    Geometry geom = new WKTReader().read(poi.getPolygonCoordinatesWkt());
                    if (geom.contains(targetPoint)) {
                        pois.add(poi);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse WKT for POI {}: {}", poi.getId(), e.getMessage());
                }
            }
        }

        // --- Record telemetry for discovered POIs ---
        List<PoiResponse> responses = new ArrayList<>();
        for (var poi : pois) {
            boolean alreadyRecorded = poiTelemetryRepository
                .findByPoiIdAndUserIdAndEnteredAfter(poi.getId(), user.getId(), windowStart).isPresent();
            if (!alreadyRecorded) {
                var telemetry = PoiTelemetry.builder().poi(poi).user(user).enteredAt(now).build();
                poiTelemetryRepository.save(telemetry);
                log.info("Telemetry recorded for POI id={} by user id={}", poi.getId(), user.getId());
                eventDispatcher.dispatchPoiVisited(user.getId(), poi.getId());
                responses.add(PoiResponse.from(poi));
            }
        }
        return responses;
    }

    private void validateCreateRequest(PoiCreateRequest request) {
        if (request.geometryType() == PoiType.POLYGON) {
            if (request.polygonCoordinates() == null || request.polygonCoordinates().size() < MIN_POLYGON_COORDINATES) {
                throw new PoiValidationException("Polygon must have at least " + MIN_POLYGON_COORDINATES + " coordinates");
            }
            var first = request.polygonCoordinates().get(0);
            var last = request.polygonCoordinates().get(request.polygonCoordinates().size() - 1);
            if (first.latitude() != last.latitude() || first.longitude() != last.longitude()) {
                throw new PoiValidationException("Polygon must be closed (first and last coordinates must match)");
            }
        } else if (request.geometryType() == PoiType.POINT) {
            if (request.centerLatitude() == null || request.centerLongitude() == null) {
                throw new PoiValidationException("Point geometry requires centerLatitude and centerLongitude");
            }
            if (request.radiusMeters() == null || request.radiusMeters() <= 0) {
                throw new PoiValidationException("Point geometry requires a positive radiusMeters");
            }
        }
    }

    private String polygonToWkt(List<PoiCreateRequest.PolygonCoordinate> coordinates) {
        StringBuilder wkt = new StringBuilder("POLYGON ((");
        for (int i = 0; i < coordinates.size(); i++) {
            var coord = coordinates.get(i);
            wkt.append(coord.longitude()).append(" ").append(coord.latitude());
            if (i < coordinates.size() - 1) wkt.append(", ");
        }
        wkt.append("))");
        return wkt.toString();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

