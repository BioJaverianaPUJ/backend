package com.biojaveriana.api.gamification.event;

import java.util.UUID;

/**
 * Event fired when a user creates a post.
 */
public class PostCreatedEvent extends GamificationEvent {

    private final UUID postId;

    public PostCreatedEvent(Object source, UUID userId, UUID postId) {
        super(source, userId);
        this.postId = postId;
    }

    public UUID getPostId() {
        return postId;
    }

    @Override
    public GamificationEventType getType() {
        return GamificationEventType.POST_CREATED;
    }
}