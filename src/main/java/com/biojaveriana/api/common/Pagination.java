package com.biojaveriana.api.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

@Schema(description = "Pagination metadata for paginated responses", example = "{\"page\": 0, \"size\": 20, \"totalElements\": 100, \"totalPages\": 5, \"first\": true, \"last\": false}")
public record Pagination(
    @Schema(description = "Current page number (0-based)", example = "0")
    int page,

    @Schema(description = "Number of elements per page", example = "20")
    int size,

    @Schema(description = "Total number of elements across all pages", example = "100")
    long totalElements,

    @Schema(description = "Total number of pages", example = "5")
    int totalPages,

    @Schema(description = "Whether this is the first page", example = "true")
    boolean first,

    @Schema(description = "Whether this is the last page", example = "false")
    boolean last
) {
    public static Pagination from(Page<?> page) {
        return new Pagination(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
