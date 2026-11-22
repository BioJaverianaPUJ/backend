package com.biojaveriana.api.gamification.event;

/**
 * The types of user actions that drive gamification. Each event type is the
 * trigger that causes the achievement engine to re-evaluate achievements.
 */
public enum GamificationEventType {
    POST_CREATED,
    COMMENT_CREATED,
    OBSERVATION_CREATED,
    COMMENT_REACTION_RECEIVED,
    POI_VISITED
}