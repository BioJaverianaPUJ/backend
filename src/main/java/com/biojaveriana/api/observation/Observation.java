package com.biojaveriana.api.observation;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.biojaveriana.api.specimen.Specimen;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "observations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Observation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_observation_user"))
    private User user;

    // The Job entity no longer holds a back‑reference to Observation.
    // Jobs are queried by their polymorphic resource fields (resourceType/resourceId).

    @NotBlank
    @Column(name = "reference_picture_key", nullable = false)
    private String referencePictureKey;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Default
    @Column(name = "hidden", nullable = false)
    private boolean hidden = false;

    @Default
    @Column(name = "checked", nullable = false)
    private boolean checked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_type", nullable = true)
    private ObservationType observationType;

    @Column(name = "species_common_name", nullable = true)
    private String speciesCommonName;

    @Column(name = "certainty", nullable = true)
    private Double certainty;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "specimen_id", nullable = true)
    private Specimen specimen;

    // --- Long-polling deadline ---
    @Column(name = "wait_until", nullable = true)
    private Instant waitUntil;

    // --- Revision tracking ---
    @Column(name = "revision_requested", nullable = false)
    @Builder.Default
    private boolean revisionRequested = false;

    @Column(name = "revised_at", nullable = true)
    private Instant revisedAt;
}
