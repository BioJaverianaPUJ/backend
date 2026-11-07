package com.biojaveriana.api.comment.dto;

import java.util.List;
import java.util.UUID;

import com.biojaveriana.api.comment.ReactionType;

public record ReactionSummaryResponse(
    ReactionType emoji,
    int count,
    List<UserSummaryResponse> users
) {
}
