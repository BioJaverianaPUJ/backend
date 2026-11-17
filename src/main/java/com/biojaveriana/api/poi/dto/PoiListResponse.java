package com.biojaveriana.api.poi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.poi.Poi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Paginated response containing a list of POIs", example = "{\"content\": [], \"pagination\": {\"page\": 0, \"size\": 20, \"totalElements\": 0, \"totalPages\": 0, \"first\": true, \"last\": true}}")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoiListResponse {
    @Schema(description = "List of POIs in this page", example = "[{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"creatorId\": \"660e8400-e29b-41d4-a716-446655440001\", \"name\": \"Campus Main Gate\", \"description\": \"Main entrance\", \"geometryType\": \"POINT\", \"polygonCoordinatesWkt\": null, \"centerLatitude\": 4.5708, \"centerLongitude\": -75.7521, \"radiusMeters\": 50.0, \"visibility\": \"PUBLIC\", \"active\": true, \"createdAt\": \"2026-06-15T10:00:00Z\", \"updatedAt\": \"2026-06-15T10:00:00Z\"}]")
    private List<PoiResponse> content;

    @Schema(description = "Pagination metadata for this page")
    private Pagination pagination;

    public static PoiListResponse from(Page<Poi> page) {
        List<PoiResponse> content = page.getContent().stream()
            .map(PoiResponse::from)
            .toList();

        return PoiListResponse.builder()
            .content(content)
            .pagination(Pagination.from(page))
            .build();
    }
}
