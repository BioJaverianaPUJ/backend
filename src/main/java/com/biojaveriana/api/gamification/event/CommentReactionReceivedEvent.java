package com.biojaveriana.api.gamification.event;

import java.util.UUID;

/**
 * Event fired when a user receives a reaction on their comment.
 */
public class CommentReactionReceivedEvent extends GamificationEvent {

    private final UUID commentId;
    private final UUID reactionId;

    public CommentReactionReceivedEvent(Object source, UUID userId, UUID commentId, UUID reactionId) {
        super(source, userId);
        this.commentId = commentId;
        this.reactionId = reactionId;
    }

    public UUID getCommentId() {
        return commentId;
    }

    public UUID getReactionId() {
        return reactionId;
    }

    @Override
    public String getEventType() {
        return "COMMENT_REACTION_RECEIVED";
    }
}
