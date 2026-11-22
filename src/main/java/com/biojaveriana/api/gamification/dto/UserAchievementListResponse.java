package com.biojaveriana.api.gamification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;

public record UserAchievementListResponse(
    List<UserAchievementResponse> content,
    Pagination pagination
) {
    public static UserAchievementListResponse from(Page<UserAchievementResponse> page) {
        List<UserAchievementResponse> content = page.getContent().stream().toList();

        return new UserAchievementListResponse(content, Pagination.from(page));
    }
}