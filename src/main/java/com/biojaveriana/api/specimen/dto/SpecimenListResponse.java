package com.biojaveriana.api.specimen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.specimen.Specimen;

@Schema(description = "Paginated response containing a list of specimens", example = "{\"content\": [], \"pagination\": {\"page\": 0, \"size\": 20, \"totalElements\": 0, \"totalPages\": 0, \"first\": true, \"last\": true}}")
public record SpecimenListResponse(
    @Schema(description = "List of specimens in this page", example = "[{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"speciesId\": \"660e8400-e29b-41d4-a716-446655440001\", \"latitude\": 4.5708, \"longitude\": -75.7521, \"circumference\": 150.5, \"height\": 25.0, \"healthStatus\": \"WELL\", \"notes\": \"Healthy specimen\", \"observationDate\": \"2026-06-15T10:00:00Z\", \"createdAt\": \"2026-06-15T10:00:00Z\"}]")
    List<SpecimenResponse> content,

    @Schema(description = "Pagination metadata for this page")
    Pagination pagination
) {
    public static SpecimenListResponse from(Page<Specimen> page) {
        List<SpecimenResponse> content = page.getContent().stream()
            .map(SpecimenResponse::from)
            .toList();

        return new SpecimenListResponse(content, Pagination.from(page));
    }
}
