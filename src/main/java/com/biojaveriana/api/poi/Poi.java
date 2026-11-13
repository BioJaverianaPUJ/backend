package com.biojaveriana.api.poi;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

import com.biojaveriana.api.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pois")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poi {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false, foreignKey = @ForeignKey(name = "fk_poi_creator"))
    private User creator;

    @NotBlank
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "geometry_type", nullable = false)
    @NotNull
    private PoiType geometryType;

    @Lob
    @Column(name = "polygon_coordinates")
    private String polygonCoordinatesWkt;

    @Column(name = "center_latitude")
    private Double centerLatitude;

    @Column(name = "center_longitude")
    private Double centerLongitude;

    @Column(name = "radius_meters")
    @Positive
    private Double radiusMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    @NotNull
    private PoiVisibility visibility;

    @Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Geometry toGeometry() {
        try {
            if (geometryType == PoiType.POLYGON && polygonCoordinatesWkt != null) {
                return new WKTReader().read(polygonCoordinatesWkt);
            } else if (geometryType == PoiType.POINT && centerLatitude != null && centerLongitude != null) {
                var factory = new GeometryFactory();
                return factory.createPoint(new Coordinate(centerLongitude, centerLatitude));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static Poi fromWkt(String wkt) {
        var reader = new WKTReader();
        try {
            Geometry geometry = reader.read(wkt);
            return Poi.builder()
                .polygonCoordinatesWkt(wkt)
                .geometryType(geometry instanceof Polygon ? PoiType.POLYGON : PoiType.POINT)
                .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid WKT geometry: " + e.getMessage(), e);
        }
    }
}
