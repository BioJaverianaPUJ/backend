package com.biojaveriana.api.comment.dto;

import java.util.List;

import com.biojaveriana.api.common.Pagination;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated response containing a list of comments", example = "{\"content\": [], \"pagination\": {\"page\": 0, \"size\": 20, \"totalElements\": 0, \"totalPages\": 0, \"first\": true, \"last\": true}}")
public record CommentListResponse(
    @Schema(description = "List of comment summaries in this page", example = "[{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"author\": {\"id\": \"660e8400-e29b-41d4-a716-446655440001\", \"username\": \"john_doe\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"profilePictureUrl\": null}, \"postId\": \"770e8400-e29b-41d4-a716-446655440002\", \"parentId\": null, \"content\": \"Great observation!\", \"totalReactions\": 5, \"banned\": false, \"createdAt\": \"2026-06-15T10:00:00Z\"}]")
    List<CommentSummaryResponse> content,

    @Schema(description = "Pagination metadata for this page")
    Pagination pagination
) {
}
