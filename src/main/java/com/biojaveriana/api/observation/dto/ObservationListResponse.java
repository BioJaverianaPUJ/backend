package com.biojaveriana.api.observation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.s3.S3UrlGenerator;

@Schema(description = "Paginated response containing a list of observations", example = "{\"content\": [], \"pagination\": {\"page\": 0, \"size\": 20, \"totalElements\": 0, \"totalPages\": 0, \"first\": true, \"last\": true}}")
public record ObservationListResponse(
    @Schema(description = "List of observations in this page", example = "[{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"latitude\": 4.5708, \"longitude\": -75.7521, \"checked\": false, \"revisionRequested\": false, \"speciesCommonName\": \"English Oak\", \"certainty\": 0.95, \"similarityScore\": 0.92, \"observationType\": \"PLANT\", \"specimen\": null, \"imageUrl\": \"https://s3.example.com/uploads/observation/abc123.jpg\", \"createdAt\": \"2026-06-15T10:00:00Z\"}]")
    List<ObservationResponse> content,

    @Schema(description = "Pagination metadata for this page")
    Pagination pagination
) {
    public static ObservationListResponse from(Page<Observation> page, S3UrlGenerator urlGenerator) {
        List<ObservationResponse> content = page.getContent().stream()
            .map(o -> ObservationResponse.from(o, urlGenerator))
            .toList();

        return new ObservationListResponse(content, Pagination.from(page));
    }
}
