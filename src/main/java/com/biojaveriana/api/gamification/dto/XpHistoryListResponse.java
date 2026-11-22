package com.biojaveriana.api.gamification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.gamification.model.XpLog;

public record XpHistoryListResponse(
    List<XpLogResponse> content,
    Pagination pagination
) {
    public static XpHistoryListResponse from(Page<XpLog> page) {
        List<XpLogResponse> content = page.getContent().stream()
            .map(XpLogResponse::from)
            .toList();

        return new XpHistoryListResponse(content, Pagination.from(page));
    }
}