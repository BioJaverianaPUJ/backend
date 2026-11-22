package com.biojaveriana.api.gamification.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.biojaveriana.api.gamification.service.AchievementEngine;

/**
 * Listens for gamification domain events and delegates to the achievement
 * engine. Uses {@code @TransactionalEventListener} so that achievements are
 * evaluated after the originating transaction commits (counts are up to date).
 *
 * <p>Failures are contained: a gamification problem must never fail the
 * action that triggered it (a post stays valid even if XP awarding breaks).
 * XP can be re-awarded later via the admin re-evaluation endpoint.
 */
@Component
public class GamificationEventListener {
    private static final Logger log = LoggerFactory.getLogger(GamificationEventListener.class);

    private final AchievementEngine achievementEngine;

    public GamificationEventListener(AchievementEngine achievementEngine) {
        this.achievementEngine = achievementEngine;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handlePostCreated(PostCreatedEvent event) {
        handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleCommentCreated(CommentCreatedEvent event) {
        handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleObservationCreated(ObservationCreatedEvent event) {
        handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleCommentReactionReceived(CommentReactionReceivedEvent event) {
        handle(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handlePoiVisited(PoiVisitedEvent event) {
        handle(event);
    }

    private void handle(GamificationEvent event) {
        try {
            achievementEngine.evaluateUser(event.getUserId());
        } catch (Exception e) {
            log.error("Gamification processing failed for event {} (user '{}'): {}",
                event.getType(), event.getUserId(), e.getMessage(), e);
        }
    }
}