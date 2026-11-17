package com.biojaveriana.api.comment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The type of reaction/emoji on a comment", example = "LIKE")
public enum ReactionType {
    @Schema(description = "Love reaction")
    LOVE,
    @Schema(description = "Haha reaction")
    HAHA,
    @Schema(description = "Wow reaction")
    WOW,
    @Schema(description = "Sad reaction")
    SAD,
    @Schema(description = "Angry reaction")
    ANGRY,
    @Schema(description = "Like reaction")
    LIKE
}
