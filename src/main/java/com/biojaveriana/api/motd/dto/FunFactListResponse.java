package com.biojaveriana.api.motd.dto;

import java.util.List;

import com.biojaveriana.api.common.Pagination;

public record FunFactListResponse(
    List<FunFactSummaryResponse> content,
    Pagination pagination
) {
}
