package com.biojaveriana.api.user.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.user.Ban;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated response containing a list of bans", example = "{\"content\": [], \"pagination\": {\"page\": 0, \"size\": 20, \"totalElements\": 0, \"totalPages\": 0, \"first\": true, \"last\": true}}")
public record BanListResponse(
    @Schema(description = "List of bans in this page", example = "[{\"id\": \"660e8400-e29b-41d4-a716-446655440001\", \"userId\": \"550e8400-e29b-41d4-a716-446655440000\", \"username\": \"john_doe\", \"reason\": \"Repeated violations\", \"banType\": \"TEMPORAL\", \"status\": \"ACTIVE\", \"expiresAt\": \"2026-12-31T23:59:59Z\", \"createdAt\": \"2026-06-15T10:00:00Z\"}]")
    List<BanResponse> content,

    @Schema(description = "Pagination metadata for this page")
    Pagination pagination
) {
    public static BanListResponse from(Page<Ban> page) {
        List<BanResponse> content = page.getContent().stream()
            .map(BanResponse::from)
            .toList();

        return new BanListResponse(
            content,
            Pagination.from(page)
        );
    }
}
