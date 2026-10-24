package com.biojaveriana.api.specimen.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.specimen.Specimen;

public record SpecimenListResponse(
    List<SpecimenResponse> content,
    Pagination pagination
) {
    public static SpecimenListResponse from(Page<Specimen> page) {
        List<SpecimenResponse> content = page.getContent().stream()
            .map(SpecimenResponse::from)
            .toList();

        return new SpecimenListResponse(content, Pagination.from(page));
    }
}
