package com.biojaveriana.api.specimen.dto;

import jakarta.validation.constraints.DecimalMin;

public record SpeciesUpdateDTO(
    String scientificName,
    String commonName,
    String family,

    @DecimalMin(value = "0.0")
    Double woodDensity
) {}
