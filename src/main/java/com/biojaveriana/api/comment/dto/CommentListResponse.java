package com.biojaveriana.api.comment.dto;

import java.util.List;

import com.biojaveriana.api.common.Pagination;

public record CommentListResponse(
    List<CommentSummaryResponse> content,
    Pagination pagination
) {
}
