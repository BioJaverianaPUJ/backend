package com.biojaveriana.api.specimen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.specimen.Species;

@Schema(description = "Paginated response containing a list of species", example = "{\"content\": [], \"pagination\": {\"page\": 0, \"size\": 20, \"totalElements\": 0, \"totalPages\": 0, \"first\": true, \"last\": true}}")
public record SpeciesListResponse(
    @Schema(description = "List of species in this page", example = "[{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"scientificName\": \"Quercus robur\", \"commonName\": \"English Oak\", \"family\": \"Fagaceae\", \"woodDensity\": 0.7}]")
    List<SpeciesResponse> content,

    @Schema(description = "Pagination metadata for this page")
    Pagination pagination
) {
    public static SpeciesListResponse from(Page<Species> page) {
        List<SpeciesResponse> content = page.getContent().stream()
            .map(SpeciesResponse::from)
            .toList();

        return new SpeciesListResponse(
            content,
            Pagination.from(page)
        );
    }
}
