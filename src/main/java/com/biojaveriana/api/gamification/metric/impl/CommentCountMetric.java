package com.biojaveriana.api.gamification.metric.impl;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.comment.CommentRepository;
import com.biojaveriana.api.gamification.metric.Metric;

/**
 * Number of comments authored by the user.
 */
@Component
public class CommentCountMetric implements Metric {
    public static final String TYPE = "comments";

    private final CommentRepository commentRepository;

    public CommentCountMetric(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Long value(UUID userId) {
        return commentRepository.countByAuthorId(userId);
    }
}