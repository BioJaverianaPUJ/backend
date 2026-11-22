package com.biojaveriana.api.gamification.metric.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.metric.Metric;
import com.biojaveriana.api.gamification.metric.WindowedMetric;
import com.biojaveriana.api.post.PostRepository;

/**
 * Number of posts authored by the user.
 */
@Component
public class PostCountMetric implements WindowedMetric {
    public static final String TYPE = "posts";

    private final PostRepository postRepository;

    public PostCountMetric(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Long value(UUID userId) {
        return postRepository.countByAuthorId(userId);
    }

    @Override
    public Long valueSince(UUID userId, Instant since) {
        return postRepository.countByAuthorIdAndCreatedAtGreaterThanEqual(userId, since);
    }
}