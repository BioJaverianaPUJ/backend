package com.biojaveriana.api.observation.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.s3.S3UrlGenerator;

public record ObservationListResponse(
    List<ObservationResponse> content,
    Pagination pagination
) {
    public static ObservationListResponse from(Page<Observation> page, S3UrlGenerator urlGenerator) {
        List<ObservationResponse> content = page.getContent().stream()
            .map(o -> ObservationResponse.from(o, urlGenerator))
            .toList();

        return new ObservationListResponse(content, Pagination.from(page));
    }
}
