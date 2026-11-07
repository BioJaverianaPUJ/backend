package com.biojaveriana.api.comment.dto;

import com.biojaveriana.api.comment.ReactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReactionRequest {
    private ReactionType emoji;
}
