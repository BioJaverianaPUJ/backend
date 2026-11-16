package com.biojaveriana.api.gamification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class GamificationConfig {

    @Value("${app.gamification.base-xp:100}")
    private long baseXp;

    @Value("${app.gamification.log-base:2}")
    private double logBase;

    private long resolvedBaseXp;
    private double resolvedLogBase;

    @PostConstruct
    public void init() {
        this.resolvedBaseXp = this.baseXp;
        this.resolvedLogBase = this.logBase;
    }

    public long getBaseXp() {
        return resolvedBaseXp;
    }

    public double getLogBase() {
        return resolvedLogBase;
    }
}
