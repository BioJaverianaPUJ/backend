package com.biojaveriana.api.gamification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;

/**
 * Paginated list of mission templates.
 */
public record MissionListResponse(
    List<MissionResponse> content,
    Pagination pagination
) {
    public static MissionListResponse from(Page<MissionResponse> page) {
        List<MissionResponse> content = page.getContent().stream().toList();
        return new MissionListResponse(content, Pagination.from(page));
    }
}
