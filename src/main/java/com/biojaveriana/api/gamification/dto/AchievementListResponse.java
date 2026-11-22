package com.biojaveriana.api.gamification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;

public record AchievementListResponse(
    List<AchievementResponse> content,
    Pagination pagination
) {
    public static AchievementListResponse from(Page<AchievementResponse> page) {
        List<AchievementResponse> content = page.getContent().stream().toList();

        return new AchievementListResponse(content, Pagination.from(page));
    }
}