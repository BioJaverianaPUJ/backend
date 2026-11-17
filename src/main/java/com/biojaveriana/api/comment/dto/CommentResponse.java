package com.biojaveriana.api.comment.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response body containing full comment details", example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"author\": {\"id\": \"660e8400-e29b-41d4-a716-446655440001\", \"username\": \"john_doe\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"profilePictureUrl\": null}, \"postId\": \"770e8400-e29b-41d4-a716-446655440002\", \"parentId\": null, \"content\": \"This is a great observation!\", \"totalReactions\": 5, \"reactions\": [], \"banned\": false, \"createdAt\": \"2026-06-15T10:00:00Z\", \"updatedAt\": \"2026-06-15T10:00:00Z\"}")
public record CommentResponse(
    @Schema(description = "Unique comment identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Author of the comment")
    UserSummaryResponse author,

    @Schema(description = "ID of the post this comment belongs to", example = "770e8400-e29b-41d4-a716-446655440002")
    UUID postId,

    @Schema(description = "ID of the parent comment if this is a reply", example = "550e8400-e29b-41d4-a716-446655440001")
    UUID parentId,

    @Schema(description = "Content of the comment", example = "This is a great observation!")
    String content,

    @Schema(description = "Total number of reactions on this comment", example = "5")
    Integer totalReactions,

    @Schema(description = "List of reaction summaries")
    List<ReactionSummaryResponse> reactions,

    @Schema(description = "Whether this comment is banned", example = "false")
    Boolean banned,

    @Schema(description = "Timestamp when the comment was created", example = "2026-06-15T10:00:00Z")
    Instant createdAt,

    @Schema(description = "Timestamp when the comment was last updated", example = "2026-06-15T10:00:00Z")
    Instant updatedAt
) {
}
