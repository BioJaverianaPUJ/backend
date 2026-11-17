package com.biojaveriana.api.specimen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

import com.biojaveriana.api.specimen.Species;

@Schema(description = "Response body containing species information", example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"scientificName\": \"Quercus robur\", \"commonName\": \"English Oak\", \"family\": \"Fagaceae\", \"woodDensity\": 0.7}")
public record SpeciesResponse(
    @Schema(description = "Unique species identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Scientific name (binomial nomenclature)", example = "Quercus robur")
    String scientificName,

    @Schema(description = "Common name of the species", example = "English Oak")
    String commonName,

    @Schema(description = "Taxonomic family", example = "Fagaceae")
    String family,

    @Schema(description = "Wood density value (if available)", example = "0.7")
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
