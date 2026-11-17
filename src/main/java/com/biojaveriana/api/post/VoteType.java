package com.biojaveriana.api.post;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The type of vote on a post", example = "LIKE")
public enum VoteType {
    @Schema(description = "Like vote")
    LIKE,
    @Schema(description = "Dislike vote")
    DISLIKE
}
