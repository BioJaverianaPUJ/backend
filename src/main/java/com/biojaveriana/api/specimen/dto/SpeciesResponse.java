package com.biojaveriana.api.specimen.dto;

import java.util.UUID;

import com.biojaveriana.api.specimen.Species;

public record SpeciesResponse(
    UUID id,
    String scientificName,
    String commonName,
    String family,
    Double woodDensity
) {
    public static SpeciesResponse from(Species species) {
        return new SpeciesResponse(
            species.getId(),
            species.getScientificName(),
            species.getCommonName(),
            species.getFamily(),
            species.getWoodDensity()
        );
    }
}
