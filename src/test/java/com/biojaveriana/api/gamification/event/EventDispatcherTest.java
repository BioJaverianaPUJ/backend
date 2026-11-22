package com.biojaveriana.api.gamification.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EventDispatcherTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private EventDispatcher dispatcher;
    private UUID userId;

    @BeforeEach
    void setUp() {
        dispatcher = new EventDispatcher(eventPublisher);
        userId = UUID.randomUUID();
    }

    @SuppressWarnings("unchecked")
    private <T extends GamificationEvent> T capturePublished() {
        ArgumentCaptor<GamificationEvent> captor = ArgumentCaptor.forClass(GamificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return (T) captor.getValue();
    }

    @Test
    @DisplayName("dispatchPostCreated publishes a POST_CREATED event carrying the post id")
    void dispatchPostCreated() {
        UUID postId = UUID.randomUUID();

        dispatcher.dispatchPostCreated(userId, postId);

        PostCreatedEvent event = capturePublished();
        assertEquals(GamificationEventType.POST_CREATED, event.getType());
        assertEquals(userId, event.getUserId());
        assertEquals(postId, event.getPostId());
        assertNotNull(event.getId());
    }

    @Test
    @DisplayName("dispatchCommentCreated publishes a COMMENT_CREATED event carrying the comment id")
    void dispatchCommentCreated() {
        UUID commentId = UUID.randomUUID();

        dispatcher.dispatchCommentCreated(userId, commentId);

        CommentCreatedEvent event = capturePublished();
        assertEquals(GamificationEventType.COMMENT_CREATED, event.getType());
        assertEquals(commentId, event.getCommentId());
    }

    @Test
    @DisplayName("dispatchObservationCreated publishes an OBSERVATION_CREATED event carrying the observation id")
    void dispatchObservationCreated() {
        UUID observationId = UUID.randomUUID();

        dispatcher.dispatchObservationCreated(userId, observationId);

        ObservationCreatedEvent event = capturePublished();
        assertEquals(GamificationEventType.OBSERVATION_CREATED, event.getType());
        assertEquals(observationId, event.getObservationId());
    }

    @Test
    @DisplayName("dispatchCommentReactionReceived publishes the recipient's event with comment and reaction ids")
    void dispatchCommentReactionReceived() {
        UUID commentId = UUID.randomUUID();
        UUID reactionId = UUID.randomUUID();

        dispatcher.dispatchCommentReactionReceived(userId, commentId, reactionId);

        CommentReactionReceivedEvent event = capturePublished();
        assertEquals(GamificationEventType.COMMENT_REACTION_RECEIVED, event.getType());
        assertEquals(userId, event.getUserId());
        assertEquals(commentId, event.getCommentId());
        assertEquals(reactionId, event.getReactionId());
    }

    @Test
    @DisplayName("dispatchPoiVisited publishes a POI_VISITED event carrying the poi id")
    void dispatchPoiVisited() {
        UUID poiId = UUID.randomUUID();

        dispatcher.dispatchPoiVisited(userId, poiId);

        PoiVisitedEvent event = capturePublished();
        assertEquals(GamificationEventType.POI_VISITED, event.getType());
        assertEquals(poiId, event.getPoiId());
    }

    @Test
    @DisplayName("each published event receives a unique id")
    void eventsHaveUniqueIds() {
        dispatcher.dispatchPostCreated(userId, UUID.randomUUID());
        dispatcher.dispatchCommentCreated(userId, UUID.randomUUID());

        ArgumentCaptor<GamificationEvent> captor = ArgumentCaptor.forClass(GamificationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        GamificationEvent first = captor.getAllValues().get(0);
        GamificationEvent second = captor.getAllValues().get(1);

        assertNotNull(first.getId());
        assertNotEquals(first.getId(), second.getId());
    }
}
