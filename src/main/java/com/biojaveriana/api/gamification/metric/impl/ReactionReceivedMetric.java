package com.biojaveriana.api.gamification.metric.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.comment.CommentReactionRepository;
import com.biojaveriana.api.gamification.metric.Metric;
import com.biojaveriana.api.gamification.metric.WindowedMetric;

/**
 * Total number of reactions the user has received across all of their comments.
 */
@Component
public class ReactionReceivedMetric implements WindowedMetric {
    public static final String TYPE = "reactions_received";

    private final CommentReactionRepository commentReactionRepository;

    public ReactionReceivedMetric(CommentReactionRepository commentReactionRepository) {
        this.commentReactionRepository = commentReactionRepository;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Long value(UUID userId) {
        return commentReactionRepository.countReactionsReceivedByUser(userId);
    }

    @Override
    public Long valueSince(UUID userId, Instant since) {
        return commentReactionRepository.countReactionsReceivedByUserSince(userId, since);
    }
}