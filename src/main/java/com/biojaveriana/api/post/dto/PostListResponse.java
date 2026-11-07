package com.biojaveriana.api.post.dto;

import java.util.List;

import com.biojaveriana.api.common.Pagination;

public record PostListResponse(
    List<PostSummaryResponse> content,
    Pagination pagination
) {
    public static PostListResponse from(List<PostSummaryResponse> content, com.biojaveriana.api.common.Pagination pagination) {
        return new PostListResponse(content, pagination);
    }
}
