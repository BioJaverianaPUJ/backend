package com.biojaveriana.api.comment.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentSummaryResponse(
    UUID id,
    UserSummaryResponse author,
    UUID postId,
    UUID parentId,
    String content,
    Integer totalReactions,
    Boolean banned,
    Instant createdAt
) {
}
