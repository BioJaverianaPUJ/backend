package com.biojaveriana.api.poi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.gamification.event.EventDispatcher;
import com.biojaveriana.api.poi.dto.PoiCreateRequest;
import com.biojaveriana.api.poi.dto.PoiResponse;
import com.biojaveriana.api.poi.exceptions.PoiValidationException;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

@ExtendWith(MockitoExtension.class)
class PoiServiceTest {

    @Mock
    private PoiRepository poiRepository;

    @Mock
    private PoiTelemetryRepository poiTelemetryRepository;

    @Mock
    private EventDispatcher eventDispatcher;

    private PoiService poiService;
    private User user;

    @BeforeEach
    void setUp() {
        poiService = new PoiService(poiRepository, poiTelemetryRepository, eventDispatcher);
        user = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
    }

    @Nested
    @DisplayName("createPoi - polygon closed validation")
    class PolygonClosedValidationTests {

        @Test
        @DisplayName("accepts a closed polygon whose first and last coordinates are equal by value")
        void closedPolygonIsAccepted() {
            Double firstLatitude = 4.61;
            Double lastLatitude = 4.61;
            Double firstLongitude = 74.07;
            Double lastLongitude = 74.07;
            // Guard the regression: the two boxed values must be distinct
            // object instances, so only a value-based comparison can pass.
            assertNotSame(firstLatitude, lastLatitude);
            assertNotSame(firstLongitude, lastLongitude);

            when(poiRepository.save(any(Poi.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var request = new PoiCreateRequest(
                "Rio Claro",
                "Test POI",
                PoiType.POLYGON,
                PoiZoneType.GREEN_ZONE,
                List.of(
                    new PoiCreateRequest.PolygonCoordinate(firstLatitude, firstLongitude),
                    new PoiCreateRequest.PolygonCoordinate(4.62, 74.08),
                    new PoiCreateRequest.PolygonCoordinate(4.63, 74.06),
                    new PoiCreateRequest.PolygonCoordinate(lastLatitude, lastLongitude)
                ),
                null,
                null,
                null,
                PoiVisibility.PUBLIC
            );

            PoiResponse response = assertDoesNotThrow(() -> poiService.createPoi(user, request));

            assertEquals(PoiZoneType.GREEN_ZONE, response.type());
            assertEquals("POLYGON ((74.07 4.61, 74.08 4.62, 74.06 4.63, 74.07 4.61))", response.polygonCoordinatesWkt());
            verify(poiRepository).save(any(Poi.class));
        }

        @Test
        @DisplayName("rejects an unclosed polygon and persists nothing")
        void unclosedPolygonIsRejected() {
            var request = new PoiCreateRequest(
                "Rio Claro",
                null,
                PoiType.POLYGON,
                PoiZoneType.INTEREST_ZONE,
                List.of(
                    new PoiCreateRequest.PolygonCoordinate(4.61, 74.07),
                    new PoiCreateRequest.PolygonCoordinate(4.62, 74.08),
                    new PoiCreateRequest.PolygonCoordinate(4.63, 74.06)
                ),
                null,
                null,
                null,
                PoiVisibility.PUBLIC
            );

            PoiValidationException exception = assertThrows(PoiValidationException.class,
                () -> poiService.createPoi(user, request));

            assertEquals("Polygon must be closed (first and last coordinates must match)", exception.getMessage());
            verify(poiRepository, never()).save(any(Poi.class));
        }
    }
}
