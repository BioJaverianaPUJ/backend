package com.biojaveriana.api.gamification.config;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class GamificationConfig {

    @Value("${app.gamification.base-xp:100}")
    private long baseXp;

    @Value("${app.gamification.log-base:2}")
    private double logBase;

    @Value("${app.gamification.mission.daily-count:2}")
    private int dailyMissionCount;

    @Value("${app.gamification.mission.weekly-count:1}")
    private int weeklyMissionCount;

    @Value("${app.gamification.mission.timezone:UTC}")
    private String missionTimezone;

    private long resolvedBaseXp;
    private double resolvedLogBase;
    private int resolvedDailyMissionCount;
    private int resolvedWeeklyMissionCount;
    private ZoneId resolvedMissionTimezone;

    @PostConstruct
    public void init() {
        this.resolvedBaseXp = this.baseXp;
        this.resolvedLogBase = this.logBase;
        this.resolvedDailyMissionCount = this.dailyMissionCount;
        this.resolvedWeeklyMissionCount = this.weeklyMissionCount;
        this.resolvedMissionTimezone = ZoneId.of(missionTimezone);
    }

    public long getBaseXp() {
        return resolvedBaseXp;
    }

    public double getLogBase() {
        return resolvedLogBase;
    }

    /**
     * How many DAILY missions each user receives per day.
     */
    public int getDailyMissionCount() {
        return resolvedDailyMissionCount;
    }

    /**
     * How many WEEKLY missions each user receives per week.
     */
    public int getWeeklyMissionCount() {
        return resolvedWeeklyMissionCount;
    }

    /**
     * Timezone used for day/week mission boundaries.
     */
    public ZoneId getMissionTimezone() {
        return resolvedMissionTimezone;
    }
}
