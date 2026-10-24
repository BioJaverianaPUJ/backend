package com.biojaveriana.api.specimen;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "species")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Species {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(name = "scientific_name", nullable = false, unique = true)
    private String scientificName;

    @NotBlank
    @Column(name = "common_name", nullable = false, unique = true)
    private String commonName;

    @NotBlank
    @Column(name = "family", nullable = false)
    private String family;

    @DecimalMin(value = "0.0")
    @Column(name = "wood_density_g_cm3")
    private Double woodDensity;
}
