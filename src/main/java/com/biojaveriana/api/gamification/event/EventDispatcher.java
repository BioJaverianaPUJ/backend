package com.biojaveriana.api.gamification.event;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Dispatcher for gamification events. Wraps event publication.
 */
@Component
public class EventDispatcher {

    private final ApplicationEventPublisher eventPublisher;

    public EventDispatcher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void dispatch(GamificationEvent event) {
        eventPublisher.publishEvent(event);
    }

    public void dispatchPostCreated(UUID userId, UUID postId) {
        dispatch(new PostCreatedEvent(this, userId, postId));
    }

    public void dispatchCommentCreated(UUID userId, UUID commentId) {
        dispatch(new CommentCreatedEvent(this, userId, commentId));
    }

    public void dispatchObservationCreated(UUID userId, UUID observationId) {
        dispatch(new ObservationCreatedEvent(this, userId, observationId));
    }

    public void dispatchCommentReactionReceived(UUID userId, UUID commentId, UUID reactionId) {
        dispatch(new CommentReactionReceivedEvent(this, userId, commentId, reactionId));
    }

    public void dispatchPoiVisited(UUID userId, UUID poiId) {
        dispatch(new PoiVisitedEvent(this, userId, poiId));
    }
}
