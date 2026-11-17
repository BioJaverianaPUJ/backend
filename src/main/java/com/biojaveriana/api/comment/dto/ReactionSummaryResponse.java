package com.biojaveriana.api.comment.dto;

import java.util.List;

import com.biojaveriana.api.comment.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of reactions on a comment", example = "{\"emoji\": \"LIKE\", \"count\": 5, \"users\": []}")
public record ReactionSummaryResponse(
    @Schema(description = "Type of reaction emoji", example = "LIKE", allowableValues = {"LOVE", "HAHA", "WOW", "SAD", "ANGRY", "LIKE"})
    ReactionType emoji,

    @Schema(description = "Number of users who reacted with this emoji", example = "5")
    int count,

    @Schema(description = "List of users who reacted with this emoji")
    List<UserSummaryResponse> users
) {}
