package com.biojaveriana.api.gamification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;

/**
 * Paginated list of a user's completed missions (history).
 */
public record UserMissionListResponse(
    List<UserMissionResponse> content,
    Pagination pagination
) {
    public static UserMissionListResponse from(Page<UserMissionResponse> page) {
        List<UserMissionResponse> content = page.getContent().stream().toList();
        return new UserMissionListResponse(content, Pagination.from(page));
    }
}
