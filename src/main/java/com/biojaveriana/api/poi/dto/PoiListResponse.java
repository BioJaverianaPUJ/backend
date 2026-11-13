package com.biojaveriana.api.poi.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.poi.Poi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoiListResponse {
    private List<PoiResponse> content;
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
