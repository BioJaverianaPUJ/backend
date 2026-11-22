package com.biojaveriana.api.poi.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.poi.Poi;

public record PoiListResponse(
    List<PoiResponse> content,
    Pagination pagination
) {
    public static PoiListResponse from(Page<Poi> page) {
        List<PoiResponse> content = page.getContent().stream()
            .map(PoiResponse::from)
            .toList();

        return new PoiListResponse(content, Pagination.from(page));
    }
}
