package com.biojaveriana.api.poi;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.poi.dto.PoiCreateRequest;
import com.biojaveriana.api.poi.dto.PoiListResponse;
import com.biojaveriana.api.poi.dto.PoiResponse;
import com.biojaveriana.api.poi.dto.PoiTelemetryRequest;
import com.biojaveriana.api.poi.dto.PoiUpdateRequest;
import com.biojaveriana.api.poi.exceptions.PoiNotFoundException;
import com.biojaveriana.api.poi.exceptions.PoiTelemetryException;
import com.biojaveriana.api.poi.exceptions.PoiValidationException;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;

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

    @Transactional
    public PoiResponse createPoi(User user, PoiCreateRequest request) {
        validateCreateRequest(request);
        var poi = Poi.builder()
            .creator(user).name(request.getName()).description(request.getDescription())
            .geometryType(request.getGeometryType()).visibility(request.getVisibility()).active(true).build();
        if (request.getGeometryType() == PoiType.POLYGON) {
            poi.setPolygonCoordinatesWkt(polygonToWkt(request.getPolygonCoordinates()));
        } else {
            poi.setCenterLatitude(request.getCenterLatitude());
            poi.setCenterLongitude(request.getCenterLongitude());
            poi.setRadiusMeters(request.getRadiusMeters());
        }
        poi = poiRepository.save(poi);
        log.info("POI created id={} by user id={}", poi.getId(), user.getId());
        return PoiResponse.from(poi);
    }

    @Transactional
    public PoiResponse updatePoi(UUID poiId, User user, PoiUpdateRequest request) {
        var poi = poiRepository.findById(poiId).orElseThrow(() -> new PoiNotFoundException(poiId));
        if (!poi.getCreator().getId().equals(user.getId()) && user.getRole() != Role.ADMIN && user.getRole() != Role.SUPERADMIN) {
            throw new com.biojaveriana.api.common.exceptions.ForbiddenException();
        }
        if (request.getName() != null) poi.setName(request.getName());
        if (request.getDescription() != null) poi.setDescription(request.getDescription());
        if (request.getVisibility() != null) poi.setVisibility(request.getVisibility());
        if (request.getActive() != null) poi.setActive(request.getActive());
        if (request.getPolygonCoordinates() != null && !request.getPolygonCoordinates().isEmpty()) {
            poi.setGeometryType(PoiType.POLYGON);
            poi.setPolygonCoordinatesWkt(polygonToWkt(request.getPolygonCoordinates()));
            poi.setCenterLatitude(null); poi.setCenterLongitude(null); poi.setRadiusMeters(null);
        } else if (request.getCenterLatitude() != null && request.getCenterLongitude() != null) {
            poi.setGeometryType(PoiType.POINT);
            poi.setCenterLatitude(request.getCenterLatitude());
            poi.setCenterLongitude(request.getCenterLongitude());
            poi.setRadiusMeters(request.getRadiusMeters());
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
        List<PoiVisibility> visibleTypes = List.of(PoiVisibility.PUBLIC, PoiVisibility.ROLE_BASED);
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPERADMIN) {
            visibleTypes = List.of(PoiVisibility.PUBLIC, PoiVisibility.PRIVATE, PoiVisibility.ROLE_BASED);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Poi> pageResult = poiRepository.findByActiveTrueAndVisibilityInOrderByCreatedAtDesc(visibleTypes, pageable);
        return PoiListResponse.from(pageResult);
    }

    @Transactional
    public List<PoiResponse> recordTelemetry(User user, PoiTelemetryRequest request) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(Duration.ofMinutes(TELEMETRY_DEDUPLICATION_WINDOW_MINUTES));
        List<PoiVisibility> visibleTypes = List.of(PoiVisibility.PUBLIC, PoiVisibility.ROLE_BASED);
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPERADMIN) {
            visibleTypes = List.of(PoiVisibility.PUBLIC, PoiVisibility.PRIVATE, PoiVisibility.ROLE_BASED);
        }
        double longitude = request.getLongitude();
        double latitude = request.getLatitude();
        List<Poi> pois = new ArrayList<>();
        var polygonPois = poiRepository.findPoisContainingPoint(visibleTypes, "POLYGON EMPTY", longitude, latitude);
        for (var poi : polygonPois) {
            if (poi.getPolygonCoordinatesWkt() != null) {
                try {
                    Geometry geom = new WKTReader().read(poi.getPolygonCoordinatesWkt());
                    Point point = new GeometryFactory().createPoint(
                        new org.locationtech.jts.geom.Coordinate(longitude, latitude));
                    if (geom.contains(point)) pois.add(poi);
                } catch (Exception e) { log.warn("Failed to parse WKT for POI {}: {}", poi.getId(), e.getMessage()); }
            }
        }
        var pointPois = poiRepository.findPointPoisNearLocation(visibleTypes, longitude, latitude, Double.MAX_VALUE);
        for (var poi : pointPois) {
            if (poi.getGeometryType() == PoiType.POINT && poi.getRadiusMeters() != null) {
                double distance = calculateDistance(latitude, longitude, poi.getCenterLatitude(), poi.getCenterLongitude());
                if (distance <= poi.getRadiusMeters()) pois.add(poi);
            }
        }
        List<PoiResponse> responses = new ArrayList<>();
        for (var poi : pois) {
            boolean alreadyRecorded = poiTelemetryRepository.findByPoiIdAndUserIdAndEnteredAfter(poi.getId(), user.getId(), windowStart).isPresent();
            if (!alreadyRecorded) {
                var telemetry = PoiTelemetry.builder().poi(poi).user(user).enteredAt(now).build();
                poiTelemetryRepository.save(telemetry);
                log.info("Telemetry recorded for POI id={} by user id={}", poi.getId(), user.getId());
                responses.add(PoiResponse.from(poi));
            }
        }
        return responses;
    }

    private void validateCreateRequest(PoiCreateRequest request) {
        if (request.getGeometryType() == PoiType.POLYGON) {
            if (request.getPolygonCoordinates() == null || request.getPolygonCoordinates().size() < MIN_POLYGON_COORDINATES) {
                throw new PoiValidationException("Polygon must have at least " + MIN_POLYGON_COORDINATES + " coordinates");
            }
            var first = request.getPolygonCoordinates().get(0);
            var last = request.getPolygonCoordinates().get(request.getPolygonCoordinates().size() - 1);
            if (first.getLatitude() != last.getLatitude() || first.getLongitude() != last.getLongitude()) {
                throw new PoiValidationException("Polygon must be closed (first and last coordinates must match)");
            }
        } else if (request.getGeometryType() == PoiType.POINT) {
            if (request.getCenterLatitude() == null || request.getCenterLongitude() == null) {
                throw new PoiValidationException("Point geometry requires centerLatitude and centerLongitude");
            }
            if (request.getRadiusMeters() == null || request.getRadiusMeters() <= 0) {
                throw new PoiValidationException("Point geometry requires a positive radiusMeters");
            }
        }
    }

    private String polygonToWkt(List<PoiCreateRequest.PolygonCoordinate> coordinates) {
        StringBuilder wkt = new StringBuilder("POLYGON ((");
        for (int i = 0; i < coordinates.size(); i++) {
            var coord = coordinates.get(i);
            wkt.append(coord.getLongitude()).append(" ").append(coord.getLatitude());
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

