package com.biojaveriana.api.post.dto;

import com.biojaveriana.api.post.VoteType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostVoteRequest {
    @NotNull(message = "Vote type is required")
    private VoteType voteType;
}
