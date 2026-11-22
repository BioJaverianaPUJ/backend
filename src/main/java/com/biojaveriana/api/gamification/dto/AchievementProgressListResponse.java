package com.biojaveriana.api.gamification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;

public record AchievementProgressListResponse(
    List<AchievementProgressResponse> content,
    Pagination pagination
) {
    public static AchievementProgressListResponse from(Page<AchievementProgressResponse> page) {
        List<AchievementProgressResponse> content = page.getContent().stream().toList();

        return new AchievementProgressListResponse(content, Pagination.from(page));
    }
}