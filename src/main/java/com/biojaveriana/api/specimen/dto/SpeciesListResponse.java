package com.biojaveriana.api.specimen.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.specimen.Species;

public record SpeciesListResponse(
    List<SpeciesResponse> content,
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
