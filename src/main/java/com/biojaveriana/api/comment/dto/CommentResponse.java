package com.biojaveriana.api.comment.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UserSummaryResponse author,
    UUID postId,
    UUID parentId,
    String content,
    Integer totalReactions,
    List<ReactionSummaryResponse> reactions,
    Boolean banned,
    Instant createdAt,
    Instant updatedAt
) {
}
