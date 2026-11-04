package com.biojaveriana.api.observation.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ObservationListResponse {
    private List<ObservationResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static ObservationListResponse from(java.util.List<ObservationResponse> content, int page, int size, long totalElements, int totalPages) {
        return ObservationListResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
