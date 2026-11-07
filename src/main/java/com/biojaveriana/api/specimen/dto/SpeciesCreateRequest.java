package com.biojaveriana.api.specimen.dto;

import com.biojaveriana.api.specimen.Species;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;


public record SpeciesCreateRequest(
    @NotBlank
    String scientificName,

    @NotBlank
    String commonName,

    @NotBlank
    String family,

    @DecimalMin(value = "0.0")
    Double woodDensity
) {
    public Species toEntity() {
        return Species.builder()
            .scientificName(scientificName)
            .commonName(commonName)
            .family(family)
            .woodDensity(woodDensity)
            .build();
    }
}
