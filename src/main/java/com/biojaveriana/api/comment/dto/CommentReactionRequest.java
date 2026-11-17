package com.biojaveriana.api.comment.dto;

import com.biojaveriana.api.comment.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for adding a reaction to a comment", example = "{\"emoji\": \"LIKE\"}")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReactionRequest {
    @Schema(description = "Type of reaction emoji to add", example = "LIKE", allowableValues = {"LOVE", "HAHA", "WOW", "SAD", "ANGRY", "LIKE"})
    private ReactionType emoji;
}
