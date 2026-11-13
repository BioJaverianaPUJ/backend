package com.biojaveriana.api.poi;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.biojaveriana.api.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "poi_telemetry", indexes = {
    @Index(name = "idx_poi_telemetry_poi_id", columnList = "poi_id"),
    @Index(name = "idx_poi_telemetry_user_id", columnList = "user_id"),
    @Index(name = "idx_poi_telemetry_entered_at", columnList = "entered_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoiTelemetry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poi_id", nullable = false, foreignKey = @ForeignKey(name = "fk_poi_telemetry_poi"))
    private Poi poi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_poi_telemetry_user"))
    private User user;

    @CreationTimestamp
    @Column(name = "entered_at", nullable = false, updatable = false)
    private Instant enteredAt;
}
