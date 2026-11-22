package com.biojaveriana.api.gamification.model;

/**
 * The type of a mission. DAILY and WEEKLY missions are randomly assigned to
 * each user for their respective calendar period; EVENT missions are shared
 * by all users during a fixed time window defined on the mission.
 */
public enum MissionType {
    DAILY,
    WEEKLY,
    EVENT
}
