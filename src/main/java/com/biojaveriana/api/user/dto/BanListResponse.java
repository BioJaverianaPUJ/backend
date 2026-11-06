package com.biojaveriana.api.user.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.user.Ban;

public record BanListResponse(
    List<BanResponse> content,
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
