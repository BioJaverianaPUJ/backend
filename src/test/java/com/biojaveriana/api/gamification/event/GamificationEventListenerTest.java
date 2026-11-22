package com.biojaveriana.api.gamification.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.gamification.service.AchievementEngine;
import com.biojaveriana.api.gamification.service.MissionEngine;

@ExtendWith(MockitoExtension.class)
class GamificationEventListenerTest {

    @Mock
    private AchievementEngine achievementEngine;

    @Mock
    private MissionEngine missionEngine;

    private GamificationEventListener listener;
    private UUID userId;

    @BeforeEach
    void setUp() {
        listener = new GamificationEventListener(achievementEngine, missionEngine);
        userId = UUID.randomUUID();
    }

    private PostCreatedEvent postCreatedEvent() {
        return new PostCreatedEvent(this, userId, UUID.randomUUID());
    }

    @Test
    @DisplayName("handlePostCreated evaluates the user in both engines")
    void postCreatedEvaluatesBothEngines() {
        listener.handlePostCreated(postCreatedEvent());

        verify(achievementEngine).evaluateUser(userId);
        verify(missionEngine).evaluateUser(userId);
    }

    @Test
    @DisplayName("each event type evaluates the user in both engines")
    void allEventTypesEvaluateUser() {
        listener.handleCommentCreated(new CommentCreatedEvent(this, userId, UUID.randomUUID()));
        listener.handleObservationCreated(new ObservationCreatedEvent(this, userId, UUID.randomUUID()));
        listener.handleCommentReactionReceived(
            new CommentReactionReceivedEvent(this, userId, UUID.randomUUID(), UUID.randomUUID()));
        listener.handlePoiVisited(new PoiVisitedEvent(this, userId, UUID.randomUUID()));

        verify(achievementEngine, times(4)).evaluateUser(userId);
        verify(missionEngine, times(4)).evaluateUser(userId);
    }

    @Test
    @DisplayName("an achievement engine failure does not prevent mission evaluation")
    void achievementFailureDoesNotBlockMissions() {
        doThrow(new RuntimeException("boom")).when(achievementEngine).evaluateUser(userId);

        assertDoesNotThrow(() -> listener.handlePostCreated(postCreatedEvent()));

        verify(missionEngine).evaluateUser(userId);
    }

    @Test
    @DisplayName("a mission engine failure does not propagate to the caller")
    void missionFailureIsContained() {
        doThrow(new RuntimeException("boom")).when(missionEngine).evaluateUser(userId);

        assertDoesNotThrow(() -> listener.handlePostCreated(postCreatedEvent()));

        verify(achievementEngine).evaluateUser(userId);
    }
}
