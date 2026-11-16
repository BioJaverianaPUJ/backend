package com.biojaveriana.api.gamification.event;

import java.util.UUID;

/**
 * Event fired when a user creates a comment.
 */
public class CommentCreatedEvent extends GamificationEvent {

    private final UUID commentId;

    public CommentCreatedEvent(Object source, UUID userId, UUID commentId) {
        super(source, userId);
        this.commentId = commentId;
    }

    public UUID getCommentId() {
        return commentId;
    }

    @Override
    public String getEventType() {
        return "COMMENT_CREATED";
    }
}
